package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.hud.widget.HudWidgetDefaults;
import com.dinomiha.dotmod.hud.widget.HudWidgetSettings;
import com.dinomiha.dotmod.hud.widget.HudWidgetDefaults;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static void validate(DotModConfig config) {
        if (config.schemaVersion > DotModConfig.CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedDataVersionException("Unsupported dotMOD config schema " + config.schemaVersion);
        }
        int loadedSchema = config.schemaVersion;
        config.general = config.general == null ? new GeneralConfig() : config.general;
        config.commands = config.commands == null ? new CommandsConfig() : config.commands;
        config.hud = config.hud == null ? new HudConfig() : config.hud;
        config.quickCraft = config.quickCraft == null ? new QuickCraftConfig() : config.quickCraft;
        config.inventoryPresets = config.inventoryPresets == null ? new InventoryPresetsConfig() : config.inventoryPresets;
        config.inventorySearch = config.inventorySearch == null ? new InventorySearchConfig() : config.inventorySearch;
        if (config.inventorySearch.enabled == null) {
            config.inventorySearch.enabled = true;
        }
        config.durability = config.durability == null ? new DurabilityConfig() : config.durability;
        config.screenshots = feature(config.screenshots);
        config.deathHistory = feature(config.deathHistory);
        config.commandAliases = feature(config.commandAliases);
        if (loadedSchema < 6) {
            config.inventorySearch.enabled = true;
            config.durability.enabled = true;
            config.commandAliases.enabled = true;
            config.screenshots.enabled = true;
            config.deathHistory.enabled = true;
        }
        config.toggleWalk = config.toggleWalk == null ? new ToggleWalkConfig() : config.toggleWalk;
        config.freelook = config.freelook == null ? new FreelookConfig() : config.freelook;
        if (loadedSchema < 7) {
            config.toggleWalk.enabled = true;
            config.freelook.enabled = true;
        }
        config.fullBrightness = feature(config.fullBrightness);
        if (loadedSchema < 8) {
            config.fullBrightness.enabled = true;
            config.freelook.perspective = FreelookPerspective.SWITCH_TO_THIRD_PERSON_BACK;
            HudWidgetSettings movement = config.hud.widgets.get(HudWidgetDefaults.MOVEMENT);
            if (movement != null) {
                movement.offsetX -= 60;
            }
        }
        config.toggleWalk.toggleSprint = config.toggleWalk.toggleSprint == null
                ? new ToggleSprintConfig() : config.toggleWalk.toggleSprint;
        if (loadedSchema < 9) {
            config.toggleWalk.toggleSprint.enabled = true;
        }
        config.playerColors = config.playerColors == null ? new PlayerColorsConfig() : config.playerColors;
        config.keybinds = config.keybinds == null ? new KeybindsConfig() : config.keybinds;
        config.interfaceConfig = config.interfaceConfig == null ? new InterfaceConfig() : config.interfaceConfig;
        config.inventoryPresets.panelSide = config.inventoryPresets.panelSide == null
                ? PresetPanelSide.AUTO
                : config.inventoryPresets.panelSide;
        config.inventorySearch.displayMode = config.inventorySearch.displayMode == null
                ? InventorySearchDisplayMode.DIM
                : config.inventorySearch.displayMode;

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
        if (loadedSchema < 4) {
            migrateHudWidgets(config.hud);
        }
        config.hud.widgets = widgets(config.hud.widgets);
        config.hud.offsets = null;
        config.hud.uniformNameTags = config.hud.uniformNameTags == null ? new UniformNameTagsConfig() : config.hud.uniformNameTags;
        if (!Float.isFinite(config.hud.uniformNameTags.size)) {
            config.hud.uniformNameTags.size = 1.0F;
        }
        config.hud.uniformNameTags.size = Math.max(0.1F, Math.min(5.0F, config.hud.uniformNameTags.size));
        config.hud.uniformNameTags.backgroundColor = color(config.hud.uniformNameTags.backgroundColor, "#000000");

        config.playerColors.greenColor = color(config.playerColors.greenColor, "#55FF55");
        config.playerColors.redColor = color(config.playerColors.redColor, "#FF5555");
        config.playerColors.defaultColor = color(config.playerColors.defaultColor, "#FFFFFF");
        if (!Float.isFinite(config.durability.warningThreshold)) {
            config.durability.warningThreshold = 0.15F;
        }
        config.durability.warningThreshold = Math.max(0.0F, Math.min(1.0F, config.durability.warningThreshold));
        config.durability.warningCooldownSeconds = clamp(config.durability.warningCooldownSeconds, 0, 86400);
        config.durability.lowColor = color(config.durability.lowColor, "#FF5555");
        config.durability.middleColor = color(config.durability.middleColor, "#FFFF55");
        config.durability.highColor = color(config.durability.highColor, "#55FF55");
        config.toggleWalk.toggleShift = config.toggleWalk.toggleShift == null ? new ToggleShiftConfig() : config.toggleWalk.toggleShift;
        config.freelook.activation = config.freelook.activation == null
                ? FreelookActivation.HOLD : config.freelook.activation;
        config.freelook.perspective = config.freelook.perspective == null
                ? FreelookPerspective.SWITCH_TO_THIRD_PERSON_BACK : config.freelook.perspective;
        config.freelook.sensitivity = finite(config.freelook.sensitivity, 1.0F, 0.1F, 4.0F);
        config.freelook.returnDurationMs = clamp(config.freelook.returnDurationMs, 0, 1000);
        config.schemaVersion = DotModConfig.CURRENT_SCHEMA_VERSION;
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

    private static void migrateHudWidgets(HudConfig hud) {
        LinkedHashMap<String, HudWidgetSettings> migrated = new LinkedHashMap<>();
        if (hud.widgets != null) {
            hud.widgets.forEach((id, settings) -> {
                if (id != null && settings != null) {
                    migrated.put(id, settings);
                }
            });
        }
        HudWidgetDefaults.settings().forEach(migrated::putIfAbsent);
        hud.widgets = migrated;
        if (hud.offsets == null) {
            return;
        }
        hud.offsets.forEach((element, offset) -> {
            if (element != null && offset != null) {
                HudWidgetSettings settings = hud.widgets.get(element.widgetId());
                    settings.offsetX += clamp(offset.dx, -16384, 16384);
                    settings.offsetY += clamp(offset.dy, -16384, 16384);
            }
        });
    }

    private static Map<String, HudWidgetSettings> widgets(Map<String, HudWidgetSettings> values) {
        LinkedHashMap<String, HudWidgetSettings> result = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((id, settings) -> {
                if (id != null && !id.isBlank() && id.length() <= 128 && settings != null) {
                    settings.anchor = settings.anchor == null
                            ? defaultSettings(id).anchor
                            : settings.anchor;
                    settings.offsetX = clamp(settings.offsetX, -32768, 32768);
                    settings.offsetY = clamp(settings.offsetY, -32768, 32768);
                    settings.scale = finite(settings.scale, 1.0F, 0.25F, 4.0F);
                    settings.alpha = finite(settings.alpha, 1.0F, 0.0F, 1.0F);
                    result.put(id, settings);
                }
            });
        }
        HudWidgetDefaults.definitions().forEach(definition ->
                result.putIfAbsent(definition.id(), definition.defaults()));
        return result;
    }

    private static HudWidgetSettings defaultSettings(String id) {
        return HudWidgetDefaults.definitions().stream()
                .filter(definition -> definition.id().equals(id))
                .findFirst()
                .map(com.dinomiha.dotmod.hud.widget.HudWidgetDefinition::defaults)
                .orElseGet(HudWidgetSettings::new);
    }

    private static float finite(float value, float fallback, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
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
