package com.dinomiha.dotmod.gui;

import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.hud.HudLayout;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudElement dragging;
    private int dragStartMouseX;
    private int dragStartMouseY;
    private int dragStartDx;
    private int dragStartDy;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("dotMOD HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> DotModConfig.resetHud())
                .dimensions(8, 8, 80, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(8, 32, 80, 20)
                .build());
    }

    @Override
    public void close() {
        DotModConfig.save();
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
        context.fill(0, 0, width, height, 0x88000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        renderGrid(context);
        for (HudElement element : HudElement.values()) {
            HudLayout.Rect rect = HudLayout.rect(element, width, height);
            boolean hovered = mouseX >= rect.x() && mouseX <= rect.x() + rect.width() && mouseY >= rect.y() && mouseY <= rect.y() + rect.height();
            int color = hovered || element == dragging ? 0xAA55FF55 : 0xAAFFFFFF;
            context.drawStrokedRectangle(rect.x(), rect.y(), rect.width(), rect.height(), color);
            context.drawTextWithShadow(textRenderer, element.displayName(), rect.x() + 3, rect.y() + 3, 0xFFFFFF);
        }
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Drag elements. Positions are saved as dx/dy offsets."), width / 2, 24, 0xA0A0A0);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0) {
            for (HudElement element : HudElement.values()) {
                HudLayout.Rect rect = HudLayout.rect(element, width, height);
                if (mouseX >= rect.x() && mouseX <= rect.x() + rect.width() && mouseY >= rect.y() && mouseY <= rect.y() + rect.height()) {
                    DotModConfig.HudOffset offset = DotModConfig.get().hudOffset(element);
                    dragging = element;
                    dragStartMouseX = (int) mouseX;
                    dragStartMouseY = (int) mouseY;
                    dragStartDx = offset.dx;
                    dragStartDy = offset.dy;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (dragging != null && click.button() == 0) {
            DotModConfig config = DotModConfig.get();
            DotModConfig.HudOffset offset = config.hudOffset(dragging);
            int dx = dragStartDx + (int) mouseX - dragStartMouseX;
            int dy = dragStartDy + (int) mouseY - dragStartMouseY;
            if (config.hudSnapToGrid) {
                int grid = Math.max(1, config.hudGridSize);
                dx = Math.round(dx / (float) grid) * grid;
                dy = Math.round(dy / (float) grid) * grid;
            }
            offset.dx = dx;
            offset.dy = dy;
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null && click.button() == 0) {
            dragging = null;
            DotModConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    private void renderGrid(DrawContext context) {
        DotModConfig config = DotModConfig.get();
        if (!config.hudSnapToGrid) {
            return;
        }
        int grid = Math.max(2, config.hudGridSize * 8);
        for (int x = 0; x < width; x += grid) {
            context.fill(x, 0, x + 1, height, 0x22000000);
        }
        for (int y = 0; y < height; y += grid) {
            context.fill(0, y, width, y + 1, 0x22000000);
        }
    }
}
