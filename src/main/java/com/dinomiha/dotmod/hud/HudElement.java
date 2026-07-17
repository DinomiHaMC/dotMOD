package com.dinomiha.dotmod.hud;

public enum HudElement {
    HEARTS("vanilla.hearts", "hud.dotmod.element.hearts", 91, 9),
    ARMOR("vanilla.armor_bar", "hud.dotmod.element.armor", 91, 9),
    HUNGER("vanilla.hunger", "hud.dotmod.element.hunger", 91, 9),
    HOTBAR("vanilla.hotbar", "hud.dotmod.element.hotbar", 182, 22),
    EXP_BAR("vanilla.experience_bar", "hud.dotmod.element.experience_bar", 182, 5),
    EXP_LEVEL("vanilla.experience_level", "hud.dotmod.element.experience_level", 40, 12),
    EFFECTS("vanilla.effects", "hud.dotmod.element.effects", 124, 28),
    BOSSBAR("vanilla.bossbar", "hud.dotmod.element.bossbar", 182, 20),
    SCOREBOARD("vanilla.scoreboard", "hud.dotmod.element.scoreboard", 120, 120);

    private final String widgetId;
    private final String translationKey;
    private final int width;
    private final int height;

    HudElement(String widgetId, String translationKey, int width, int height) {
        this.widgetId = widgetId;
        this.translationKey = translationKey;
        this.width = width;
        this.height = height;
    }

    public String widgetId() {
        return widgetId;
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
