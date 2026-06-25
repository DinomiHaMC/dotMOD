package com.dinomiha.dotmod.hud;

import com.dinomiha.dotmod.config.DotModConfig;
import net.minecraft.client.gui.DrawContext;

public final class HudTransform {
    private HudTransform() {
    }

    public static void push(DrawContext context, HudElement element) {
        DotModConfig.HudOffset offset = HudLayout.activeOffset(element);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(offset.dx, offset.dy);
    }

    public static void pop(DrawContext context) {
        context.getMatrices().popMatrix();
    }
}
