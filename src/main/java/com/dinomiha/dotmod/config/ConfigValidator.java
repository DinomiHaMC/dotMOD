package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static void validate(DotModConfig config) {
        if (config.schemaVersion > DotModConfig.CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedDataVersionException("Unsupported dotMOD config schema " + config.schemaVersion);
        }
        config.schemaVersion = DotModConfig.CURRENT_SCHEMA_VERSION;
        config.general = config.general == null ? new GeneralConfig() : config.general;
        config.commands = config.commands == null ? new CommandsConfig() : config.commands;
        config.hud = config.hud == null ? new HudConfig() : config.hud;
        config.quickCraft = config.quickCraft == null ? new QuickCraftConfig() : config.quickCraft;
        config.inventoryPresets = config.inventoryPresets == null ? new InventoryPresetsConfig() : config.inventoryPresets;
        config.inventorySearch = feature(config.inventorySearch);
        config.durability = feature(config.durability);
        config.screenshots = feature(config.screenshots);
        config.deathHistory = feature(config.deathHistory);
        config.toggleWalk = config.toggleWalk == null ? new ToggleWalkConfig() : config.toggleWalk;
        config.freelook = feature(config.freelook);
        config.playerColors = config.playerColors == null ? new PlayerColorsConfig() : config.playerColors;
        config.commandAliases = feature(config.commandAliases);
        config.keybinds = config.keybinds == null ? new KeybindsConfig() : config.keybinds;
        config.interfaceConfig = config.interfaceConfig == null ? new InterfaceConfig() : config.interfaceConfig;
        config.inventoryPresets.panelSide = config.inventoryPresets.panelSide == null
                ? PresetPanelSide.AUTO
                : config.inventoryPresets.panelSide;

        config.commands.prefix = config.commands.prefix == null ? MessagePrefixMode.DOTMOD_COLON : config.commands.prefix;
        config.commands.customPrefix = text(config.commands.customPrefix, "dotMod:", 32);

        config.quickCraft.slots2x2 = slots(config.quickCraft.slots2x2, List.of(9, 10, 18, 19));
        config.quickCraft.slots3x3 = slots(config.quickCraft.slots3x3, List.of(9, 10, 11, 18, 19, 20, 27, 28, 29));
        config.quickCraft.buttonText = text(config.quickCraft.buttonText, "Craft", 32);
        config.quickCraft.buttonOffsetX = clamp(config.quickCraft.buttonOffsetX, -4096, 4096);
        config.quickCraft.buttonOffsetY = clamp(config.quickCraft.buttonOffsetY, -4096, 4096);

        config.hud.editorButtonText = text(config.hud.editorButtonText, "HUD", 32);
        config.hud.editorButtonOffsetX = clamp(config.hud.editorButtonOffsetX, -4096, 4096);
        config.hud.editorButtonOffsetY = clamp(config.hud.editorButtonOffsetY, -4096, 4096);
        config.hud.gridSize = clamp(config.hud.gridSize, 1, 16);
        config.hud.magneticSnapDistance = clamp(config.hud.magneticSnapDistance, 1, 16);
        config.hud.offsets = offsets(config.hud.offsets);
        config.hud.uniformNameTags = config.hud.uniformNameTags == null ? new UniformNameTagsConfig() : config.hud.uniformNameTags;
        if (!Float.isFinite(config.hud.uniformNameTags.size)) {
            config.hud.uniformNameTags.size = 1.0F;
        }
        config.hud.uniformNameTags.size = Math.max(0.1F, Math.min(5.0F, config.hud.uniformNameTags.size));
        config.hud.uniformNameTags.backgroundColor = color(config.hud.uniformNameTags.backgroundColor, "#000000");

        config.playerColors.greenColor = color(config.playerColors.greenColor, "#55FF55");
        config.playerColors.redColor = color(config.playerColors.redColor, "#FF5555");
        config.playerColors.defaultColor = color(config.playerColors.defaultColor, "#FFFFFF");
        config.toggleWalk.toggleShift = config.toggleWalk.toggleShift == null ? new ToggleShiftConfig() : config.toggleWalk.toggleShift;
    }

    private static FeatureConfig feature(FeatureConfig value) {
        return value == null ? new FeatureConfig() : value;
    }

    private static List<Integer> slots(List<Integer> values, List<Integer> fallback) {
        if (values == null) {
            return new ArrayList<>(fallback);
        }
        LinkedHashSet<Integer> valid = new LinkedHashSet<>();
        for (Integer value : values) {
            if (value != null && value >= 0 && value <= 35) {
                valid.add(value);
            }
        }
        return valid.isEmpty() ? new ArrayList<>(fallback) : new ArrayList<>(valid);
    }

    private static Map<HudElement, DotModConfig.HudOffset> offsets(Map<HudElement, DotModConfig.HudOffset> values) {
        EnumMap<HudElement, DotModConfig.HudOffset> result = new EnumMap<>(HudElement.class);
        if (values != null) {
            values.forEach((element, offset) -> {
                if (element != null && offset != null) {
                    offset.dx = clamp(offset.dx, -16384, 16384);
                    offset.dy = clamp(offset.dy, -16384, 16384);
                    result.put(element, offset);
                }
            });
        }
        for (HudElement element : HudElement.values()) {
            result.computeIfAbsent(element, ignored -> new DotModConfig.HudOffset());
        }
        return result;
    }

    private static String color(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!hex.matches("(?i)[0-9a-f]{6}")) {
            return fallback;
        }
        return "#" + hex.toUpperCase();
    }

    private static String text(String value, String fallback, int maxLength) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
