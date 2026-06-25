package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.DotModClient;
import com.dinomiha.dotmod.hud.HudElement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DotModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(DotModClient.MOD_ID + ".json");
    private static DotModConfig INSTANCE = defaults();

    public boolean modEnabled = true;

    public boolean quickCraftEnabled = true;
    public List<Integer> quickCraftSlots2x2 = new ArrayList<>(List.of(9, 10, 18, 19));
    public List<Integer> quickCraftSlots3x3 = new ArrayList<>(List.of(9, 10, 11, 18, 19, 20, 27, 28, 29));
    public int quickCraftButtonOffsetX = 4;
    public int quickCraftButtonOffsetY = 4;
    public String quickCraftButtonText = "Craft";

    public boolean hudEditorEnabled = true;
    public int hudEditorButtonOffsetX = 4;
    public int hudEditorButtonOffsetY = 28;
    public String hudEditorButtonText = "HUD";
    public boolean hudSnapToGrid = true;
    public int hudGridSize = 2;
    public Map<HudElement, HudOffset> hudOffsets = new EnumMap<>(HudElement.class);

    public boolean nameColorsEnabled = true;
    public String greenColor = "#55FF55";
    public String redColor = "#FF5555";
    public String defaultColor = "#FFFFFF";
    public boolean persistNameColors = true;
    public boolean notifyNameColorChanges = true;
    public Map<UUID, String> playerNameColors = new HashMap<>();

    public boolean toggleShiftEnabled = true;
    public boolean toggleShiftActive = false;

    public static DotModConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                DotModConfig loaded = GSON.fromJson(reader, DotModConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    INSTANCE.fillMissingDefaults();
                    if (!INSTANCE.persistNameColors) {
                        INSTANCE.playerNameColors.clear();
                    }
                    return;
                }
            } catch (IOException ignored) {
            }
        }
        INSTANCE = defaults();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static void resetHud() {
        INSTANCE.hudOffsets.clear();
        for (HudElement element : HudElement.values()) {
            INSTANCE.hudOffsets.put(element, new HudOffset());
        }
        save();
    }

    public HudOffset hudOffset(HudElement element) {
        return hudOffsets.computeIfAbsent(element, ignored -> new HudOffset());
    }

    public void setNameColor(UUID uuid, String color) {
        playerNameColors.put(uuid, color);
        save();
    }

    public void clearNameColor(UUID uuid) {
        playerNameColors.remove(uuid);
        save();
    }

    private static DotModConfig defaults() {
        DotModConfig config = new DotModConfig();
        config.fillMissingDefaults();
        return config;
    }

    private void fillMissingDefaults() {
        if (quickCraftSlots2x2 == null || quickCraftSlots2x2.isEmpty()) {
            quickCraftSlots2x2 = new ArrayList<>(List.of(9, 10, 18, 19));
        }
        if (quickCraftSlots3x3 == null || quickCraftSlots3x3.isEmpty()) {
            quickCraftSlots3x3 = new ArrayList<>(List.of(9, 10, 11, 18, 19, 20, 27, 28, 29));
        }
        if (quickCraftButtonText == null || quickCraftButtonText.isBlank()) {
            quickCraftButtonText = "Craft";
        }
        if (hudEditorButtonText == null || hudEditorButtonText.isBlank()) {
            hudEditorButtonText = "HUD";
        }
        if (greenColor == null || greenColor.isBlank()) {
            greenColor = "#55FF55";
        }
        if (redColor == null || redColor.isBlank()) {
            redColor = "#FF5555";
        }
        if (defaultColor == null || defaultColor.isBlank()) {
            defaultColor = "#FFFFFF";
        }
        if (playerNameColors == null) {
            playerNameColors = new HashMap<>();
        }
        if (hudOffsets == null) {
            hudOffsets = new EnumMap<>(HudElement.class);
        }
        if (!(hudOffsets instanceof EnumMap)) {
            hudOffsets = new EnumMap<>(hudOffsets);
        }
        for (HudElement element : HudElement.values()) {
            hudOffsets.computeIfAbsent(element, ignored -> new HudOffset());
        }
        hudGridSize = Math.max(1, hudGridSize);
    }

    public static final class HudOffset {
        public int dx = 0;
        public int dy = 0;
    }
}
