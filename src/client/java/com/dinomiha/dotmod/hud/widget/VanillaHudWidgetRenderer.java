package com.dinomiha.dotmod.hud.widget;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.util.List;

/** Exception-safe wrappers around vanilla HUD layers. */
public final class VanillaHudWidgetRenderer {
    private static final List<Mapping> MAPPINGS = List.of(
            new Mapping(VanillaHudElements.HOTBAR, HudElement.HOTBAR),
            new Mapping(VanillaHudElements.ARMOR_BAR, HudElement.ARMOR),
            new Mapping(VanillaHudElements.HEALTH_BAR, HudElement.HEARTS),
            new Mapping(VanillaHudElements.FOOD_BAR, HudElement.HUNGER),
            new Mapping(VanillaHudElements.INFO_BAR, HudElement.EXP_BAR),
            new Mapping(VanillaHudElements.EXPERIENCE_LEVEL, HudElement.EXP_LEVEL),
            new Mapping(VanillaHudElements.STATUS_EFFECTS, HudElement.EFFECTS),
            new Mapping(VanillaHudElements.BOSS_BAR, HudElement.BOSSBAR),
            new Mapping(VanillaHudElements.SCOREBOARD, HudElement.SCOREBOARD)
    );

    private VanillaHudWidgetRenderer() {
    }

    public static void register() {
        for (Mapping mapping : MAPPINGS) {
            HudElementRegistry.replaceElement(mapping.vanillaId, original ->
                    (context, tickCounter) -> render(context, tickCounter, mapping.element, original));
        }
    }

    private static void render(
            DrawContext context,
            RenderTickCounter tickCounter,
            HudElement element,
            net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement original
    ) {
        var config = ConfigService.get().config();
        if (!config.general.enabled) {
            original.render(context, tickCounter);
            return;
        }
        HudWidgetSettings settings = config.hud.widget(element.widgetId());
        if (!settings.visible || settings.alpha <= 0.0F) {
            return;
        }
        HudPlacement placement = HudPlacementResolver.resolve(
                settings,
                element.width(),
                element.height(),
                context.getScaledWindowWidth(),
                context.getScaledWindowHeight()
        );
        HudWidgetSettings defaults = HudWidgetDefaults.require(element.widgetId()).defaults();
        int baseX = defaults.anchor.x(context.getScaledWindowWidth()) + defaults.offsetX;
        int baseY = defaults.anchor.y(context.getScaledWindowHeight()) + defaults.offsetY;
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(
                    placement.x() - baseX * placement.scale(),
                    placement.y() - baseY * placement.scale()
            );
            context.getMatrices().scale(placement.scale(), placement.scale());
            original.render(context, tickCounter);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    private record Mapping(Identifier vanillaId, HudElement element) {
    }
}
