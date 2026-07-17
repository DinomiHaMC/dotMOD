package com.dinomiha.dotmod.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {
    @TempDir
    Path tempDirectory;

    @Test
    void migratesLegacyFieldsAndPlayerColors() throws IOException {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Path legacy = tempDirectory.resolve("dotmod.json");
        Files.writeString(legacy, """
                {
                  "modEnabled": false,
                  "quickCraftButtonText": "Go",
                  "hudGridSize": 7,
                  "persistNameColors": true,
                  "playerNameColors": {
                    "%s": "#123456",
                    "not-a-uuid": "#FFFFFF"
                  },
                  "toggleShiftActive": true
                }
                """.formatted(uuid));

        ConfigMigrator.MigrationBundle result = new ConfigMigrator(new Gson()).readLegacy(legacy);

        assertFalse(result.config().general.enabled);
        assertEquals("Go", result.config().quickCraft.buttonText);
        assertEquals(7, result.config().hud.gridSize);
        assertTrue(result.config().hud.magneticSnapping);
        assertEquals(4, result.config().hud.magneticSnapDistance);
        assertTrue(result.config().toggleWalk.toggleShift.active);
        assertEquals("#123456", result.playerColors().get(uuid));
        assertEquals(1, result.playerColors().size());
    }
}
