package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.hud.HudElement;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ConfigMigrator {
    private final Gson gson;

    public ConfigMigrator(Gson gson) {
        this.gson = gson;
    }

    public MigrationBundle readLegacy(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            LegacyConfig legacy = gson.fromJson(reader, LegacyConfig.class);
            if (legacy == null) {
                throw new IllegalStateException("Legacy config is empty");
            }
            return migrate(legacy);
        }
    }

    MigrationBundle migrate(LegacyConfig legacy) {
        DotModConfig config = new DotModConfig();
        config.schemaVersion = 3;
        config.general.enabled = legacy.modEnabled;

        config.quickCraft.enabled = legacy.quickCraftEnabled;
        config.quickCraft.slots2x2 = copy(legacy.quickCraftSlots2x2);
        config.quickCraft.slots3x3 = copy(legacy.quickCraftSlots3x3);
        config.quickCraft.buttonOffsetX = legacy.quickCraftButtonOffsetX;
        config.quickCraft.buttonOffsetY = legacy.quickCraftButtonOffsetY;
        config.quickCraft.buttonText = legacy.quickCraftButtonText;

        config.hud.editorEnabled = legacy.hudEditorEnabled;
        config.hud.editorButtonOffsetX = legacy.hudEditorButtonOffsetX;
        config.hud.editorButtonOffsetY = legacy.hudEditorButtonOffsetY;
        config.hud.editorButtonText = legacy.hudEditorButtonText;
        config.hud.snapToGrid = legacy.hudSnapToGrid;
        config.hud.gridSize = legacy.hudGridSize;
        config.hud.magneticSnapping = legacy.hudMagneticSnapping;
        config.hud.magneticSnapDistance = legacy.hudMagneticSnapDistance;
        config.hud.offsets = copyOffsets(legacy.hudOffsets);
        config.hud.uniformNameTags.enabled = legacy.uniformNameTagsEnabled;
        config.hud.uniformNameTags.active = legacy.uniformNameTagsActive;
        config.hud.uniformNameTags.size = legacy.uniformNameTagSize;
        config.hud.uniformNameTags.backgroundColor = legacy.uniformNameTagBackgroundColor;

        config.playerColors.enabled = legacy.nameColorsEnabled;
        config.playerColors.greenColor = legacy.greenColor;
        config.playerColors.redColor = legacy.redColor;
        config.playerColors.defaultColor = legacy.defaultColor;
        config.playerColors.persist = legacy.persistNameColors;
        config.playerColors.notifyChanges = legacy.notifyNameColorChanges;

        config.toggleWalk.toggleShift.enabled = legacy.toggleShiftEnabled;
        config.validate();

        Map<UUID, String> colors = new HashMap<>();
        if (legacy.playerNameColors != null) {
            legacy.playerNameColors.forEach((uuidText, color) -> {
                try {
                    colors.put(UUID.fromString(uuidText), color);
                } catch (IllegalArgumentException ignored) {
                    // One malformed key must not discard the remaining UUID colors.
                }
            });
        }
        return new MigrationBundle(config, colors);
    }

    private static List<Integer> copy(List<Integer> values) {
        return values == null ? null : new ArrayList<>(values);
    }

    private static Map<HudElement, DotModConfig.HudOffset> copyOffsets(Map<HudElement, DotModConfig.HudOffset> values) {
        EnumMap<HudElement, DotModConfig.HudOffset> result = new EnumMap<>(HudElement.class);
        if (values != null) {
            values.forEach((element, offset) -> {
                if (element != null && offset != null) {
                    DotModConfig.HudOffset copy = new DotModConfig.HudOffset();
                    copy.dx = offset.dx;
                    copy.dy = offset.dy;
                    result.put(element, copy);
                }
            });
        }
        return result;
    }

    public record MigrationBundle(DotModConfig config, Map<UUID, String> playerColors) {
    }

    static final class LegacyConfig {
        boolean modEnabled = true;
        boolean quickCraftEnabled = true;
        List<Integer> quickCraftSlots2x2 = new ArrayList<>(List.of(9, 10, 18, 19));
        List<Integer> quickCraftSlots3x3 = new ArrayList<>(List.of(9, 10, 11, 18, 19, 20, 27, 28, 29));
        int quickCraftButtonOffsetX = 4;
        int quickCraftButtonOffsetY = 4;
        String quickCraftButtonText = "Craft";
        boolean hudEditorEnabled = true;
        int hudEditorButtonOffsetX = 4;
        int hudEditorButtonOffsetY = 28;
        String hudEditorButtonText = "HUD";
        boolean hudSnapToGrid = true;
        int hudGridSize = 2;
        boolean hudMagneticSnapping = true;
        int hudMagneticSnapDistance = 4;
        Map<HudElement, DotModConfig.HudOffset> hudOffsets = new EnumMap<>(HudElement.class);
        boolean nameColorsEnabled = true;
        String greenColor = "#55FF55";
        String redColor = "#FF5555";
        String defaultColor = "#FFFFFF";
        boolean persistNameColors = true;
        boolean notifyNameColorChanges = true;
        Map<String, String> playerNameColors = new HashMap<>();
        boolean uniformNameTagsEnabled = true;
        boolean uniformNameTagsActive;
        float uniformNameTagSize = 1.0F;
        String uniformNameTagBackgroundColor = "#000000";
        boolean toggleShiftEnabled = true;
    }
}
