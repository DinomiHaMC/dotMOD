package com.dinomiha.dotmod.hud.widget;

public enum HudAnchor {
    TOP_LEFT(0, 0),
    TOP_CENTER(1, 0),
    TOP_RIGHT(2, 0),
    CENTER_LEFT(0, 1),
    CENTER(1, 1),
    CENTER_RIGHT(2, 1),
    BOTTOM_LEFT(0, 2),
    BOTTOM_CENTER(1, 2),
    BOTTOM_RIGHT(2, 2);

    private final int horizontal;
    private final int vertical;

    HudAnchor(int horizontal, int vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public int x(int screenWidth) {
        return horizontal == 0 ? 0 : horizontal == 1 ? screenWidth / 2 : screenWidth;
    }

    public int y(int screenHeight) {
        return vertical == 0 ? 0 : vertical == 1 ? screenHeight / 2 : screenHeight;
    }

    public HudAnchor next() {
        HudAnchor[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
