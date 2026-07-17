package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.storage.AtomicJsonStore;
import com.dinomiha.dotmod.storage.PlayerColorData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerColorService {
    private static PlayerColorService instance;

    private final ConfigService configService;
    private final AtomicJsonStore<PlayerColorData> store;
    private final Map<UUID, String> colors = new HashMap<>();
    private final Map<UUID, String> lastKnownNames = new HashMap<>();
    private boolean writeBlocked;

    private PlayerColorService(ConfigService configService) {
        this.configService = configService;
        this.store = configService.playerColorStore();
        reload();
        configService.addSaveListener(this::applyPersistenceSetting);
    }

    public static void initialize() {
        instance = new PlayerColorService(ConfigService.get());
    }

    public static PlayerColorService get() {
        if (instance == null) {
            throw new IllegalStateException("PlayerColorService has not been initialized");
        }
        return instance;
    }

    public Optional<String> color(UUID uuid) {
        return Optional.ofNullable(colors.get(uuid));
    }

    public Optional<String> lastKnownName(UUID uuid) {
        return Optional.ofNullable(lastKnownNames.get(uuid));
    }

    public Map<UUID, String> snapshot() {
        return Map.copyOf(colors);
    }

    public Map<UUID, String> lastKnownNamesSnapshot() {
        return Map.copyOf(lastKnownNames);
    }

    public boolean set(UUID uuid, String color) {
        colors.put(uuid, color);
        return saveIfPersistent();
    }

    public boolean set(UUID uuid, String color, String name) {
        colors.put(uuid, color);
        if (PlayerColorData.isValidName(name)) {
            lastKnownNames.put(uuid, name);
        }
        return saveIfPersistent();
    }

    public boolean clear(UUID uuid) {
        colors.remove(uuid);
        lastKnownNames.remove(uuid);
        return saveIfPersistent();
    }

    public boolean reload() {
        AtomicJsonStore.LoadResult<PlayerColorData> result = store.load();
        writeBlocked = result.writeBlocked();
        colors.clear();
        lastKnownNames.clear();
        if (configService.config().playerColors.persist) {
            result.value().colors.forEach((uuid, color) -> colors.put(UUID.fromString(uuid), color));
            result.value().lastKnownNames.forEach((uuid, name) -> lastKnownNames.put(UUID.fromString(uuid), name));
            return !result.recovered() && !writeBlocked;
        }
        if (writeBlocked) {
            return false;
        }
        boolean cleared = (result.value().colors.isEmpty() && result.value().lastKnownNames.isEmpty())
                || store.save(new PlayerColorData());
        return !result.recovered() && cleared;
    }

    private boolean applyPersistenceSetting() {
        if (writeBlocked) {
            return false;
        }
        if (configService.config().playerColors.persist) {
            return save();
        }
        return store.save(new PlayerColorData());
    }

    private boolean saveIfPersistent() {
        if (configService.config().playerColors.persist) {
            return save();
        }
        return true;
    }

    private boolean save() {
        if (writeBlocked) {
            return false;
        }
        PlayerColorData data = new PlayerColorData();
        colors.forEach((uuid, color) -> data.colors.put(uuid.toString(), color));
        lastKnownNames.forEach((uuid, name) -> data.lastKnownNames.put(uuid.toString(), name));
        return store.save(data);
    }
}
