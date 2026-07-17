package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.feature.invsee.MinecraftTestBootstrap;
import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryPresetTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void deeplyCopiesInventoryAndDeduplicatesTags() {
        ItemStack source = new ItemStack(Items.STONE, 4);
        InventoryPreset preset = new InventoryPreset(
                UUID.randomUUID(), "Mining", "Description", List.of("Mine", "mine"),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"), snapshot(source)
        );
        source.setCount(1);
        ItemStack returned = preset.inventory().getStack(0);
        returned.setCount(2);

        assertEquals(4, preset.inventory().getStack(0).getCount());
        assertEquals(List.of("Mine"), preset.tags());
    }

    @Test
    void rejectsReversedTimestampsAndInvalidMetadata() {
        assertThrows(PresetException.class, () -> new InventoryPreset(
                UUID.randomUUID(), "Kit", "", List.of(),
                Instant.parse("2026-01-02T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"), VirtualInventorySnapshot.empty()
        ));
        assertThrows(PresetException.class, () -> new InventoryPreset(
                UUID.randomUUID(), "Kit", "x".repeat(1025), List.of(),
                Instant.EPOCH, Instant.EPOCH, VirtualInventorySnapshot.empty()
        ));
    }

    @Test
    void updatesNeverMoveTimestampBackwards() {
        Instant updated = Instant.parse("2026-02-01T00:00:00Z");
        InventoryPreset preset = new InventoryPreset(
                UUID.randomUUID(), "Kit", "", List.of(), Instant.EPOCH, updated, VirtualInventorySnapshot.empty()
        );

        assertEquals(updated, preset.withName("Renamed", Instant.EPOCH).updatedAt());
        assertEquals(updated, preset.withInventory(VirtualInventorySnapshot.empty(), Instant.EPOCH).updatedAt());
    }

    private static VirtualInventorySnapshot snapshot(ItemStack first) {
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        stacks.set(0, first);
        return new VirtualInventorySnapshot(stacks);
    }
}
