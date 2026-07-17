package com.dinomiha.dotmod.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerColorData {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public Map<String, String> colors = new HashMap<>();

    public void validate() {
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedDataVersionException("Unsupported player-color schema " + schemaVersion);
        }
        schemaVersion = CURRENT_SCHEMA_VERSION;
        Map<String, String> valid = new HashMap<>();
        if (colors != null) {
            colors.forEach((uuidText, color) -> {
                if (uuidText == null) {
                    return;
                }
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    if (color != null && color.matches("(?i)#[0-9a-f]{6}")) {
                        valid.put(uuid.toString(), color.toUpperCase());
                    }
                } catch (IllegalArgumentException ignored) {
                    // Keep valid entries even if one serialized UUID is malformed.
                }
            });
        }
        colors = valid;
    }
}
