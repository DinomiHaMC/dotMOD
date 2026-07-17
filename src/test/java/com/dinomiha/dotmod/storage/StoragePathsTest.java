package com.dinomiha.dotmod.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoragePathsTest {
    @TempDir
    Path tempDirectory;

    @Test
    void separatesSubsystemPathsAndCreatesMigrationBackup() throws Exception {
        StoragePaths paths = new StoragePaths(tempDirectory);
        assertEquals(tempDirectory.resolve("dotmod/config.json"), paths.configFile());
        assertEquals(tempDirectory.resolve("dotmod/player-colors.json"), paths.playerColorsFile());
        assertEquals(tempDirectory.resolve("dotmod/presets"), paths.presetsDirectory());
        assertEquals(tempDirectory.resolve("dotmod/deaths"), paths.deathsDirectory());

        Files.writeString(paths.legacyConfigFile(), "legacy");
        new BackupService().copy(paths.legacyConfigFile(), paths.legacyBackupFile());
        assertTrue(Files.exists(paths.legacyBackupFile()));
        assertEquals("legacy", Files.readString(paths.legacyBackupFile()));
    }
}
