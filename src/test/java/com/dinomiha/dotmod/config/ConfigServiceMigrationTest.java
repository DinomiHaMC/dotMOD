package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.storage.StoragePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigServiceMigrationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void failedMigrationDoesNotCreateCommitMarkerAndCanBeRetried() throws Exception {
        StoragePaths paths = new StoragePaths(tempDirectory);
        Files.writeString(paths.legacyConfigFile(), "{ broken");

        ConfigService service = new ConfigService(paths);

        assertFalse(Files.exists(paths.configFile()));
        assertFalse(service.save());
        assertFalse(Files.exists(paths.configFile()));

        Files.writeString(paths.legacyConfigFile(), "{\"modEnabled\":false}");
        assertTrue(service.reload());
        assertTrue(Files.exists(paths.configFile()));
        assertFalse(service.config().general.enabled);
    }

    @Test
    void newerSchemaRemainsUntouchedAndBlocksSaving() throws Exception {
        StoragePaths paths = new StoragePaths(tempDirectory);
        Files.createDirectories(paths.root());
        String futureConfig = "{\"schemaVersion\":999,\"futureField\":true}";
        Files.writeString(paths.configFile(), futureConfig);

        ConfigService service = new ConfigService(paths);

        assertFalse(service.save());
        assertEquals(futureConfig, Files.readString(paths.configFile()));
        assertFalse(Files.exists(paths.configFile().resolveSibling("config.json.broken")));
    }

    @Test
    void olderSchemaIsNormalizedAndPersistedDuringLoad() throws Exception {
        StoragePaths paths = new StoragePaths(tempDirectory);
        Files.createDirectories(paths.root());
        Files.writeString(paths.configFile(), "{\"schemaVersion\":6,\"general\":{\"enabled\":false}}");

        ConfigService service = new ConfigService(paths);

        assertFalse(service.config().general.enabled);
        assertTrue(service.config().toggleWalk.toggleSprint.enabled);
        String persisted = Files.readString(paths.configFile());
        assertTrue(persisted.contains("\"schemaVersion\": " + DotModConfig.CURRENT_SCHEMA_VERSION));
        assertTrue(Files.exists(paths.configFile().resolveSibling("config.json.bak")));
    }
}
