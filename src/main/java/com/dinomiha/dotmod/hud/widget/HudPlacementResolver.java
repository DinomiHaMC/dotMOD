package com.dinomiha.dotmod.hud.widget;

public final class HudPlacementResolver {
    private HudPlacementResolver() {
    }

    public static HudPlacement resolve(
            HudWidgetSettings settings,
            int contentWidth,
            int contentHeight,
            int screenWidth,
            int screenHeight
    ) {
        if (settings == null || contentWidth <= 0 || contentHeight <= 0 || screenWidth < 0 || screenHeight < 0) {
            throw new IllegalArgumentException("Invalid HUD placement input");
        }
        float scale = finiteClamp(settings.scale, 0.25F, 4.0F, 1.0F);
        if (screenWidth > 0 && screenHeight > 0) {
            float fitScale = Math.min((float) screenWidth / contentWidth, (float) screenHeight / contentHeight);
            scale = Math.min(scale, fitScale);
        }
        float alpha = finiteClamp(settings.alpha, 0.0F, 1.0F, 1.0F);
        HudAnchor anchor = settings.anchor == null ? HudAnchor.TOP_LEFT : settings.anchor;
        int scaledWidth = Math.min(screenWidth, Math.max(1, (int) Math.ceil(contentWidth * scale)));
        int scaledHeight = Math.min(screenHeight, Math.max(1, (int) Math.ceil(contentHeight * scale)));
        int rawX = anchor.x(screenWidth) + settings.offsetX;
        int rawY = anchor.y(screenHeight) + settings.offsetY;
        int x = clamp(rawX, 0, Math.max(0, screenWidth - scaledWidth));
        int y = clamp(rawY, 0, Math.max(0, screenHeight - scaledHeight));
        return new HudPlacement(x, y, scaledWidth, scaledHeight, scale, alpha, settings.visible);
    }

    public static void preservePositionForAnchor(
            HudWidgetSettings settings,
            HudAnchor next,
            int absoluteX,
            int absoluteY,
            int screenWidth,
            int screenHeight
    ) {
        settings.anchor = next;
        settings.offsetX = absoluteX - next.x(screenWidth);
        settings.offsetY = absoluteY - next.y(screenHeight);
    }

    public static int applyAlpha(int argb, float alpha) {
        int sourceAlpha = argb >>> 24;
        int nextAlpha = Math.round(sourceAlpha * finiteClamp(alpha, 0.0F, 1.0F, 1.0F));
        return nextAlpha << 24 | argb & 0x00FFFFFF;
    }

    private static float finiteClamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
