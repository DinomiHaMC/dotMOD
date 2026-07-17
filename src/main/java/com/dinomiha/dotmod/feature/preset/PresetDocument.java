package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventoryDocument;
import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventorySerializer;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;
import net.minecraft.registry.RegistryWrapper;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PresetDocument {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public String id;
    public String name;
    public String description = "";
    public List<String> tags = new ArrayList<>();
    public String createdAt;
    public String updatedAt;
    public VirtualInventoryDocument inventory;

    public static PresetDocument fromPreset(
            InventoryPreset preset,
            VirtualInventorySerializer serializer,
            RegistryWrapper.WrapperLookup registries
    ) {
        PresetDocument document = new PresetDocument();
        document.id = preset.id().toString();
        document.name = preset.name();
        document.description = preset.description();
        document.tags = new ArrayList<>(preset.tags());
        document.createdAt = preset.createdAt().toString();
        document.updatedAt = preset.updatedAt().toString();
        document.inventory = serializer.encode(preset.inventory(), registries);
        return document;
    }

    public InventoryPreset toPreset(
            VirtualInventorySerializer serializer,
            RegistryWrapper.WrapperLookup registries
    ) {
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedDataVersionException("Unsupported preset schema " + schemaVersion);
        }
        validateHeader();
        if (inventory == null) {
            throw new PresetException(PresetError.INVALID_DATA, "Preset inventory is missing");
        }
        return new InventoryPreset(
                UUID.fromString(id),
                name,
                description,
                tags,
                Instant.parse(createdAt),
                Instant.parse(updatedAt),
                serializer.decode(inventory, registries)
        );
    }

    public void validateHeader() {
        try {
            UUID.fromString(id);
            PresetNameValidator.normalize(name);
            Instant created = Instant.parse(createdAt);
            Instant updated = Instant.parse(updatedAt);
            if (updated.isBefore(created)) {
                throw new PresetException(PresetError.INVALID_DATA, "Preset timestamps are reversed");
            }
            new InventoryPreset(UUID.fromString(id), name, description, tags, created, updated, com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot.empty());
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new PresetException(PresetError.INVALID_DATA, "Invalid preset header", exception);
        }
    }
}
