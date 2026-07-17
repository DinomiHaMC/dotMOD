package com.dinomiha.dotmod.ui.component;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.function.Consumer;

public final class DotColorPicker extends ClickableWidget {
    private final Consumer<String> listener;
    private String color;

    public DotColorPicker(int x, int y, int width, int height, String initialColor, Consumer<String> listener) {
        super(x, y, width, height, Text.translatable("screen.dotmod.recolor.picker"));
        this.listener = listener;
        this.color = initialColor;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        float hue = (float) Math.clamp((click.x() - getX()) / getWidth(), 0.0, 0.999999);
        float brightness = 1.0F - (float) Math.clamp((click.y() - getY()) / getHeight(), 0.0, 1.0);
        int rgb = Color.HSBtoRGB(hue, 1.0F, brightness) & 0xFFFFFF;
        color = String.format("#%06X", rgb);
        listener.accept(color);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        for (int x = 0; x < getWidth(); x++) {
            float hue = x / (float) getWidth();
            int top = 0xFF000000 | (Color.HSBtoRGB(hue, 1.0F, 1.0F) & 0xFFFFFF);
            context.fillGradient(getX() + x, getY(), getX() + x + 1, getY() + getHeight(), top, 0xFF000000);
        }
        context.drawStrokedRectangle(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
        try {
            int rgb = Integer.parseInt(color.substring(1), 16);
            context.fill(getX() + 4, getY() + 4, getX() + 20, getY() + 20, 0xFF000000 | rgb);
            context.drawStrokedRectangle(getX() + 3, getY() + 3, 18, 18, 0xFFFFFFFF);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
