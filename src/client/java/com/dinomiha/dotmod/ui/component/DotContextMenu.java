package com.dinomiha.dotmod.ui.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;

public final class DotContextMenu extends ClickableWidget {
    public static final int ROW_HEIGHT = 20;

    private final TextRenderer textRenderer;
    private List<Action> actions = List.of();

    public DotContextMenu(TextRenderer textRenderer) {
        super(0, 0, 1, 1, Text.translatable("screen.dotmod.context_menu"));
        this.textRenderer = textRenderer;
        hide();
    }

    public void show(int x, int y, int width, List<Action> actions) {
        this.actions = List.copyOf(actions);
        setDimensionsAndPosition(width, actions.size() * ROW_HEIGHT + 2, x, y);
        visible = true;
        active = true;
    }

    public void hide() {
        visible = false;
        active = false;
        actions = List.of();
        setFocused(false);
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        int index = ((int) click.y() - getY() - 1) / ROW_HEIGHT;
        if (index >= 0 && index < actions.size() && actions.get(index).enabled()) {
            Runnable callback = actions.get(index).callback();
            hide();
            callback.run();
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(getX(), getY(), getRight(), getBottom(), 0xFF888888);
        context.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, 0xF0202020);
        int hovered = isMouseOver(mouseX, mouseY) ? (mouseY - getY() - 1) / ROW_HEIGHT : -1;
        for (int index = 0; index < actions.size(); index++) {
            Action action = actions.get(index);
            int rowY = getY() + 1 + index * ROW_HEIGHT;
            if (index == hovered) {
                context.fill(getX() + 1, rowY, getRight() - 1, rowY + ROW_HEIGHT, action.enabled() ? 0x805555AA : 0x40404040);
            }
            context.drawTextWithShadow(
                    textRenderer,
                    textRenderer.trimToWidth(action.label().getString(), Math.max(1, getWidth() - 8)),
                    getX() + 4,
                    rowY + 6,
                    action.enabled() ? 0xFFFFFF : 0x777777
            );
        }
        if (hovered >= 0 && hovered < actions.size() && actions.get(hovered).tooltip() != null) {
            context.drawTooltip(textRenderer, actions.get(hovered).tooltip(), mouseX, mouseY);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    public record Action(Text label, Text tooltip, boolean enabled, Runnable callback) {
    }
}
