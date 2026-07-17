package com.dinomiha.dotmod.feature.invsee;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Local inventory model that never exposes mutable references to its ItemStacks. */
public final class VirtualInventory {
    public static final int SLOT_COUNT = 41;

    private final List<ItemStack> stacks = new ArrayList<>(SLOT_COUNT);

    public VirtualInventory() {
        stacks.addAll(Collections.nCopies(SLOT_COUNT, ItemStack.EMPTY));
    }

    public VirtualInventory(VirtualInventorySnapshot snapshot) {
        stacks.addAll(snapshot.copyStacks());
    }

    public ItemStack getStack(int index) {
        checkIndex(index);
        return stacks.get(index).copy();
    }

    public VirtualInventorySnapshot snapshot() {
        return new VirtualInventorySnapshot(stacks);
    }

    void setStack(int index, ItemStack stack) {
        checkIndex(index);
        stacks.set(index, validatedCopy(stack));
    }

    void clear(int index) {
        checkIndex(index);
        stacks.set(index, ItemStack.EMPTY);
    }

    void restore(VirtualInventorySnapshot snapshot) {
        stacks.clear();
        stacks.addAll(snapshot.copyStacks());
    }

    static ItemStack validatedCopy(ItemStack stack) {
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        if (stack.getCount() < 0) {
            throw new IllegalArgumentException("Negative ItemStack count");
        }
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (stack.getCount() < 1 || stack.getCount() > stack.getMaxCount()) {
            throw new IllegalArgumentException("Invalid stack count " + stack.getCount() + " for " + stack.getItem());
        }
        if (ItemStack.validate(stack).isError()) {
            throw new IllegalArgumentException("Invalid ItemStack components for " + stack.getItem());
        }
        return stack.copy();
    }

    static void checkIndex(int index) {
        if (index < 0 || index >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Virtual inventory index: " + index);
        }
    }
}
