package com.dinomiha.dotmod.hud;

public enum HudElement {
    HEARTS("Hearts", 91, 9),
    ARMOR("Armor", 91, 9),
    HUNGER("Hunger", 91, 9),
    HOTBAR("Hotbar", 182, 22),
    EXP_BAR("Experience Bar", 182, 5),
    EXP_LEVEL("Experience Level", 40, 12),
    EFFECTS("Effects", 124, 28),
    BOSSBAR("Bossbar", 182, 20),
    SCOREBOARD("Scoreboard", 120, 120);

    private final String displayName;
    private final int width;
    private final int height;

    HudElement(String displayName, int width, int height) {
        this.displayName = displayName;
        this.width = width;
        this.height = height;
    }

    public String displayName() {
        return displayName;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
