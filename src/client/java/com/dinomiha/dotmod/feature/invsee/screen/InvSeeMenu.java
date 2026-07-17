package com.dinomiha.dotmod.feature.invsee.screen;

import com.dinomiha.dotmod.feature.invsee.InvSeeCapability;
import com.dinomiha.dotmod.feature.invsee.InvSeeInputController;
import com.dinomiha.dotmod.feature.invsee.InvSeeLayout;
import com.dinomiha.dotmod.feature.invsee.InvSeeSaveTarget;
import com.dinomiha.dotmod.feature.invsee.InvSeeSession;
import com.dinomiha.dotmod.feature.invsee.MutationResult;
import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.feature.invsee.catalog.CatalogEntry;
import com.dinomiha.dotmod.feature.invsee.catalog.LocalItemCatalog;
import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventorySerializer;
import com.dinomiha.dotmod.ui.component.DotButton;
import com.dinomiha.dotmod.ui.component.DotConfirmationDialog;
import com.dinomiha.dotmod.ui.component.DotTextField;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class InvSeeMenu extends Screen {
    private static final int PANEL_COLOR = 0xB0181818;
    private static final int PANEL_BORDER = 0xFF666666;

    private final Screen parent;
    private final InvSeeSession session;
    private final InvSeeInputController inputController;
    private final InvSeeSaveTarget saveTarget;
    private final RegistryWrapper.WrapperLookup registries;
    private final InvSeeSupplement supplement;
    private final BooleanSupplier parentValid;
    private final LocalItemCatalog catalog;
    private final VirtualInventorySerializer serializer = new VirtualInventorySerializer();
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final List<VirtualSlotWidget> inventorySlots = new ArrayList<>();
    private final List<VirtualSlotWidget> catalogSlots = new ArrayList<>();

    private InvSeeLayout layout;
    private List<CatalogEntry> filteredCatalog = List.of();
    private TextFieldWidget searchField;
    private TextFieldWidget amountField;
    private ButtonWidget amountButton;
    private ButtonWidget saveButton;
    private ButtonWidget rollbackButton;
    private ButtonWidget deleteButton;
    private ButtonWidget copyButton;
    private ButtonWidget panelButton;
    private ButtonWidget supplementButton;
    private int selectedInventorySlot;
    private int selectedCatalogIndex = -1;
    private int catalogFirstRow;
    private int catalogColumns;
    private int catalogRows;
    private boolean showCatalogPanel;
    private String searchQuery = "";
    private String pendingAmount;
    private int pendingAmountSlot = -1;
    private boolean syncingAmountField;
    private Text status = Text.empty();
    private int statusColor = 0xAAAAAA;

    public InvSeeMenu(
            Screen parent,
            Text title,
            InvSeeSession session,
            InvSeeSaveTarget saveTarget,
            RegistryWrapper.WrapperLookup registries
    ) {
        this(parent, title, session, saveTarget, registries, null, () -> true);
    }

    public InvSeeMenu(
            Screen parent,
            Text title,
            InvSeeSession session,
            InvSeeSaveTarget saveTarget,
            RegistryWrapper.WrapperLookup registries,
            InvSeeSupplement supplement
    ) {
        this(parent, title, session, saveTarget, registries, supplement, () -> true);
    }

    public InvSeeMenu(
            Screen parent,
            Text title,
            InvSeeSession session,
            InvSeeSaveTarget saveTarget,
            RegistryWrapper.WrapperLookup registries,
            InvSeeSupplement supplement,
            BooleanSupplier parentValid
    ) {
        super(title);
        this.parent = parent;
        this.session = session;
        this.inputController = new InvSeeInputController(session);
        this.saveTarget = saveTarget;
        this.registries = registries;
        this.supplement = supplement;
        this.parentValid = parentValid;
        this.catalog = session.mode().allows(InvSeeCapability.CATALOG) ? LocalItemCatalog.create() : null;
        if (catalog != null) {
            filteredCatalog = catalog.search("");
        }
        if (supplement != null) {
            status = supplement.summary();
            statusColor = supplement.warningSlots().isEmpty() ? 0x55FF55 : 0xFFAA00;
        }
    }

    @Override
    protected void init() {
        inventorySlots.clear();
        catalogSlots.clear();
        layout = InvSeeLayout.compute(width, height, catalog != null);

        createInventorySlots();
        if (catalog != null) {
            createCatalog();
        }
        if (session.mode().allows(InvSeeCapability.SET_AMOUNT)) {
            createAmountEditor();
        }
        createFooterButtons();
        selectInventory(selectedInventorySlot);
        refreshAll();
        if (layout.compact() && showCatalogPanel && searchField != null) {
            searchField.setFocused(true);
            setFocused(searchField);
        }
    }

    private void createInventorySlots() {
        InvSeeLayout.Rect panel = layout.inventoryPanel();
        int mainX = panel.x() + 24;
        int mainY = panel.y() + 6;

        for (int index = 0; index < VirtualInventory.SLOT_COUNT; index++) {
            int x;
            int y;
            if (index < 9) {
                x = mainX + index * VirtualSlotWidget.SIZE;
                y = mainY + 58;
            } else if (index < 36) {
                int relative = index - 9;
                x = mainX + relative % 9 * VirtualSlotWidget.SIZE;
                y = mainY + relative / 9 * VirtualSlotWidget.SIZE;
            } else if (index < 40) {
                x = panel.x() + 2;
                y = mainY + (39 - index) * VirtualSlotWidget.SIZE;
            } else {
                x = mainX + 9 * VirtualSlotWidget.SIZE + 4;
                y = mainY + 58;
            }
            VirtualSlotWidget widget = new VirtualSlotWidget(index, x, y, textRenderer, this::onInventorySlotPressed);
            widget.setWarning(supplement != null && supplement.warningSlots().contains(index));
            inventorySlots.add(addDrawableChild(widget));
        }
    }

    private void createCatalog() {
        InvSeeLayout.Rect panel = layout.catalogPanel();
        searchField = DotTextField.create(
                textRenderer,
                panel.x() + 4,
                panel.y() + 4,
                Math.max(40, panel.width() - 8),
                20,
                Text.translatable("screen.dotmod.ism.catalog.search"),
                Text.translatable("screen.dotmod.ism.catalog.search.placeholder"),
                searchQuery,
                64,
                value -> true,
                this::filterCatalog
        );
        addDrawableChild(searchField);

        catalogColumns = Math.max(1, (panel.width() - 8) / VirtualSlotWidget.SIZE);
        catalogRows = Math.max(1, (panel.height() - 30) / VirtualSlotWidget.SIZE);
        int visibleSlots = catalogColumns * catalogRows;
        for (int index = 0; index < visibleSlots; index++) {
            int x = panel.x() + 4 + index % catalogColumns * VirtualSlotWidget.SIZE;
            int y = panel.y() + 28 + index / catalogColumns * VirtualSlotWidget.SIZE;
            VirtualSlotWidget widget = new VirtualSlotWidget(index, x, y, textRenderer, this::onCatalogSlotPressed);
            catalogSlots.add(addDrawableChild(widget));
        }
    }

    private void createAmountEditor() {
        amountField = DotTextField.create(
                textRenderer,
                Math.max(8, width - 108),
                14,
                48,
                20,
                Text.translatable("screen.dotmod.ism.amount"),
                Text.translatable("screen.dotmod.ism.amount.placeholder"),
                "",
                3,
                value -> value.isEmpty() || value.chars().allMatch(Character::isDigit),
                value -> {
                    if (!syncingAmountField) {
                        pendingAmount = value;
                        pendingAmountSlot = selectedInventorySlot;
                    }
                }
        );
        addDrawableChild(amountField);
        amountButton = addDrawableChild(DotButton.create(
                Math.max(60, width - 56),
                14,
                48,
                Text.translatable("screen.dotmod.ism.amount.set"),
                button -> applyAmount()
        ));
    }

    private void createFooterButtons() {
        saveButton = footerButton(0, Text.translatable("screen.dotmod.ism.save"), button -> save());
        supplementButton = footerButton(0, supplement == null ? Text.empty() : supplement.actionLabel(), button -> {
            if (supplement != null) {
                supplement.action().accept(this);
            }
        });
        rollbackButton = footerButton(1, Text.translatable("screen.dotmod.ism.rollback"), button -> rollback());
        deleteButton = footerButton(2, Text.translatable("screen.dotmod.ism.delete"), button -> deleteSelected());
        copyButton = footerButton(3, Text.translatable("screen.dotmod.ism.copy"), button -> copySelectedInfo());
        panelButton = footerButton(4, Text.empty(), button -> togglePanel());
        footerButton(5, Text.translatable("screen.dotmod.ism.close"), button -> requestClose());
    }

    private ButtonWidget footerButton(int position, Text label, ButtonWidget.PressAction action) {
        InvSeeLayout.Rect footer = layout.footer();
        int gap = 4;
        int buttonWidth = Math.max(40, (footer.width() - gap * 2) / 3);
        int column = position % 3;
        int row = position / 3;
        return addDrawableChild(DotButton.create(
                footer.x() + column * (buttonWidth + gap),
                footer.y() + row * 22,
                buttonWidth,
                label,
                action
        ));
    }

    private void onInventorySlotPressed(VirtualSlotWidget widget, int button, boolean shift) {
        int index = widget.referenceIndex();
        boolean selectOnly = session.mode() == com.dinomiha.dotmod.feature.invsee.InvSeeMode.VIEW
                || (!session.hasCursorStack() && selectedInventorySlot != index && button == 0 && !shift);
        if (!selectInventory(index)) {
            restoreAmountFocus();
            return;
        }
        if (!selectOnly) {
            if (!commitPendingAmount()) {
                restoreAmountFocus();
                return;
            }
            MutationResult result = inputController.clickSlot(index, button);
            showMutationResult(result);
            refreshAll();
        }
    }

    private void onCatalogSlotPressed(VirtualSlotWidget widget, int button, boolean shift) {
        int index = catalogFirstRow * catalogColumns + widget.referenceIndex();
        if (index < 0 || index >= filteredCatalog.size()) {
            return;
        }
        selectedCatalogIndex = index;
        if (session.hasCursorStack()) {
            setStatus(Text.translatable("screen.dotmod.ism.status.cursor_not_empty"), Formatting.RED);
            return;
        }
        MutationResult result = inputController.takeCatalogStack(filteredCatalog.get(index).stack(), button == 1);
        showMutationResult(result);
        boolean switchedToInventory = false;
        if (layout.compact() && result == MutationResult.APPLIED) {
            showCatalogPanel = false;
            switchedToInventory = true;
        }
        refreshAll();
        if (switchedToInventory && client != null) {
            client.send(() -> {
                if (client.currentScreen == this) {
                    selectInventory(selectedInventorySlot);
                }
            });
        }
    }

    private boolean selectInventory(int index) {
        int next = Math.max(0, Math.min(VirtualInventory.SLOT_COUNT - 1, index));
        if (next != selectedInventorySlot && !commitPendingAmount()) {
            return false;
        }
        selectedInventorySlot = next;
        VirtualSlotWidget focused = null;
        for (VirtualSlotWidget slot : inventorySlots) {
            boolean selected = slot.referenceIndex() == selectedInventorySlot;
            slot.setSelected(selected);
            slot.setFocused(selected);
            if (selected) {
                focused = slot;
            }
        }
        if (focused != null) {
            setFocused(focused);
        }
        updateAmountField(true);
        return true;
    }

    private void filterCatalog(String query) {
        searchQuery = query;
        filteredCatalog = catalog.search(query);
        catalogFirstRow = 0;
        selectedCatalogIndex = -1;
        refreshCatalog();
    }

    private void refreshAll() {
        for (VirtualSlotWidget widget : inventorySlots) {
            widget.setStack(session.getStack(widget.referenceIndex()));
        }
        refreshCatalog();
        updateAmountField(false);
        updateWidgetStates();
    }

    private void refreshCatalog() {
        if (catalog == null) {
            return;
        }
        int start = catalogFirstRow * catalogColumns;
        int totalRows = (filteredCatalog.size() + catalogColumns - 1) / catalogColumns;
        catalogFirstRow = Math.max(0, Math.min(catalogFirstRow, Math.max(0, totalRows - catalogRows)));
        start = catalogFirstRow * catalogColumns;
        for (int visibleIndex = 0; visibleIndex < catalogSlots.size(); visibleIndex++) {
            int catalogIndex = start + visibleIndex;
            VirtualSlotWidget widget = catalogSlots.get(visibleIndex);
            widget.setStack(catalogIndex < filteredCatalog.size() ? filteredCatalog.get(catalogIndex).stack() : ItemStack.EMPTY);
            widget.active = catalogIndex < filteredCatalog.size();
            widget.setSelected(catalogIndex == selectedCatalogIndex);
        }
    }

    private void updateAmountField(boolean force) {
        if (amountField == null || amountField.isFocused() && !force) {
            return;
        }
        String value;
        if (pendingAmountSlot == selectedInventorySlot && pendingAmount != null) {
            value = pendingAmount;
        } else {
            ItemStack selected = session.getStack(selectedInventorySlot);
            value = selected.isEmpty() ? "" : Integer.toString(selected.getCount());
        }
        syncingAmountField = true;
        amountField.setText(value);
        syncingAmountField = false;
    }

    private void updateWidgetStates() {
        boolean inventoryVisible = catalog == null || layout.wide() || !showCatalogPanel;
        boolean catalogVisible = catalog != null && (layout.wide() || showCatalogPanel);
        inventorySlots.forEach(widget -> widget.visible = inventoryVisible);
        catalogSlots.forEach(widget -> widget.visible = catalogVisible);
        if (searchField != null) {
            searchField.setVisible(catalogVisible);
        }
        if (amountField != null) {
            amountField.setVisible(inventoryVisible);
            amountField.setEditable(inventoryVisible);
            amountButton.visible = inventoryVisible;
            amountButton.active = inventoryVisible && !session.getStack(selectedInventorySlot).isEmpty();
        }
        saveButton.visible = session.mode().allows(InvSeeCapability.SAVE);
        saveButton.active = !session.hasCursorStack();
        rollbackButton.visible = session.mode().allows(InvSeeCapability.ROLLBACK);
        rollbackButton.active = session.isDirty() || session.hasCursorStack();
        deleteButton.visible = session.mode().allows(InvSeeCapability.MUTATE);
        deleteButton.active = inventoryVisible && !session.getStack(selectedInventorySlot).isEmpty();
        copyButton.active = inventoryVisible && !session.getStack(selectedInventorySlot).isEmpty();
        panelButton.visible = layout.compact();
        supplementButton.visible = supplement != null;
        panelButton.setMessage(Text.translatable(showCatalogPanel
                ? "screen.dotmod.ism.panel.inventory"
                : "screen.dotmod.ism.panel.catalog"));
    }

    private void applyAmount() {
        MutationResult result = inputController.setAmount(selectedInventorySlot, amountField.getText());
        if (result == MutationResult.INVALID) {
            setStatus(Text.translatable("screen.dotmod.ism.status.invalid_amount"), Formatting.RED);
        } else {
            showMutationResult(result);
        }
        refreshAll();
        pendingAmount = null;
        pendingAmountSlot = -1;
        updateAmountField(true);
    }

    private void deleteSelected() {
        if (!commitPendingAmount()) {
            restoreAmountFocus();
            return;
        }
        showMutationResult(inputController.deleteSlot(selectedInventorySlot));
        refreshAll();
    }

    private void rollback() {
        pendingAmount = null;
        pendingAmountSlot = -1;
        showMutationResult(session.rollback());
        refreshAll();
        updateAmountField(true);
    }

    private void save() {
        if (!commitPendingAmount()) {
            return;
        }
        if (session.hasCursorStack()) {
            setStatus(Text.translatable("screen.dotmod.ism.status.cursor_not_empty"), Formatting.RED);
            return;
        }
        if (session.save(saveTarget)) {
            setStatus(Text.translatable("screen.dotmod.ism.status.saved"), Formatting.GREEN);
            closeDirectly();
        } else {
            setStatus(Text.translatable("screen.dotmod.ism.status.save_failed"), Formatting.RED);
        }
    }

    private void copySelectedInfo() {
        ItemStack stack = session.getStack(selectedInventorySlot);
        if (stack.isEmpty() || client == null) {
            setStatus(Text.translatable("screen.dotmod.ism.status.empty_selection"), Formatting.YELLOW);
            return;
        }
        try {
            String json = gson.toJson(serializer.encodeStack(stack, registries));
            String value = Text.translatable(
                    "screen.dotmod.ism.clipboard.item",
                    stack.getName(),
                    Registries.ITEM.getId(stack.getItem()),
                    stack.getCount(),
                    json
            ).getString();
            client.keyboard.setClipboard(value);
            setStatus(Text.translatable("screen.dotmod.ism.status.copied"), Formatting.GREEN);
        } catch (RuntimeException exception) {
            setStatus(Text.translatable("screen.dotmod.ism.status.copy_failed"), Formatting.RED);
        }
    }

    private void togglePanel() {
        if (!showCatalogPanel && !commitPendingAmount()) {
            return;
        }
        showCatalogPanel = !showCatalogPanel;
        updateWidgetStates();
    }

    private boolean hasPendingAmount() {
        if (pendingAmount == null || pendingAmountSlot != selectedInventorySlot) {
            return false;
        }
        ItemStack selected = session.getStack(selectedInventorySlot);
        String current = selected.isEmpty() ? "" : Integer.toString(selected.getCount());
        return !pendingAmount.equals(current);
    }

    private boolean commitPendingAmount() {
        if (!hasPendingAmount()) {
            return true;
        }
        MutationResult result = inputController.setAmount(selectedInventorySlot, pendingAmount);
        if (result == MutationResult.INVALID || result == MutationResult.DENIED) {
            setStatus(Text.translatable("screen.dotmod.ism.status.invalid_amount"), Formatting.RED);
            return false;
        }
        pendingAmount = null;
        pendingAmountSlot = -1;
        refreshAll();
        return true;
    }

    private void restoreAmountFocus() {
        if (client == null || amountField == null) {
            return;
        }
        client.send(() -> {
            if (client.currentScreen == this) {
                amountField.setFocused(true);
                setFocused(amountField);
            }
        });
    }

    private void showMutationResult(MutationResult result) {
        if (result == MutationResult.DENIED) {
            setStatus(Text.translatable("screen.dotmod.ism.status.read_only"), Formatting.RED);
        } else if (result == MutationResult.INVALID) {
            setStatus(Text.translatable("screen.dotmod.ism.status.invalid_action"), Formatting.RED);
        }
    }

    private void setStatus(Text message, Formatting color) {
        status = message;
        statusColor = color.getColorValue() == null ? 0xAAAAAA : color.getColorValue();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            requestClose();
            return true;
        }
        if (amountField != null && amountField.isFocused() && input.isEnter()) {
            applyAmount();
            return true;
        }
        boolean textFocused = (amountField != null && amountField.isFocused()) || (searchField != null && searchField.isFocused());
        if (input.hasCtrlOrCmd() && input.getKeycode() == GLFW.GLFW_KEY_S
                && session.mode().allows(InvSeeCapability.SAVE)) {
            save();
            return true;
        }
        if (input.hasCtrlOrCmd() && input.getKeycode() == GLFW.GLFW_KEY_Z
                && session.mode().allows(InvSeeCapability.ROLLBACK)) {
            rollback();
            return true;
        }
        if (input.hasCtrlOrCmd() && input.getKeycode() == GLFW.GLFW_KEY_C && !textFocused) {
            copySelectedInfo();
            return true;
        }
        if (!textFocused) {
            if ((input.isEnter() || input.getKeycode() == GLFW.GLFW_KEY_SPACE) && super.keyPressed(input)) {
                return true;
            }
            if ((input.getKeycode() == GLFW.GLFW_KEY_DELETE || input.getKeycode() == GLFW.GLFW_KEY_BACKSPACE)
                    && inventorySlots.contains(getFocused())
                    && (catalog == null || layout.wide() || !showCatalogPanel)) {
                deleteSelected();
                return true;
            }
            if (inventorySlots.contains(getFocused())) {
                int next = switch (input.getKeycode()) {
                    case GLFW.GLFW_KEY_LEFT -> selectedInventorySlot - 1;
                    case GLFW.GLFW_KEY_RIGHT -> selectedInventorySlot + 1;
                    case GLFW.GLFW_KEY_UP -> selectedInventorySlot - 9;
                    case GLFW.GLFW_KEY_DOWN -> selectedInventorySlot + 9;
                    default -> selectedInventorySlot;
                };
                if (next != selectedInventorySlot) {
                    selectInventory(next);
                    return true;
                }
            } else if (catalogSlots.contains(getFocused()) && navigateCatalog(input.getKeycode())) {
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private boolean navigateCatalog(int keycode) {
        if (filteredCatalog.isEmpty()) {
            return false;
        }
        int current = selectedCatalogIndex < 0 ? 0 : selectedCatalogIndex;
        int next = switch (keycode) {
            case GLFW.GLFW_KEY_LEFT -> current - 1;
            case GLFW.GLFW_KEY_RIGHT -> current + 1;
            case GLFW.GLFW_KEY_UP -> current - catalogColumns;
            case GLFW.GLFW_KEY_DOWN -> current + catalogColumns;
            case GLFW.GLFW_KEY_PAGE_UP -> current - catalogColumns * catalogRows;
            case GLFW.GLFW_KEY_PAGE_DOWN -> current + catalogColumns * catalogRows;
            default -> current;
        };
        next = Math.max(0, Math.min(filteredCatalog.size() - 1, next));
        if (next == current && selectedCatalogIndex >= 0) {
            return false;
        }
        selectedCatalogIndex = next;
        int row = selectedCatalogIndex / catalogColumns;
        if (row < catalogFirstRow) {
            catalogFirstRow = row;
        } else if (row >= catalogFirstRow + catalogRows) {
            catalogFirstRow = row - catalogRows + 1;
        }
        refreshCatalog();
        int visibleIndex = selectedCatalogIndex - catalogFirstRow * catalogColumns;
        inventorySlots.forEach(slot -> slot.setFocused(false));
        for (int index = 0; index < catalogSlots.size(); index++) {
            catalogSlots.get(index).setFocused(index == visibleIndex);
        }
        setFocused(catalogSlots.get(visibleIndex));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        boolean catalogVisible = catalog != null && (layout.wide() || showCatalogPanel);
        if (catalogVisible && layout.catalogPanel().contains(mouseX, mouseY)) {
            double amount = verticalAmount == 0.0 ? horizontalAmount : verticalAmount;
            int totalRows = (filteredCatalog.size() + catalogColumns - 1) / catalogColumns;
            int maxFirstRow = Math.max(0, totalRows - catalogRows);
            int next = Math.max(0, Math.min(maxFirstRow, catalogFirstRow + (amount > 0 ? -1 : 1)));
            if (amount != 0.0 && next != catalogFirstRow) {
                catalogFirstRow = next;
                refreshCatalog();
            }
            return amount != 0.0;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0xD0101010);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        boolean inventoryVisible = catalog == null || layout.wide() || !showCatalogPanel;
        boolean catalogVisible = catalog != null && (layout.wide() || showCatalogPanel);
        if (inventoryVisible) {
            drawPanel(context, layout.inventoryPanel());
        }
        if (catalogVisible) {
            drawPanel(context, layout.catalogPanel());
        }
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 5, 0xFFFFFF);
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.dotmod.ism.mode", Text.translatable(session.mode().shortTranslationKey()), Text.translatable(session.mode().translationKey())),
                8,
                20,
                0xAAAAAA
        );
        if (!status.getString().isEmpty()) {
            List<OrderedText> statusLines = textRenderer.wrapLines(status, Math.max(80, layout.status().width() - 8));
            int firstLine = Math.max(0, statusLines.size() - 2);
            int y = layout.status().y() + 1;
            for (int line = firstLine; line < statusLines.size(); line++) {
                context.drawCenteredTextWithShadow(textRenderer, statusLines.get(line), width / 2, y, statusColor);
                y += 10;
            }
        }
        super.render(context, mouseX, mouseY, deltaTicks);

        ItemStack cursor = session.getCursorStack();
        if (!cursor.isEmpty()) {
            context.drawItem(cursor, mouseX - 8, mouseY - 8);
            context.drawStackOverlay(textRenderer, cursor, mouseX - 8, mouseY - 8);
        }
        VirtualSlotWidget hovered = hoveredSlot(mouseX, mouseY);
        if (hovered != null && !hovered.stack().isEmpty()) {
            context.drawItemTooltip(textRenderer, hovered.stack(), mouseX, mouseY);
        }
    }

    private void drawPanel(DrawContext context, InvSeeLayout.Rect panel) {
        context.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), PANEL_BORDER);
        context.fill(panel.x() + 1, panel.y() + 1, panel.x() + panel.width() - 1, panel.y() + panel.height() - 1, PANEL_COLOR);
    }

    private VirtualSlotWidget hoveredSlot(int mouseX, int mouseY) {
        for (VirtualSlotWidget slot : inventorySlots) {
            if (slot.visible && slot.isMouseOver(mouseX, mouseY)) {
                return slot;
            }
        }
        for (VirtualSlotWidget slot : catalogSlots) {
            if (slot.visible && slot.isMouseOver(mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private void requestClose() {
        if (client == null) {
            return;
        }
        if (session.isDirty() || session.hasCursorStack() || hasPendingAmount()) {
            DotConfirmationDialog.open(
                    client,
                    this,
                    Text.translatable("screen.dotmod.ism.discard.title"),
                    Text.translatable("screen.dotmod.ism.discard.message"),
                    this::closeDirectly
            );
        } else {
            closeDirectly();
        }
    }

    private void closeDirectly() {
        if (client != null) {
            client.setScreen(parentValid.getAsBoolean() ? parent : null);
        }
    }

    @Override
    public void close() {
        requestClose();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
