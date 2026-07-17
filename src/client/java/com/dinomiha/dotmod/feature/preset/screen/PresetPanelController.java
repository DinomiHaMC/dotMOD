package com.dinomiha.dotmod.feature.preset.screen;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.PresetPanelSide;
import com.dinomiha.dotmod.feature.preset.PresetClientService;
import com.dinomiha.dotmod.feature.preset.PresetException;
import com.dinomiha.dotmod.feature.preset.PresetRecord;
import com.dinomiha.dotmod.feature.preset.helper.PresetHelperClientService;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.dinomiha.dotmod.mixin.HandledScreenAccessor;
import com.dinomiha.dotmod.ui.component.DotButton;
import com.dinomiha.dotmod.ui.component.DotConfirmationDialog;
import com.dinomiha.dotmod.ui.component.DotContextMenu;
import com.dinomiha.dotmod.ui.component.DotTextField;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class PresetPanelController {
    private static final int GAP = 4;
    private static final int DESIRED_WIDTH = 116;
    private static final int RAIL_WIDTH = 20;
    private static final int MAX_ROWS = 5;
    private static final Map<InventoryScreen, PanelState> SCREEN_STATES = new WeakHashMap<>();
    private static final Map<InventoryScreen, Boolean> CAPTURE_STATES = new WeakHashMap<>();

    private final MinecraftClient client;
    private final InventoryScreen screen;
    private final List<ClickableWidget> widgets = new ArrayList<>();
    private final List<PresetRowWidget> rows = new ArrayList<>();
    private List<PresetRecord> records = List.of();
    private List<PresetRecord> filtered = List.of();
    private TextFieldWidget search;
    private ButtonWidget collapse;
    private ButtonWidget create;
    private ButtonWidget importButton;
    private DotContextMenu contextMenu;
    private Bounds bounds = Bounds.hidden();
    private int firstRow;
    private int visibleRowCount = MAX_ROWS;
    private String query = "";
    private boolean mouseCaptured;
    private ClickableWidget capturedWidget;

    public PresetPanelController(MinecraftClient client, InventoryScreen screen) {
        this.client = client;
        this.screen = screen;
        PanelState state = SCREEN_STATES.get(screen);
        if (state != null) {
            this.query = state.query();
            this.firstRow = state.firstRow();
        }
        this.mouseCaptured = CAPTURE_STATES.getOrDefault(screen, false);
    }

    public void attach() {
        search = add(DotTextField.create(
                client.textRenderer, 0, 0, 80, 20,
                Text.translatable("screen.dotmod.preset.search"),
                Text.translatable("screen.dotmod.preset.search.placeholder"),
                query, 64, value -> true, this::filter
        ));
        collapse = add(DotButton.create(0, 0, 20, Text.literal("<"), button -> toggleExpanded()));
        create = add(DotButton.create(0, 0, 20, Text.literal("+"), Text.translatable("screen.dotmod.preset.create.tooltip"), button -> openCreate()));
        importButton = add(DotButton.create(0, 0, 20, Text.literal("I"), Text.translatable("screen.dotmod.preset.import.tooltip"), button -> importClipboard()));
        for (int index = 0; index < MAX_ROWS; index++) {
            rows.add(add(new PresetRowWidget(client.textRenderer, this::select, this::showContext)));
        }
        contextMenu = add(new DotContextMenu(client.textRenderer));
        refresh();
        updateLayout();

        ScreenEvents.afterBackground(screen).register((ignored, context, mouseX, mouseY, tickDelta) -> renderBackground(context));
        ScreenEvents.beforeRender(screen).register((ignored, context, mouseX, mouseY, tickDelta) -> updateLayout());
        ScreenMouseEvents.afterMouseScroll(screen).register((ignored, mouseX, mouseY, horizontalAmount, verticalAmount, consumed) -> {
            if (consumed || !bounds.expanded() || !bounds.contains(mouseX, mouseY)) {
                return false;
            }
            double amount = verticalAmount == 0.0 ? horizontalAmount : verticalAmount;
            if (amount == 0.0) {
                return false;
            }
            int max = Math.max(0, filtered.size() - visibleRowCount);
            firstRow = Math.max(0, Math.min(max, firstRow + (amount > 0 ? -1 : 1)));
            rememberState();
            updateRows();
            return true;
        });
        ScreenMouseEvents.allowMouseClick(screen).register((ignored, click) -> {
            if (!contextMenu.visible) {
                if (!bounds.contains(click.x(), click.y())) {
                    return true;
                }
                mouseCaptured = true;
                CAPTURE_STATES.put(screen, true);
                capturedWidget = null;
                dispatchClick(click);
                return false;
            }
            mouseCaptured = true;
            CAPTURE_STATES.put(screen, true);
            capturedWidget = null;
            if (contextMenu.isMouseOver(click.x(), click.y())) {
                capturedWidget = contextMenu;
                contextMenu.mouseClicked(click, false);
            } else {
                contextMenu.hide();
            }
            return false;
        });
        ScreenMouseEvents.allowMouseRelease(screen).register((ignored, click) -> {
            if (!mouseCaptured && !CAPTURE_STATES.getOrDefault(screen, false)) {
                return true;
            }
            if (capturedWidget != null) {
                capturedWidget.mouseReleased(click);
            }
            mouseCaptured = false;
            CAPTURE_STATES.put(screen, false);
            capturedWidget = null;
            return false;
        });
        ScreenKeyboardEvents.allowKeyPress(screen).register((ignored, input) -> {
            if (contextMenu.visible) {
                if (input.isEscape()) {
                    contextMenu.hide();
                }
                return false;
            }
            return true;
        });
    }

    private <T extends ClickableWidget> T add(T widget) {
        Screens.getButtons(screen).add(widget);
        widgets.add(widget);
        return widget;
    }

    private void refresh() {
        try {
            records = PresetClientService.list(client);
            filter(query);
        } catch (PresetException exception) {
            records = List.of();
            filtered = List.of();
            PresetClientService.report(exception);
        }
    }

    private void filter(String value) {
        String nextQuery = value == null ? "" : value;
        boolean changed = !nextQuery.equals(query);
        query = nextQuery;
        String key = query.strip().toLowerCase(Locale.ROOT);
        filtered = records.stream()
                .filter(record -> record.preset().name().toLowerCase(Locale.ROOT).contains(key))
                .toList();
        if (changed) {
            firstRow = 0;
        }
        rememberState();
        updateRows();
    }

    private void updateRows() {
        for (int index = 0; index < rows.size(); index++) {
            int presetIndex = firstRow + index;
            rows.get(index).setRecord(index < visibleRowCount && presetIndex < filtered.size() ? filtered.get(presetIndex) : null);
            rows.get(index).visible &= index < visibleRowCount && bounds.expanded() && bounds.visible();
        }
    }

    private void updateLayout() {
        bounds = computeBounds();
        boolean visible = bounds.visible();
        collapse.visible = visible;
        collapse.active = visible;
        if (!visible) {
            widgets.forEach(widget -> widget.visible = false);
            contextMenu.hide();
            return;
        }
        collapse.setDimensionsAndPosition(20, 20, bounds.x(), bounds.y());
        collapse.setMessage(Text.literal(bounds.expanded() ? "<" : ">"));
        search.setVisible(bounds.expanded());
        search.active = bounds.expanded();
        create.visible = bounds.expanded();
        importButton.visible = bounds.expanded();
        if (bounds.expanded()) {
            visibleRowCount = Math.max(0, Math.min(MAX_ROWS, (bounds.height() - 24) / 21));
            search.setDimensionsAndPosition(Math.max(20, bounds.width() - 64), 20, bounds.x() + 22, bounds.y());
            create.setDimensionsAndPosition(20, 20, bounds.x() + bounds.width() - 40, bounds.y());
            importButton.setDimensionsAndPosition(20, 20, bounds.x() + bounds.width() - 20, bounds.y());
            for (int index = 0; index < rows.size(); index++) {
                rows.get(index).setDimensionsAndPosition(bounds.width() - 4, 20, bounds.x() + 2, bounds.y() + 24 + index * 21);
            }
        } else {
            if (screen.getFocused() == search) {
                screen.setFocused(null);
            }
            contextMenu.hide();
        }
        updateRows();
    }

    private Bounds computeBounds() {
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int inventoryX = accessor.dotmod$getX();
        int inventoryY = accessor.dotmod$getY();
        int inventoryWidth = accessor.dotmod$getBackgroundWidth();
        int inventoryHeight = accessor.dotmod$getBackgroundHeight();
        boolean recipeBookOpen = client.player != null
                && client.player.getRecipeBook().isGuiOpen(screen.getScreenHandler().getCategory());
        if (recipeBookOpen) {
            return Bounds.hidden();
        }

        PresetPanelSide side = ConfigService.get().config().inventoryPresets.panelSide;
        int leftAvailable = inventoryX - GAP - 4;
        int rightStart = inventoryX + inventoryWidth + GAP;
        int rightAvailable = screen.width - rightStart - 4;
        boolean useRight = side == PresetPanelSide.RIGHT || side == PresetPanelSide.AUTO && leftAvailable < RAIL_WIDTH;
        if (useRight && client.player != null && !client.player.getStatusEffects().isEmpty()) {
            return Bounds.hidden();
        }
        int available = useRight ? rightAvailable : leftAvailable;
        if (available < RAIL_WIDTH) {
            return Bounds.hidden();
        }
        boolean expanded = ConfigService.get().config().inventoryPresets.panelExpanded && available >= 92;
        int panelWidth = expanded ? Math.min(DESIRED_WIDTH, available) : RAIL_WIDTH;
        int x = useRight ? rightStart : inventoryX - GAP - panelWidth;
        int y = useRight ? inventoryY + 52 : inventoryY;
        int height = Math.min(inventoryHeight - (useRight ? 52 : 0), screen.height - y - 4);
        if (height < 20) {
            return Bounds.hidden();
        }
        return new Bounds(x, y, panelWidth, height, true, expanded);
    }

    private void renderBackground(DrawContext context) {
        if (!bounds.visible()) {
            return;
        }
        context.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xCC181818);
        context.drawStrokedRectangle(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF777777);
    }

    private void toggleExpanded() {
        var config = ConfigService.get().config().inventoryPresets;
        config.panelExpanded = !config.panelExpanded;
        ConfigService.get().save();
        updateLayout();
    }

    private void openCreate() {
        client.setScreen(new PresetNameScreen(
                screen,
                Text.translatable("screen.dotmod.preset.create.title"),
                "",
                name -> PresetClientService.openCreate(client, screen, name)
        ));
    }

    private void importClipboard() {
        try {
            PresetRecord imported = PresetClientService.importPreset(client, client.keyboard.getClipboard());
            MessageService.sendChat(Text.translatable("command.dotmod.preset.imported", imported.preset().name()), MessageType.SUCCESS);
            refresh();
        } catch (PresetException exception) {
            PresetClientService.report(exception);
        }
    }

    private void select(PresetRecord record) {
        try {
            PresetClientService.selectAndArrange(client, record);
            refresh();
        } catch (PresetException exception) {
            PresetClientService.report(exception);
        }
    }

    private void showContext(PresetRecord record, net.minecraft.client.gui.Click click) {
        int width = Math.min(112, screen.width - 8);
        int height = DotContextMenu.ROW_HEIGHT * 5 + 2;
        int x = Math.max(4, Math.min(screen.width - width - 4, (int) click.x()));
        int y = Math.max(4, Math.min(screen.height - height - 4, (int) click.y()));
        contextMenu.show(x, y, width, List.of(
                new DotContextMenu.Action(Text.translatable("screen.dotmod.preset.context.helper"), null, true,
                        () -> withFresh(record, fresh -> PresetHelperClientService.open(client, screen, fresh))),
                new DotContextMenu.Action(Text.translatable("screen.dotmod.preset.context.view"), null, true, () -> withFresh(record, fresh -> PresetClientService.openView(client, screen, fresh))),
                new DotContextMenu.Action(Text.translatable("screen.dotmod.preset.context.edit"), null, true, () -> withFresh(record, fresh -> PresetClientService.openEdit(client, screen, fresh))),
                new DotContextMenu.Action(Text.translatable("screen.dotmod.preset.context.export"), null, true, () -> withFresh(record, this::export)),
                new DotContextMenu.Action(Text.translatable("screen.dotmod.preset.context.delete"), null, true, () -> confirmDelete(record))
        ));
        screen.setFocused(contextMenu);
    }

    private void export(PresetRecord record) {
        try {
            client.keyboard.setClipboard(PresetClientService.exportPreset(client, record));
            MessageService.sendChat(Text.translatable("command.dotmod.preset.exported", record.preset().name()), MessageType.SUCCESS);
        } catch (PresetException exception) {
            PresetClientService.report(exception);
        }
    }

    private void confirmDelete(PresetRecord record) {
        DotConfirmationDialog.open(
                client,
                screen,
                Text.translatable("command.dotmod.preset.delete.title"),
                Text.translatable("command.dotmod.preset.delete.message", record.preset().name()),
                () -> {
                    try {
                        PresetClientService.delete(client, record);
                        client.setScreen(screen);
                    } catch (PresetException exception) {
                        PresetClientService.report(exception);
                    }
                }
        );
    }

    private void withFresh(PresetRecord stale, java.util.function.Consumer<PresetRecord> action) {
        try {
            PresetRecord fresh = PresetClientService.require(client, stale.preset().id());
            if (!fresh.revision().equals(stale.revision())) {
                refresh();
                throw new PresetException(com.dinomiha.dotmod.feature.preset.PresetError.STALE_DATA, "Preset changed externally");
            }
            action.accept(fresh);
        } catch (PresetException exception) {
            PresetClientService.report(exception);
        }
    }

    private void dispatchClick(net.minecraft.client.gui.Click click) {
        for (int index = widgets.size() - 1; index >= 0; index--) {
            ClickableWidget widget = widgets.get(index);
            if (widget.visible && widget.active && widget.isMouseOver(click.x(), click.y())) {
                screen.setFocused(widget);
                widget.setFocused(true);
                capturedWidget = widget;
                widget.mouseClicked(click, false);
                return;
            }
        }
    }

    private void rememberState() {
        SCREEN_STATES.put(screen, new PanelState(query, firstRow));
    }

    private record Bounds(int x, int y, int width, int height, boolean visible, boolean expanded) {
        static Bounds hidden() {
            return new Bounds(0, 0, 0, 0, false, false);
        }

        boolean contains(double mouseX, double mouseY) {
            return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record PanelState(String query, int firstRow) {
    }
}
