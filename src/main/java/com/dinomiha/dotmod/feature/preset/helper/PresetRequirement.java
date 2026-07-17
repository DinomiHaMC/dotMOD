package com.dinomiha.dotmod.feature.preset.helper;

import net.minecraft.item.ItemStack;

import java.math.BigInteger;

public final class PresetRequirement {
    private final ExactItemKey key;
    private final long required;
    private final long available;

    public PresetRequirement(ExactItemKey key, long required, long available) {
        if (key == null || required <= 0 || available < 0) {
            throw new IllegalArgumentException("Invalid preset requirement");
        }
        this.key = key;
        this.required = required;
        this.available = available;
    }

    public ExactItemKey key() {
        return key;
    }

    public ItemStack stack() {
        return key.exemplar();
    }

    public long required() {
        return required;
    }

    public long available() {
        return available;
    }

    public long satisfied() {
        return Math.min(required, available);
    }

    public long missing() {
        return Math.max(0L, required - available);
    }

    public boolean complete() {
        return missing() == 0L;
    }

    public int percentage() {
        return complete() ? 100 : BigInteger.valueOf(satisfied())
                .multiply(BigInteger.valueOf(100L))
                .divide(BigInteger.valueOf(required))
                .intValue();
    }
}
