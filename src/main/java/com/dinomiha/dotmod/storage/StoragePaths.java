package com.dinomiha.dotmod.storage;

import java.nio.file.Path;

public record StoragePaths(Path configDirectory) {
    public Path root() {
        return configDirectory.resolve("dotmod");
    }

    public Path configFile() {
        return root().resolve("config.json");
    }

    public Path playerColorsFile() {
        return root().resolve("player-colors.json");
    }

    public Path presetsDirectory() {
        return root().resolve("presets");
    }

    public Path aliasesFile() {
        return root().resolve("command-aliases.json");
    }

    public Path commandHistoryFile() {
        return root().resolve("command-history.json");
    }

    public Path deathsDirectory() {
        return root().resolve("deaths");
    }

    public Path legacyConfigFile() {
        return configDirectory.resolve("dotmod.json");
    }

    public Path legacyBackupFile() {
        return configDirectory.resolve("dotmod.json.migrated.bak");
    }
}
