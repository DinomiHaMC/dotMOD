package com.dinomiha.dotmod.hud.widget;

import com.dinomiha.dotmod.feature.togglewalk.MovementLifecycle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class MovementIndicatorWidget implements HudWidget {
    @Override
    public String id() {
        return HudWidgetDefaults.MOVEMENT;
    }

    @Override
    public boolean hasContent(MinecraftClient client, boolean preview) {
        var snapshot = MovementLifecycle.snapshot();
        return preview || snapshot.movementActive() || snapshot.toggleSprintActive() || snapshot.sneaking();
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float alpha, boolean preview) {
        var snapshot = MovementLifecycle.snapshot();
        var forced = snapshot.forcedKeys();
        List<Text> components = new ArrayList<>();
        if (preview || forced.forward()) components.add(Text.translatable("hud.dotmod.movement.walk"));
        if (preview || forced.sprint()) components.add(Text.translatable("hud.dotmod.movement.sprint"));
        if (preview || forced.jump()) components.add(Text.translatable("hud.dotmod.movement.space"));
        if (preview || forced.sneak()) components.add(Text.translatable("hud.dotmod.movement.sneak"));
        Text label = Text.empty();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) label = label.copy().append(" + ");
            label = label.copy().append(components.get(i));
        }
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                label,
                110,
                1,
                HudPlacementResolver.applyAlpha(0xFFFFFFFF, alpha)
        );
    }
}
