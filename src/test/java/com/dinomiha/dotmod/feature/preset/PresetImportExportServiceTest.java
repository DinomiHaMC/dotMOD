package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.feature.invsee.MinecraftTestBootstrap;
import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import com.google.gson.GsonBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetImportExportServiceTest {
    private static PresetImportExportService service;

    @BeforeAll
    static void bootstrap() {
        service = new PresetImportExportService(
                new GsonBuilder().setPrettyPrinting().create(),
                MinecraftTestBootstrap.registries()
        );
    }

    @Test
    void roundTripsMetadataAndComponentStacks() {
        ItemStack named = new ItemStack(Items.DIAMOND, 2);
        named.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Exported"));
        InventoryPreset original = preset(named);

        InventoryPreset restored = service.importPreset(service.exportPreset(original));

        assertEquals(original.id(), restored.id());
        assertEquals(original.name(), restored.name());
        assertEquals("Exported", restored.inventory().getStack(0).getName().getString());
    }

    @Test
    void rejectsUnknownFieldsOversizeAndInvalidJson() {
        assertThrows(PresetException.class, () -> service.importPreset("{\"type\":\"java.lang.Runtime\"}"));
        assertThrows(PresetException.class, () -> service.importPreset("x".repeat(PresetImportExportService.MAX_BYTES + 1)));
        assertThrows(PresetException.class, () -> service.importPreset("not json"));
        PresetException future = assertThrows(PresetException.class, () -> service.importPreset("{\"schemaVersion\":999}"));
        assertEquals(PresetError.UNSUPPORTED_VERSION, future.error());
    }

    private static InventoryPreset preset(ItemStack first) {
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        stacks.set(0, first);
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        return new InventoryPreset(
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                "Export Kit", "Description", List.of("tag"), timestamp, timestamp,
                new VirtualInventorySnapshot(stacks)
        );
    }
}
