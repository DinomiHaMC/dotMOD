package com.dinomiha.dotmod.hud;

public enum HudElement {
    HEARTS("hud.dotmod.element.hearts", 91, 9),
    ARMOR("hud.dotmod.element.armor", 91, 9),
    HUNGER("hud.dotmod.element.hunger", 91, 9),
    HOTBAR("hud.dotmod.element.hotbar", 182, 22),
    EXP_BAR("hud.dotmod.element.experience_bar", 182, 5),
    EXP_LEVEL("hud.dotmod.element.experience_level", 40, 12),
    EFFECTS("hud.dotmod.element.effects", 124, 28),
    BOSSBAR("hud.dotmod.element.bossbar", 182, 20),
    SCOREBOARD("hud.dotmod.element.scoreboard", 120, 120);

    private final String translationKey;
    private final int width;
    private final int height;

    HudElement(String translationKey, int width, int height) {
        this.translationKey = translationKey;
        this.width = width;
        this.height = height;
    }

    public String translationKey() {
        return translationKey;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
