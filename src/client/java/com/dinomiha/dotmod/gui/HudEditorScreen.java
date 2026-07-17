package com.dinomiha.dotmod.gui;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.hud.editor.HudContextMenu;
import com.dinomiha.dotmod.hud.editor.HudEditorController;
import com.dinomiha.dotmod.hud.widget.HudPlacement;
import com.dinomiha.dotmod.hud.widget.HudWidgetDefaults;
import com.dinomiha.dotmod.hud.widget.HudWidgetRegistry;
import com.dinomiha.dotmod.hud.widget.HudWidgetRenderer;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.dinomiha.dotmod.ui.component.DotButton;
import com.dinomiha.dotmod.ui.component.DotConfirmationDialog;
import com.dinomiha.dotmod.ui.component.DotContextMenu;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public final class HudEditorScreen extends Screen {
    private static final int GUIDE_COLOR = 0xDD55FFFF;
    private final Screen parent;
    private final HudEditorController controller = new HudEditorController();
    private DotContextMenu contextMenu;

    public HudEditorScreen(Screen parent) {
        super(Text.translatable("screen.dotmod.hud_editor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        controller.stopDrag();
        addDrawableChild(DotButton.create(8, 8, 80, Text.translatable("screen.dotmod.hud_editor.reset"), button -> {
            if (client != null) {
                DotConfirmationDialog.open(
                        client, this,
                        Text.translatable("screen.dotmod.hud_editor.reset.title"),
                        Text.translatable("screen.dotmod.hud_editor.reset.message"),
                        () -> {
                            if (!ConfigService.get().resetHud()) {
                                MessageService.sendChat(Text.translatable("message.dotmod.config.save_failed"), MessageType.ERROR);
                            }
                        }
                );
            }
        }));
        addDrawableChild(DotButton.create(8, 32, 80, Text.translatable("gui.done"), button -> close()));
        contextMenu = addDrawableChild(new DotContextMenu(textRenderer));
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (contextMenu != null && contextMenu.visible && input.isEscape()) {
            contextMenu.hide();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0xB0101010);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        renderGrid(context);
        for (var definition : HudWidgetDefaults.definitions()) {
            var widget = HudWidgetRegistry.get(definition.id());
            if (widget != null && client != null) {
                HudWidgetRenderer.renderPreview(context, widget, controller.settings(definition.id()), client);
            }
        }
        renderGuide(context, controller.verticalGuide(), true);
        renderGuide(context, controller.horizontalGuide(), false);
        for (var definition : HudWidgetDefaults.definitions()) {
            HudPlacement placement = controller.placement(definition, width, height);
            boolean hovered = placement.contains(mouseX, mouseY);
            boolean selected = definition.id().equals(controller.draggingId());
            int color = !controller.settings(definition.id()).visible
                    ? 0xAA888888
                    : hovered || selected ? 0xDD55FF55 : 0xAAFFFFFF;
            context.drawStrokedRectangle(
                    placement.x(), placement.y(), placement.width(), placement.height(), color
            );
            context.drawTextWithShadow(
                    textRenderer,
                    textRenderer.trimToWidth(Text.translatable(definition.translationKey()).getString(),
                            Math.max(8, placement.width() - 6)),
                    placement.x() + 3,
                    placement.y() + 3,
                    0xFFFFFF
            );
        }
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        List<OrderedText> help = textRenderer.wrapLines(
                Text.translatable("screen.dotmod.hud_editor.help"), Math.max(80, width - 192)
        );
        for (int index = 0; index < help.size(); index++) {
            context.drawCenteredTextWithShadow(textRenderer, help.get(index), width / 2, 24 + index * 10, 0xA0A0A0);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (contextMenu.visible) {
            if (contextMenu.isMouseOver(click.x(), click.y())) {
                contextMenu.mouseClicked(click, doubled);
            } else {
                contextMenu.hide();
            }
            return true;
        }
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        contextMenu.hide();
        var definitions = HudWidgetDefaults.definitions();
        for (int index = definitions.size() - 1; index >= 0; index--) {
            var definition = definitions.get(index);
            HudPlacement placement = controller.placement(definition, width, height);
            if (!placement.contains(click.x(), click.y())) {
                continue;
            }
            if (click.button() == 0) {
                controller.startDrag(definition, (int) click.x(), (int) click.y(), width, height);
                return true;
            }
            if (click.button() == 1) {
                int menuWidth = Math.min(160, width - 8);
                var actions = HudContextMenu.actions(
                        controller, definition.id(), width, height, this::save
                );
                int menuHeight = DotContextMenu.ROW_HEIGHT * actions.size() + 2;
                int x = Math.max(4, Math.min(width - menuWidth - 4, (int) click.x()));
                int y = Math.max(4, Math.min(height - menuHeight - 4, (int) click.y()));
                contextMenu.show(x, y, menuWidth, actions);
                setFocused(contextMenu);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        return click.button() == 0 && controller.drag((int) click.x(), (int) click.y(), width, height)
                || super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && controller.draggingId() != null) {
            controller.stopDrag();
            save();
            return true;
        }
        return super.mouseReleased(click);
    }

    private void renderGrid(DrawContext context) {
        var hud = ConfigService.get().config().hud;
        if (!hud.snapToGrid) {
            return;
        }
        int grid = Math.max(1, hud.gridSize);
        for (int x = 0; x < width; x += grid) {
            context.fill(x, 0, x + 1, height, 0x10000000);
        }
        for (int y = 0; y < height; y += grid) {
            context.fill(0, y, width, y + 1, 0x10000000);
        }
    }

    private void renderGuide(DrawContext context, Integer position, boolean vertical) {
        if (position == null) {
            return;
        }
        if (vertical) {
            context.fill(position, 0, position + 1, height, GUIDE_COLOR);
        } else {
            context.fill(0, position, width, position + 1, GUIDE_COLOR);
        }
    }

    private void save() {
        if (!ConfigService.get().save()) {
            MessageService.sendChat(Text.translatable("message.dotmod.config.save_failed"), MessageType.ERROR);
        }
    }
}
