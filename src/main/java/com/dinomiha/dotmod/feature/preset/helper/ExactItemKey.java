package com.dinomiha.dotmod.feature.preset.helper;

import net.minecraft.item.ItemStack;

/** Count-independent identity of an item and all of its data components. */
public final class ExactItemKey {
    private final ItemStack exemplar;

    public ExactItemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Exact item keys require a non-empty stack");
        }
        this.exemplar = stack.copyWithCount(1);
    }

    public ItemStack exemplar() {
        return exemplar.copy();
    }

    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(exemplar, stack);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ExactItemKey other
                && ItemStack.areItemsAndComponentsEqual(exemplar, other.exemplar);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashCode(exemplar);
    }
}
