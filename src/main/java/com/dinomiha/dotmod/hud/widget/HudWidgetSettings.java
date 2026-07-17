package com.dinomiha.dotmod.hud.widget;

public final class HudWidgetSettings {
    public boolean visible;
    public HudAnchor anchor;
    public int offsetX;
    public int offsetY;
    public float scale;
    public float alpha;

    public HudWidgetSettings() {
        this(true, HudAnchor.TOP_LEFT, 0, 0, 1.0F, 1.0F);
    }

    public HudWidgetSettings(
            boolean visible,
            HudAnchor anchor,
            int offsetX,
            int offsetY,
            float scale,
            float alpha
    ) {
        this.visible = visible;
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.scale = scale;
        this.alpha = alpha;
    }

    public HudWidgetSettings copy() {
        return new HudWidgetSettings(visible, anchor, offsetX, offsetY, scale, alpha);
    }
}
