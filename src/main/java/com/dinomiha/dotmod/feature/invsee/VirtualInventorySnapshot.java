package com.dinomiha.dotmod.feature.invsee;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, deeply copied state of the 41 player inventory slots supported by ISM. */
public final class VirtualInventorySnapshot {
    private final List<ItemStack> stacks;

    public VirtualInventorySnapshot(List<ItemStack> stacks) {
        if (stacks == null || stacks.size() != VirtualInventory.SLOT_COUNT) {
            throw new IllegalArgumentException("ISM snapshots require exactly " + VirtualInventory.SLOT_COUNT + " slots");
        }
        List<ItemStack> copies = new ArrayList<>(VirtualInventory.SLOT_COUNT);
        for (ItemStack stack : stacks) {
            copies.add(VirtualInventory.validatedCopy(stack));
        }
        this.stacks = Collections.unmodifiableList(copies);
    }

    public static VirtualInventorySnapshot empty() {
        return new VirtualInventorySnapshot(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
    }

    public ItemStack getStack(int index) {
        VirtualInventory.checkIndex(index);
        return stacks.get(index).copy();
    }

    public List<ItemStack> copyStacks() {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    public boolean contentEquals(VirtualInventorySnapshot other) {
        if (other == null) {
            return false;
        }
        for (int index = 0; index < VirtualInventory.SLOT_COUNT; index++) {
            if (!ItemStack.areEqual(stacks.get(index), other.stacks.get(index))) {
                return false;
            }
        }
        return true;
    }
}
