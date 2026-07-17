package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PresetIndexDocument {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public String activePresetId;
    public List<String> order = new ArrayList<>();

    public void validate() {
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedDataVersionException("Unsupported preset index schema " + schemaVersion);
        }
        schemaVersion = CURRENT_SCHEMA_VERSION;
        if (activePresetId != null) {
            UUID.fromString(activePresetId);
        }
        if (order == null) {
            throw new PresetException(PresetError.INVALID_DATA, "Preset order is missing");
        }
        Set<UUID> unique = new HashSet<>();
        for (String value : order) {
            if (!unique.add(UUID.fromString(value))) {
                throw new PresetException(PresetError.INVALID_DATA, "Duplicate preset index entry");
            }
        }
    }
}
