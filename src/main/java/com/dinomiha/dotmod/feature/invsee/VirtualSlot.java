package com.dinomiha.dotmod.feature.invsee;

public record VirtualSlot(int index, VirtualSlotKind kind, int kindIndex) {
    public VirtualSlot {
        if (index < 0 || index >= VirtualInventory.SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Virtual slot index: " + index);
        }
        if (kindIndex < 0) {
            throw new IllegalArgumentException("Negative kind index");
        }
    }

    public static VirtualSlot at(int index) {
        VirtualInventory.checkIndex(index);
        if (index < 9) {
            return new VirtualSlot(index, VirtualSlotKind.HOTBAR, index);
        }
        if (index < 36) {
            return new VirtualSlot(index, VirtualSlotKind.MAIN, index - 9);
        }
        if (index < 40) {
            return new VirtualSlot(index, VirtualSlotKind.ARMOR, index - 36);
        }
        return new VirtualSlot(index, VirtualSlotKind.OFFHAND, 0);
    }
}
