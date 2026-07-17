package com.dinomiha.dotmod.hud.widget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudWidgetDefaults {
    public static final String HEARTS = "vanilla.hearts";
    public static final String ARMOR_BAR = "vanilla.armor_bar";
    public static final String HUNGER = "vanilla.hunger";
    public static final String HOTBAR = "vanilla.hotbar";
    public static final String EXPERIENCE_BAR = "vanilla.experience_bar";
    public static final String EXPERIENCE_LEVEL = "vanilla.experience_level";
    public static final String EFFECTS = "vanilla.effects";
    public static final String BOSSBAR = "vanilla.bossbar";
    public static final String SCOREBOARD = "vanilla.scoreboard";
    public static final String ARMOR = "dotmod.armor";
    public static final String COLORED_ONLINE = "dotmod.colored_online";
    public static final String DURABILITY = "dotmod.durability";

    private static final List<HudWidgetDefinition> DEFINITIONS = List.of(
            definition(HEARTS, "hud.dotmod.element.hearts", 91, 9, true, HudAnchor.BOTTOM_CENTER, -91, -39, false),
            definition(ARMOR_BAR, "hud.dotmod.element.armor", 91, 9, true, HudAnchor.BOTTOM_CENTER, -91, -49, false),
            definition(HUNGER, "hud.dotmod.element.hunger", 91, 9, true, HudAnchor.BOTTOM_CENTER, 10, -39, false),
            definition(HOTBAR, "hud.dotmod.element.hotbar", 182, 22, true, HudAnchor.BOTTOM_CENTER, -91, -22, false),
            definition(EXPERIENCE_BAR, "hud.dotmod.element.experience_bar", 182, 5, true, HudAnchor.BOTTOM_CENTER, -91, -31, false),
            definition(EXPERIENCE_LEVEL, "hud.dotmod.element.experience_level", 40, 12, true, HudAnchor.BOTTOM_CENTER, -20, -33, false),
            definition(EFFECTS, "hud.dotmod.element.effects", 124, 28, true, HudAnchor.TOP_RIGHT, -126, 2, false),
            definition(BOSSBAR, "hud.dotmod.element.bossbar", 182, 20, true, HudAnchor.TOP_CENTER, -91, 12, false),
            definition(SCOREBOARD, "hud.dotmod.element.scoreboard", 120, 120, true, HudAnchor.CENTER_RIGHT, -124, -60, false),
            definition(ARMOR, "hud.dotmod.widget.armor", 72, 18, true, HudAnchor.BOTTOM_LEFT, 8, -26, true),
            definition(COLORED_ONLINE, "hud.dotmod.widget.colored_online", 140, 84, true, HudAnchor.TOP_LEFT, 8, 8, true),
            definition(DURABILITY, "hud.dotmod.widget.durability", 126, 40, true, HudAnchor.BOTTOM_RIGHT, -134, -70, true)
    );
    private static final Map<String, HudWidgetDefinition> BY_ID;

    static {
        LinkedHashMap<String, HudWidgetDefinition> values = new LinkedHashMap<>();
        DEFINITIONS.forEach(definition -> values.put(definition.id(), definition));
        BY_ID = Map.copyOf(values);
    }

    private HudWidgetDefaults() {
    }

    public static List<HudWidgetDefinition> definitions() {
        return DEFINITIONS;
    }

    public static HudWidgetDefinition require(String id) {
        HudWidgetDefinition definition = BY_ID.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown HUD widget " + id);
        }
        return definition;
    }

    public static Map<String, HudWidgetSettings> settings() {
        LinkedHashMap<String, HudWidgetSettings> result = new LinkedHashMap<>();
        DEFINITIONS.forEach(definition -> result.put(definition.id(), definition.defaults()));
        return result;
    }

    private static HudWidgetDefinition definition(
            String id,
            String translation,
            int width,
            int height,
            boolean visible,
            HudAnchor anchor,
            int x,
            int y,
            boolean custom
    ) {
        return new HudWidgetDefinition(
                id, translation, width, height,
                new HudWidgetSettings(visible, anchor, x, y, 1.0F, 1.0F), custom
        );
    }
}
