package com.dinomiha.dotmod.hud.widget;

import com.dinomiha.dotmod.feature.togglewalk.MovementLifecycle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class MovementIndicatorWidget implements HudWidget {
    @Override
    public String id() {
        return HudWidgetDefaults.MOVEMENT;
    }

    @Override
    public boolean hasContent(MinecraftClient client, boolean preview) {
        var snapshot = MovementLifecycle.snapshot();
        return preview || snapshot.walking() || snapshot.sneaking();
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float alpha, boolean preview) {
        var snapshot = MovementLifecycle.snapshot();
        String key;
        if (preview || snapshot.walking() && snapshot.sprintRetentionArmed() && snapshot.sneaking()) {
            key = "hud.dotmod.movement.walk_sprint_sneak";
        } else if (snapshot.walking() && snapshot.sprintRetentionArmed()) {
            key = "hud.dotmod.movement.walk_sprint";
        } else if (snapshot.walking() && snapshot.sneaking()) {
            key = "hud.dotmod.movement.walk_sneak";
        } else {
            key = snapshot.walking() ? "hud.dotmod.movement.walk" : "hud.dotmod.movement.sneak";
        }
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.translatable(key),
                50,
                1,
                HudPlacementResolver.applyAlpha(0xFFFFFFFF, alpha)
        );
    }
}
