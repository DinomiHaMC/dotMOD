package com.dinomiha.dotmod.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerColorDataTest {
    @Test
    void dropsOnlyMalformedEntries() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        PlayerColorData data = new PlayerColorData();
        data.colors.put(uuid.toString().toUpperCase(), "#12abEF");
        data.colors.put("not-a-uuid", "#FFFFFF");
        data.colors.put(UUID.randomUUID().toString(), "not-a-color");

        data.validate();

        assertEquals(1, data.colors.size());
        assertEquals("#12ABEF", data.colors.get(uuid.toString()));
    }

    @Test
    void rejectsNewerSchema() {
        PlayerColorData data = new PlayerColorData();
        data.schemaVersion = PlayerColorData.CURRENT_SCHEMA_VERSION + 1;

        assertThrows(UnsupportedDataVersionException.class, data::validate);
    }
}
