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
    void upgradesSchemaOneAndInitializesNameMetadata() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        PlayerColorData data = new PlayerColorData();
        data.schemaVersion = 1;
        data.colors.put(uuid.toString(), "#123456");
        data.lastKnownNames = null;

        data.validate();

        assertEquals(2, data.schemaVersion);
        assertEquals("#123456", data.colors.get(uuid.toString()));
        assertEquals(0, data.lastKnownNames.size());
    }

    @Test
    void validatesNamesWithoutDroppingValidColors() {
        UUID validNameUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID invalidNameUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        PlayerColorData data = new PlayerColorData();
        data.colors.put(invalidNameUuid.toString(), "#abcdef");
        data.lastKnownNames.put(validNameUuid.toString().toUpperCase(), "PlayerOne");
        data.lastKnownNames.put(invalidNameUuid.toString(), "bad\nname");
        data.lastKnownNames.put(UUID.randomUUID().toString(), "x".repeat(65));
        data.lastKnownNames.put(UUID.randomUUID().toString(), "");
        data.lastKnownNames.put("not-a-uuid", "ValidName");

        data.validate();

        assertEquals("#ABCDEF", data.colors.get(invalidNameUuid.toString()));
        assertEquals(1, data.lastKnownNames.size());
        assertEquals("PlayerOne", data.lastKnownNames.get(validNameUuid.toString()));
    }

    @Test
    void rejectsNewerSchema() {
        PlayerColorData data = new PlayerColorData();
        data.schemaVersion = PlayerColorData.CURRENT_SCHEMA_VERSION + 1;

        assertThrows(UnsupportedDataVersionException.class, data::validate);
    }
}
