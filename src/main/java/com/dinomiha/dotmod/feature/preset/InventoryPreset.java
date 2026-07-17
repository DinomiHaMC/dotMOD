package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Immutable metadata and inventory payload for one local preset. */
public final class InventoryPreset {
    private final UUID id;
    private final String name;
    private final String description;
    private final List<String> tags;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final VirtualInventorySnapshot inventory;

    public InventoryPreset(
            UUID id,
            String name,
            String description,
            List<String> tags,
            Instant createdAt,
            Instant updatedAt,
            VirtualInventorySnapshot inventory
    ) {
        if (id == null || createdAt == null || updatedAt == null || inventory == null || updatedAt.isBefore(createdAt)) {
            throw new PresetException(PresetError.INVALID_DATA, "Invalid preset metadata");
        }
        this.id = id;
        this.name = PresetNameValidator.normalize(name);
        this.description = validateDescription(description);
        this.tags = validateTags(tags);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.inventory = new VirtualInventorySnapshot(inventory.copyStacks());
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public List<String> tags() {
        return tags;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public VirtualInventorySnapshot inventory() {
        return new VirtualInventorySnapshot(inventory.copyStacks());
    }

    public InventoryPreset withName(String newName, Instant updatedAt) {
        return new InventoryPreset(id, newName, description, tags, createdAt, latest(updatedAt), inventory);
    }

    public InventoryPreset withInventory(VirtualInventorySnapshot snapshot, Instant updatedAt) {
        return new InventoryPreset(id, name, description, tags, createdAt, latest(updatedAt), snapshot);
    }

    private Instant latest(Instant candidate) {
        return candidate.isAfter(updatedAt) ? candidate : updatedAt;
    }

    private static String validateDescription(String value) {
        String description = value == null ? "" : value.strip();
        if (description.length() > 1024 || description.codePoints().anyMatch(code -> Character.isISOControl(code) && code != '\n' && code != '\t')) {
            throw new PresetException(PresetError.INVALID_DATA, "Invalid preset description");
        }
        return description;
    }

    private static List<String> validateTags(List<String> input) {
        if (input == null) {
            return List.of();
        }
        LinkedHashMap<String, String> tags = new LinkedHashMap<>();
        for (String raw : input) {
            String tag = raw == null ? "" : raw.strip();
            if (tag.isEmpty() || tag.length() > 32 || tag.codePoints().anyMatch(Character::isISOControl)) {
                throw new PresetException(PresetError.INVALID_DATA, "Invalid preset tag");
            }
            tags.putIfAbsent(tag.toLowerCase(Locale.ROOT), tag);
        }
        if (tags.size() > 16) {
            throw new PresetException(PresetError.INVALID_DATA, "Too many preset tags");
        }
        return List.copyOf(new ArrayList<>(tags.values()));
    }
}
