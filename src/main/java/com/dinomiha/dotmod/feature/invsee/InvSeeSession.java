package com.dinomiha.dotmod.feature.invsee;

import net.minecraft.item.ItemStack;

/** Owns all ISM mutations and enforces the selected access mode. */
public final class InvSeeSession {
    private final InvSeeMode mode;
    private final VirtualInventory workingInventory;
    private VirtualInventorySnapshot baseline;
    private ItemStack cursorStack = ItemStack.EMPTY;

    public InvSeeSession(InvSeeMode mode, VirtualInventorySnapshot openingSnapshot) {
        this.mode = mode;
        this.baseline = new VirtualInventorySnapshot(openingSnapshot.copyStacks());
        this.workingInventory = new VirtualInventory(openingSnapshot);
    }

    public InvSeeMode mode() {
        return mode;
    }

    public ItemStack getStack(int index) {
        return workingInventory.getStack(index);
    }

    public ItemStack getCursorStack() {
        return cursorStack.copy();
    }

    public boolean hasCursorStack() {
        return !cursorStack.isEmpty();
    }

    public boolean isDirty() {
        return !workingInventory.snapshot().contentEquals(baseline);
    }

    public MutationResult leftClick(int index) {
        if (!mode.allows(InvSeeCapability.MUTATE)) {
            return MutationResult.DENIED;
        }
        ItemStack slot = workingInventory.getStack(index);
        if (cursorStack.isEmpty()) {
            if (slot.isEmpty()) {
                return MutationResult.NO_CHANGE;
            }
            cursorStack = slot;
            workingInventory.clear(index);
            return MutationResult.APPLIED;
        }
        if (slot.isEmpty()) {
            workingInventory.setStack(index, cursorStack);
            cursorStack = ItemStack.EMPTY;
            return MutationResult.APPLIED;
        }
        if (ItemStack.areItemsAndComponentsEqual(slot, cursorStack)) {
            int moved = Math.min(cursorStack.getCount(), slot.getMaxCount() - slot.getCount());
            if (moved <= 0) {
                return MutationResult.NO_CHANGE;
            }
            slot.increment(moved);
            cursorStack.decrement(moved);
            workingInventory.setStack(index, slot);
            if (cursorStack.isEmpty()) {
                cursorStack = ItemStack.EMPTY;
            }
            return MutationResult.APPLIED;
        }
        workingInventory.setStack(index, cursorStack);
        cursorStack = slot;
        return MutationResult.APPLIED;
    }

    public MutationResult rightClick(int index) {
        if (!mode.allows(InvSeeCapability.MUTATE)) {
            return MutationResult.DENIED;
        }
        ItemStack slot = workingInventory.getStack(index);
        if (cursorStack.isEmpty()) {
            if (slot.isEmpty()) {
                return MutationResult.NO_CHANGE;
            }
            int amount = (slot.getCount() + 1) / 2;
            cursorStack = slot.copyWithCount(amount);
            slot.decrement(amount);
            workingInventory.setStack(index, slot);
            return MutationResult.APPLIED;
        }
        if (slot.isEmpty()) {
            workingInventory.setStack(index, cursorStack.copyWithCount(1));
            cursorStack.decrement(1);
            if (cursorStack.isEmpty()) {
                cursorStack = ItemStack.EMPTY;
            }
            return MutationResult.APPLIED;
        }
        if (ItemStack.areItemsAndComponentsEqual(slot, cursorStack) && slot.getCount() < slot.getMaxCount()) {
            slot.increment(1);
            cursorStack.decrement(1);
            workingInventory.setStack(index, slot);
            if (cursorStack.isEmpty()) {
                cursorStack = ItemStack.EMPTY;
            }
            return MutationResult.APPLIED;
        }
        workingInventory.setStack(index, cursorStack);
        cursorStack = slot;
        return MutationResult.APPLIED;
    }

    public MutationResult clear(int index) {
        if (!mode.allows(InvSeeCapability.MUTATE)) {
            return MutationResult.DENIED;
        }
        if (workingInventory.getStack(index).isEmpty()) {
            return MutationResult.NO_CHANGE;
        }
        workingInventory.clear(index);
        return MutationResult.APPLIED;
    }

    public MutationResult setAmount(int index, int amount) {
        if (!mode.allows(InvSeeCapability.SET_AMOUNT)) {
            return MutationResult.DENIED;
        }
        ItemStack stack = workingInventory.getStack(index);
        if (stack.isEmpty()) {
            return MutationResult.INVALID;
        }
        if (amount == 0) {
            workingInventory.clear(index);
            return MutationResult.APPLIED;
        }
        if (amount < 1 || amount > stack.getMaxCount()) {
            return MutationResult.INVALID;
        }
        if (amount == stack.getCount()) {
            return MutationResult.NO_CHANGE;
        }
        stack.setCount(amount);
        workingInventory.setStack(index, stack);
        return MutationResult.APPLIED;
    }

    public MutationResult takeCatalogStack(ItemStack catalogStack, boolean maximumAmount) {
        if (!mode.allows(InvSeeCapability.CATALOG) || catalogStack == null || catalogStack.isEmpty()) {
            return MutationResult.DENIED;
        }
        if (!cursorStack.isEmpty()) {
            return MutationResult.INVALID;
        }
        try {
            int amount = maximumAmount ? catalogStack.getMaxCount() : Math.max(1, catalogStack.getCount());
            cursorStack = VirtualInventory.validatedCopy(catalogStack.copyWithCount(amount));
            return MutationResult.APPLIED;
        } catch (IllegalArgumentException exception) {
            cursorStack = ItemStack.EMPTY;
            return MutationResult.INVALID;
        }
    }

    public MutationResult rollback() {
        if (!mode.allows(InvSeeCapability.ROLLBACK)) {
            return MutationResult.DENIED;
        }
        workingInventory.restore(baseline);
        cursorStack = ItemStack.EMPTY;
        return MutationResult.APPLIED;
    }

    public boolean save(InvSeeSaveTarget target) {
        if (!mode.allows(InvSeeCapability.SAVE) || target == null || hasCursorStack()) {
            return false;
        }
        VirtualInventorySnapshot snapshot = workingInventory.snapshot();
        try {
            if (!target.save(snapshot)) {
                return false;
            }
        } catch (RuntimeException exception) {
            return false;
        }
        baseline = snapshot;
        return true;
    }

    public VirtualInventorySnapshot snapshot() {
        return workingInventory.snapshot();
    }
}
