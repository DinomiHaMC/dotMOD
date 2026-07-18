package com.dinomiha.dotmod.hud.widget;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.feature.freelook.FreelookController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class FreelookIndicatorWidget implements HudWidget {
    @Override
    public String id() {
        return HudWidgetDefaults.FREELOOK;
    }

    @Override
    public boolean hasContent(MinecraftClient client, boolean preview) {
        return preview || ConfigService.get().config().freelook.showIndicator
                && FreelookController.state() != FreelookController.RuntimeState.IDLE;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float alpha, boolean preview) {
        String key = !preview && FreelookController.state() == FreelookController.RuntimeState.RETURNING
                ? "hud.dotmod.freelook.returning" : "hud.dotmod.freelook.active";
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.translatable(key),
                50,
                1,
                HudPlacementResolver.applyAlpha(0xFFFFFFFF, alpha)
        );
    }
}
