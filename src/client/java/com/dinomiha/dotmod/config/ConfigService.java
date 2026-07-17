package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.DotModClient;
import com.dinomiha.dotmod.storage.AtomicJsonStore;
import com.dinomiha.dotmod.storage.BackupService;
import com.dinomiha.dotmod.storage.PlayerColorData;
import com.dinomiha.dotmod.storage.StoragePaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class ConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DotModClient.MOD_ID + "/config");
    private static ConfigService instance;

    private final StoragePaths paths;
    private final Gson gson;
    private final AtomicJsonStore<DotModConfig> configStore;
    private final AtomicJsonStore<PlayerColorData> playerColorStore;
    private final List<BooleanSupplier> saveListeners = new ArrayList<>();
    private DotModConfig config;
    private boolean migrationBlocked;
    private boolean configWriteBlocked;

    ConfigService(StoragePaths paths) {
        this.paths = paths;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configStore = new AtomicJsonStore<>(
                gson,
                paths.configFile(),
                DotModConfig.class,
                DotModConfig::new,
                DotModConfig::validate
        );
        this.playerColorStore = new AtomicJsonStore<>(
                gson,
                paths.playerColorsFile(),
                PlayerColorData.class,
                PlayerColorData::new,
                PlayerColorData::validate
        );
        migrationBlocked = migrateLegacyConfig() == MigrationOutcome.FAILED;
        if (migrationBlocked) {
            this.config = new DotModConfig();
            this.config.validate();
        } else {
            AtomicJsonStore.LoadResult<DotModConfig> result = configStore.load();
            this.config = result.value();
            this.configWriteBlocked = result.writeBlocked();
        }
        createDataDirectories();
    }

    public static void initialize() {
        StoragePaths paths = new StoragePaths(FabricLoader.getInstance().getConfigDir());
        instance = new ConfigService(paths);
    }

    public static ConfigService get() {
        if (instance == null) {
            throw new IllegalStateException("ConfigService has not been initialized");
        }
        return instance;
    }

    public DotModConfig config() {
        return config;
    }

    public StoragePaths paths() {
        return paths;
    }

    public AtomicJsonStore<PlayerColorData> playerColorStore() {
        return playerColorStore;
    }

    public boolean save() {
        if (migrationBlocked || configWriteBlocked) {
            LOGGER.error("Refusing to overwrite dotMOD config until its storage issue is resolved");
            return false;
        }
        if (!configStore.save(config)) {
            return false;
        }
        boolean listenersSaved = true;
        for (BooleanSupplier listener : saveListeners) {
            try {
                listenersSaved &= listener.getAsBoolean();
            } catch (RuntimeException exception) {
                listenersSaved = false;
                LOGGER.error("A config save listener failed", exception);
            }
        }
        return listenersSaved;
    }

    public boolean reload() {
        if (migrationBlocked) {
            MigrationOutcome outcome = migrateLegacyConfig();
            if (outcome == MigrationOutcome.FAILED) {
                return false;
            }
            migrationBlocked = false;
        }
        AtomicJsonStore.LoadResult<DotModConfig> result = configStore.load();
        config.replaceWith(result.value());
        configWriteBlocked = result.writeBlocked();
        return !result.recovered() && !result.writeBlocked();
    }

    public boolean resetHud() {
        config.hud.resetOffsets();
        return save();
    }

    public void addSaveListener(BooleanSupplier listener) {
        saveListeners.add(listener);
    }

    private MigrationOutcome migrateLegacyConfig() {
        if (Files.exists(paths.configFile()) || !Files.exists(paths.legacyConfigFile())) {
            return MigrationOutcome.NOT_NEEDED;
        }
        try {
            new BackupService().copy(paths.legacyConfigFile(), paths.legacyBackupFile());
            ConfigMigrator.MigrationBundle migration = new ConfigMigrator(gson).readLegacy(paths.legacyConfigFile());

            PlayerColorData colors = Files.exists(paths.playerColorsFile())
                    ? playerColorStore.load().value()
                    : new PlayerColorData();
            if (migration.config().playerColors.persist) {
                migration.playerColors().forEach((uuid, color) -> colors.colors.putIfAbsent(uuid.toString(), color));
            }
            colors.validate();

            if (!playerColorStore.save(colors) || !configStore.save(migration.config())) {
                throw new IOException("Could not write migrated dotMOD data");
            }
            LOGGER.info("Migrated dotMOD config to {}", paths.configFile());
            return MigrationOutcome.SUCCESS;
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(paths.configFile());
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            LOGGER.error("Failed to migrate legacy dotMOD config; keeping it for a retry", exception);
            return MigrationOutcome.FAILED;
        }
    }

    private void createDataDirectories() {
        try {
            Files.createDirectories(paths.presetsDirectory());
            Files.createDirectories(paths.deathsDirectory());
        } catch (IOException exception) {
            LOGGER.error("Failed to create dotMOD data directories", exception);
        }
    }

    private enum MigrationOutcome {
        NOT_NEEDED,
        SUCCESS,
        FAILED
    }
}
