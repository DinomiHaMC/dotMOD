package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventorySerializer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.registry.RegistryWrapper;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class PresetImportExportService {
    public static final int MAX_BYTES = 1_048_576;
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "schemaVersion", "id", "name", "description", "tags", "createdAt", "updatedAt", "inventory"
    );

    private final Gson gson;
    private final RegistryWrapper.WrapperLookup registries;
    private final VirtualInventorySerializer inventorySerializer = new VirtualInventorySerializer();

    public PresetImportExportService(Gson gson, RegistryWrapper.WrapperLookup registries) {
        this.gson = gson;
        this.registries = registries;
    }

    public String exportPreset(InventoryPreset preset) {
        return gson.toJson(PresetDocument.fromPreset(preset, inventorySerializer, registries));
    }

    public InventoryPreset importPreset(String json) {
        if (json == null || json.isBlank() || json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new PresetException(PresetError.INVALID_DATA, "Preset JSON is empty or too large");
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!ALLOWED_FIELDS.containsAll(root.keySet())) {
                throw new PresetException(PresetError.INVALID_DATA, "Preset JSON contains unsupported fields");
            }
            PresetDocument document = gson.fromJson(root, PresetDocument.class);
            return document.toPreset(inventorySerializer, registries);
        } catch (PresetException exception) {
            throw exception;
        } catch (UnsupportedDataVersionException exception) {
            throw new PresetException(PresetError.UNSUPPORTED_VERSION, "Unsupported preset format", exception);
        } catch (RuntimeException exception) {
            throw new PresetException(PresetError.INVALID_DATA, "Invalid preset JSON", exception);
        }
    }
}
