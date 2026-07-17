package com.dinomiha.dotmod.hud.widget;

public record HudWidgetDefinition(
        String id,
        String translationKey,
        int width,
        int height,
        HudWidgetSettings defaults,
        boolean custom
) {
    public HudWidgetDefinition {
        if (id == null || id.isBlank() || translationKey == null || width <= 0 || height <= 0 || defaults == null) {
            throw new IllegalArgumentException("Invalid HUD widget definition");
        }
        defaults = defaults.copy();
    }

    @Override
    public HudWidgetSettings defaults() {
        return defaults.copy();
    }
}
