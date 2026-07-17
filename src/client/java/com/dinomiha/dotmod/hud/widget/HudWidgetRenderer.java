package com.dinomiha.dotmod.hud.widget;

import com.dinomiha.dotmod.config.ConfigService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class HudWidgetRenderer {
    private HudWidgetRenderer() {
    }

    public static void renderRuntime(DrawContext context, HudWidget widget) {
        MinecraftClient client = MinecraftClient.getInstance();
        var config = ConfigService.get().config();
        if (!config.general.enabled) {
            return;
        }
        if (widget.id().equals(HudWidgetDefaults.DURABILITY) && !config.durability.enabled) {
            return;
        }
        render(context, widget, config.hud.widget(widget.id()), client, false, false);
    }

    public static void renderPreview(
            DrawContext context,
            HudWidget widget,
            HudWidgetSettings settings,
            MinecraftClient client
    ) {
        render(context, widget, settings, client, true, true);
    }

    private static void render(
            DrawContext context,
            HudWidget widget,
            HudWidgetSettings settings,
            MinecraftClient client,
            boolean preview,
            boolean renderHidden
    ) {
        HudWidgetDefinition definition = HudWidgetDefaults.require(widget.id());
        HudPlacement placement = HudPlacementResolver.resolve(
                settings,
                definition.width(),
                definition.height(),
                context.getScaledWindowWidth(),
                context.getScaledWindowHeight()
        );
        if ((!placement.visible() && !renderHidden)
                || placement.alpha() <= 0.0F && !preview
                || !widget.hasContent(client, preview)) {
            return;
        }
        float alpha = placement.visible() ? placement.alpha() : Math.max(0.25F, placement.alpha() * 0.5F);
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(placement.x(), placement.y());
            context.getMatrices().scale(placement.scale(), placement.scale());
            widget.render(context, client, alpha, preview);
        } finally {
            context.getMatrices().popMatrix();
        }
    }
}
