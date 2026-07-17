package com.dinomiha.dotmod.feature.invsee.persistence;

import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VirtualInventoryDocument {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public List<SlotEntry> slots = new ArrayList<>();

    public void validateStructure() {
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedDataVersionException("Unsupported ISM inventory schema " + schemaVersion);
        }
        schemaVersion = CURRENT_SCHEMA_VERSION;
        if (slots == null) {
            throw new VirtualInventoryFormatException("Missing ISM slots array");
        }
        Set<Integer> indices = new HashSet<>();
        for (SlotEntry entry : slots) {
            if (entry == null || entry.index < 0 || entry.index >= VirtualInventory.SLOT_COUNT) {
                throw new VirtualInventoryFormatException("Invalid ISM slot entry");
            }
            if (!indices.add(entry.index)) {
                throw new VirtualInventoryFormatException("Duplicate ISM slot " + entry.index);
            }
            if (entry.stack == null || entry.stack.isJsonNull()) {
                throw new VirtualInventoryFormatException("Missing stack in ISM slot " + entry.index);
            }
        }
    }

    public static final class SlotEntry {
        public int index;
        public JsonElement stack;

        public SlotEntry() {
        }

        public SlotEntry(int index, JsonElement stack) {
            this.index = index;
            this.stack = stack;
        }
    }
}
