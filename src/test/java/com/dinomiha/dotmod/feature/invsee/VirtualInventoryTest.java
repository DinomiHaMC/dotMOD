package com.dinomiha.dotmod.feature.invsee;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualInventoryTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void mapsAllSupportedPlayerSlots() {
        assertEquals(VirtualSlotKind.HOTBAR, VirtualSlot.at(0).kind());
        assertEquals(VirtualSlotKind.MAIN, VirtualSlot.at(9).kind());
        assertEquals(26, VirtualSlot.at(35).kindIndex());
        assertEquals(VirtualSlotKind.ARMOR, VirtualSlot.at(36).kind());
        assertEquals(3, VirtualSlot.at(39).kindIndex());
        assertEquals(VirtualSlotKind.OFFHAND, VirtualSlot.at(40).kind());
        assertThrows(IndexOutOfBoundsException.class, () -> VirtualSlot.at(41));
    }

    @Test
    void snapshotsDeepCopyEveryStack() {
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        ItemStack source = new ItemStack(Items.STONE, 5);
        stacks.set(0, source);
        VirtualInventorySnapshot snapshot = new VirtualInventorySnapshot(stacks);

        source.setCount(1);
        ItemStack returned = snapshot.getStack(0);
        returned.setCount(2);

        assertEquals(5, snapshot.getStack(0).getCount());
        assertEquals(VirtualInventory.SLOT_COUNT, snapshot.copyStacks().size());
    }

    @Test
    void rejectsInvalidSlotCountAndOverstackedItems() {
        assertThrows(IllegalArgumentException.class, () -> new VirtualInventorySnapshot(List.of()));
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        stacks.set(0, new ItemStack(Items.DIAMOND_SWORD, 2));
        assertThrows(IllegalArgumentException.class, () -> new VirtualInventorySnapshot(stacks));
        assertTrue(VirtualInventorySnapshot.empty().getStack(0).isEmpty());
    }
}
