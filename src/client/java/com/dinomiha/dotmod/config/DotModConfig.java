package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.DotModClient;
import com.dinomiha.dotmod.hud.HudElement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DotModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DotModClient.MOD_ID + "/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(DotModClient.MOD_ID + ".json");
    private static final Path TEMP_PATH = PATH.resolveSibling(PATH.getFileName() + ".tmp");
    private static final Path BROKEN_PATH = PATH.resolveSibling(PATH.getFileName() + ".broken");
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
    public boolean hudMagneticSnapping = true;
    public int hudMagneticSnapDistance = 4;
    public Map<HudElement, HudOffset> hudOffsets = new EnumMap<>(HudElement.class);

    public boolean nameColorsEnabled = true;
    public String greenColor = "#55FF55";
    public String redColor = "#FF5555";
    public String defaultColor = "#FFFFFF";
    public boolean persistNameColors = true;
    public boolean notifyNameColorChanges = true;
    public Map<UUID, String> playerNameColors = new HashMap<>();

    public boolean uniformNameTagsEnabled = true;
    public boolean uniformNameTagsActive = false;
    public float uniformNameTagSize = 1.0F;
    public String uniformNameTagBackgroundColor = "#000000";

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
                throw new IllegalStateException("dotMOD config is empty");
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Failed to load dotMOD config; restoring defaults", exception);
                preserveBrokenConfig();
            }
        }
        INSTANCE = defaults();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(TEMP_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
            try {
                Files.move(TEMP_PATH, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(TEMP_PATH, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to save dotMOD config", exception);
            try {
                Files.deleteIfExists(TEMP_PATH);
            } catch (IOException cleanupException) {
                LOGGER.warn("Failed to remove temporary dotMOD config", cleanupException);
            }
        }
    }

    private static void preserveBrokenConfig() {
        try {
            Files.move(PATH, BROKEN_PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.warn("Failed to preserve broken dotMOD config", exception);
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
        quickCraftSlots2x2 = sanitizeSlots(quickCraftSlots2x2, List.of(9, 10, 18, 19));
        quickCraftSlots3x3 = sanitizeSlots(quickCraftSlots3x3, List.of(9, 10, 11, 18, 19, 20, 27, 28, 29));
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
        if (uniformNameTagBackgroundColor == null || uniformNameTagBackgroundColor.isBlank()) {
            uniformNameTagBackgroundColor = "#000000";
        }
        if (!Float.isFinite(uniformNameTagSize)) {
            uniformNameTagSize = 1.0F;
        }
        uniformNameTagSize = Math.max(0.1F, Math.min(5.0F, uniformNameTagSize));
        if (playerNameColors == null) {
            playerNameColors = new HashMap<>();
        }
        Map<HudElement, HudOffset> loadedOffsets = hudOffsets;
        hudOffsets = new EnumMap<>(HudElement.class);
        if (loadedOffsets != null) {
            loadedOffsets.forEach((element, offset) -> {
                if (element != null && offset != null) {
                    hudOffsets.put(element, offset);
                }
            });
        }
        for (HudElement element : HudElement.values()) {
            hudOffsets.computeIfAbsent(element, ignored -> new HudOffset());
        }
        hudGridSize = Math.max(1, hudGridSize);
        hudMagneticSnapDistance = Math.max(1, Math.min(16, hudMagneticSnapDistance));
    }

    private static List<Integer> sanitizeSlots(List<Integer> slots, List<Integer> fallback) {
        if (slots == null) {
            return new ArrayList<>(fallback);
        }
        LinkedHashSet<Integer> valid = new LinkedHashSet<>();
        for (Integer slot : slots) {
            if (slot != null && slot >= 0 && slot <= 35) {
                valid.add(slot);
            }
        }
        return valid.isEmpty() ? new ArrayList<>(fallback) : new ArrayList<>(valid);
    }

    public static final class HudOffset {
        public int dx = 0;
        public int dy = 0;
    }
}
