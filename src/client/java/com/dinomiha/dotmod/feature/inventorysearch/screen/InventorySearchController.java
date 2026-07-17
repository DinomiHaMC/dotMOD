package com.dinomiha.dotmod.feature.inventorysearch.screen;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.InventorySearchDisplayMode;
import com.dinomiha.dotmod.feature.inventorysearch.InventorySearchEvaluator;
import com.dinomiha.dotmod.feature.inventorysearch.ItemSearchDocument;
import com.dinomiha.dotmod.feature.inventorysearch.ItemSearchDocumentFactory;
import com.dinomiha.dotmod.feature.inventorysearch.query.AndNode;
import com.dinomiha.dotmod.feature.inventorysearch.query.FilterNode;
import com.dinomiha.dotmod.feature.inventorysearch.query.FilterType;
import com.dinomiha.dotmod.feature.inventorysearch.query.QueryParseException;
import com.dinomiha.dotmod.feature.inventorysearch.query.QueryParser;
import com.dinomiha.dotmod.feature.inventorysearch.query.QueryTokenizer;
import com.dinomiha.dotmod.mixin.HandledScreenAccessor;
import com.dinomiha.dotmod.ui.component.DotButton;
import com.dinomiha.dotmod.ui.component.DotTextField;
import com.dinomiha.dotmod.ui.component.DotTooltip;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Language;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class InventorySearchController {
    private static final int SLOT_LIMIT = 2048;
    private static final int DOCUMENT_BUDGET_PER_FRAME = 64;
    private static final Map<HandledScreen<?>, WeakReference<InventorySearchController>> CONTROLLERS = new WeakHashMap<>();

    private final MinecraftClient client;
    private final HandledScreen<?> screen;
    private final QueryParser parser = new QueryParser();
    private final InventorySearchEvaluator evaluator = new InventorySearchEvaluator();
    private final Map<Integer, CachedDocument> documents = new HashMap<>();
    private String query = "";
    private AndNode parsed = new AndNode(List.of());
    private QueryParseException error;
    private TextFieldWidget field;
    private ButtonWidget helpButton;
    private boolean requiresTooltip;
    private Object language = Language.getInstance();
    private int lastMatches = -1;
    private int lastOccupied = -1;
    private boolean eventsRegistered;

    private InventorySearchController(MinecraftClient client, HandledScreen<?> screen) {
        this.client = client;
        this.screen = screen;
    }

    public static void attach(MinecraftClient client, HandledScreen<?> screen) {
        WeakReference<InventorySearchController> reference = CONTROLLERS.get(screen);
        InventorySearchController controller = reference == null ? null : reference.get();
        if (controller == null) {
            controller = new InventorySearchController(client, screen);
            CONTROLLERS.put(screen, new WeakReference<>(controller));
        }
        controller.registerEvents();
        controller.initWidgets();
    }

    public static void renderMasks(HandledScreen<?> screen, DrawContext context) {
        WeakReference<InventorySearchController> reference = CONTROLLERS.get(screen);
        InventorySearchController controller = reference == null ? null : reference.get();
        if (controller != null) {
            controller.renderMasks(context);
        }
    }

    public static boolean charTyped(CreativeInventoryScreen screen, CharInput input) {
        InventorySearchController controller = controller(screen);
        return controller != null && controller.field != null && controller.field.visible
                && controller.field.isFocused() && controller.field.charTyped(input);
    }

    public static boolean keyPressed(CreativeInventoryScreen screen, KeyInput input) {
        InventorySearchController controller = controller(screen);
        return controller != null && controller.field != null && controller.field.visible
                && controller.field.isFocused() && controller.field.keyPressed(input);
    }

    public static boolean mouseClicked(CreativeInventoryScreen screen, net.minecraft.client.gui.Click click, boolean doubled) {
        InventorySearchController controller = controller(screen);
        if (controller == null || controller.field == null || !controller.field.visible) {
            return false;
        }
        if (controller.field.isMouseOver(click.x(), click.y())) {
            screen.setFocused(controller.field);
            controller.field.setFocused(true);
            return controller.field.mouseClicked(click, doubled);
        }
        if (controller.helpButton.isMouseOver(click.x(), click.y())) {
            return controller.helpButton.mouseClicked(click, doubled);
        }
        return false;
    }

    private static InventorySearchController controller(HandledScreen<?> screen) {
        WeakReference<InventorySearchController> reference = CONTROLLERS.get(screen);
        return reference == null ? null : reference.get();
    }

    private void initWidgets() {
        field = DotTextField.create(
                client.textRenderer, 0, 0, 120, 20,
                Text.translatable("screen.dotmod.inventory_search.field"),
                Text.translatable("screen.dotmod.inventory_search.placeholder"),
                query,
                QueryTokenizer.MAX_LENGTH,
                value -> true,
                this::setQuery
        );
        helpButton = DotButton.create(
                0, 0, 20, Text.literal("?"),
                Text.translatable("screen.dotmod.inventory_search.help.body"),
                button -> {
                }
        );
        Screens.getButtons(screen).add(field);
        Screens.getButtons(screen).add(helpButton);
        updateLayout();
        updateTooltip();
    }

    private void registerEvents() {
        if (eventsRegistered) {
            return;
        }
        eventsRegistered = true;
        ScreenEvents.beforeRender(screen).register((ignored, context, mouseX, mouseY, tickDelta) -> updateLayout());
    }

    private void setQuery(String value) {
        query = value == null ? "" : value;
        try {
            parsed = parser.parse(query);
            error = null;
            requiresTooltip = parsed.children().stream()
                    .map(FilterNode.class::cast)
                    .anyMatch(filter -> filter.type() == FilterType.ALL_TEXT);
        } catch (QueryParseException exception) {
            parsed = null;
            error = exception;
            requiresTooltip = false;
        }
        lastMatches = -1;
        lastOccupied = -1;
        updateTooltip();
    }

    private void updateLayout() {
        if (field == null || helpButton == null) {
            return;
        }
        var config = ConfigService.get().config();
        boolean visible = config.general.enabled && config.inventorySearch.enabled && !narrowRecipeBookOpen();
        field.setVisible(visible);
        field.setEditable(visible);
        helpButton.visible = visible;
        helpButton.active = visible;
        if (!visible) {
            return;
        }
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int containerX = accessor.dotmod$getX();
        int containerY = accessor.dotmod$getY();
        int containerWidth = accessor.dotmod$getBackgroundWidth();
        int containerHeight = accessor.dotmod$getBackgroundHeight();
        int totalWidth = Math.max(74, Math.min(202, containerWidth));
        int x = Math.max(2, Math.min(screen.width - totalWidth - 2, containerX + (containerWidth - totalWidth) / 2));
        boolean preferBelow = screen instanceof CreativeInventoryScreen;
        int y = preferBelow && containerY + containerHeight + 22 <= screen.height
                ? containerY + containerHeight + 2
                : containerY >= 24
                ? containerY - 22
                : containerY + containerHeight + 22 <= screen.height
                ? containerY + containerHeight + 2
                : 2;
        field.setDimensionsAndPosition(totalWidth - 22, 20, x, y);
        helpButton.setDimensionsAndPosition(20, 20, x + totalWidth - 20, y);
    }

    private void renderMasks(DrawContext context) {
        var config = ConfigService.get().config();
        if (!config.general.enabled || !config.inventorySearch.enabled || query.isBlank() || parsed == null) {
            return;
        }
        if (language != Language.getInstance()) {
            language = Language.getInstance();
            documents.clear();
        }
        int occupied = 0;
        int matches = 0;
        int builtDocuments = 0;
        List<Slot> slots = screen.getScreenHandler().slots;
        int limit = Math.min(SLOT_LIMIT, slots.size());
        for (int index = 0; index < limit; index++) {
            Slot slot = slots.get(index);
            if (!slot.isEnabled()) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                documents.remove(index);
                continue;
            }
            occupied++;
            boolean match;
            try {
                CachedDocument cached = documents.get(index);
                boolean cacheValid = cached != null
                        && ItemStack.areEqual(cached.stack, stack)
                        && (!requiresTooltip || cached.includesTooltip);
                if (!cacheValid && builtDocuments >= DOCUMENT_BUDGET_PER_FRAME) {
                    matches++;
                    continue;
                }
                if (!cacheValid) {
                    builtDocuments++;
                }
                ItemSearchDocument document = document(index, stack, requiresTooltip);
                match = evaluator.matches(parsed, document);
            } catch (RuntimeException exception) {
                match = true;
            }
            if (match) {
                matches++;
                continue;
            }
            int color = config.inventorySearch.displayMode == InventorySearchDisplayMode.HIDE
                    ? 0xFF181818
                    : 0xB0000000;
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
        }
        if (matches != lastMatches || occupied != lastOccupied) {
            lastMatches = matches;
            lastOccupied = occupied;
            updateTooltip();
        }
    }

    private ItemSearchDocument document(int index, ItemStack stack, boolean includeTooltip) {
        CachedDocument cached = documents.get(index);
        if (cached != null && ItemStack.areEqual(cached.stack, stack)
                && (!includeTooltip || cached.includesTooltip)) {
            return cached.document;
        }
        ItemSearchDocument document = ItemSearchDocumentFactory.create(stack, client, includeTooltip);
        documents.put(index, new CachedDocument(stack.copy(), document, includeTooltip));
        return document;
    }

    private void updateTooltip() {
        if (field == null) {
            return;
        }
        Text tooltip;
        if (error != null) {
            tooltip = Text.translatable(
                    "screen.dotmod.inventory_search.error",
                    Text.translatable("screen.dotmod.inventory_search.error." + error.error().name().toLowerCase(Locale.ROOT)),
                    error.start() + 1
            );
            field.setEditableColor(0xFFFF5555);
        } else if (query.isBlank()) {
            tooltip = Text.translatable("screen.dotmod.inventory_search.empty");
            field.setEditableColor(0xFFE0E0E0);
        } else {
            Text explanation = explanation(parsed);
            tooltip = Text.translatable(
                    "screen.dotmod.inventory_search.explanation",
                    explanation,
                    Math.max(0, lastMatches),
                    Math.max(0, lastOccupied)
            );
            field.setEditableColor(0xFFE0E0E0);
        }
        DotTooltip.attach(field, tooltip);
    }

    private static Text explanation(AndNode query) {
        List<Text> parts = new ArrayList<>();
        for (var child : query.children()) {
            FilterNode filter = (FilterNode) child;
            Object value = filter.numberValue() == null ? filter.textValue() : filter.numberValue();
            parts.add(Text.translatable(
                    "screen.dotmod.inventory_search.filter",
                    Text.translatable("screen.dotmod.inventory_search.filter_type."
                            + filter.type().name().toLowerCase(Locale.ROOT)),
                    filter.operator().symbol(),
                    value
            ));
        }
        Text result = Text.empty();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) {
                result = result.copy().append(Text.literal("\n"));
            }
            result = result.copy().append(parts.get(index));
        }
        return result;
    }

    private boolean narrowRecipeBookOpen() {
        return screen instanceof RecipeBookScreen<?> recipeScreen
                && recipeScreen.width < 379
                && client.player != null
                && client.player.getRecipeBook().isGuiOpen(recipeScreen.getScreenHandler().getCategory());
    }

    private record CachedDocument(ItemStack stack, ItemSearchDocument document, boolean includesTooltip) {
    }

}
