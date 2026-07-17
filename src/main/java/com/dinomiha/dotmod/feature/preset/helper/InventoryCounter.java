package com.dinomiha.dotmod.feature.preset.helper;

import net.minecraft.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Immutable exact-stack counts collected from read-only inventory snapshots. */
public final class InventoryCounter {
    private final Map<ExactItemKey, Long> counts;

    public InventoryCounter(Iterable<ItemStack> stacks) {
        LinkedHashMap<ExactItemKey, Long> collected = new LinkedHashMap<>();
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                if (stack != null && !stack.isEmpty()) {
                    collected.merge(new ExactItemKey(stack), (long) stack.getCount(), Long::sum);
                }
            }
        }
        this.counts = Collections.unmodifiableMap(new LinkedHashMap<>(collected));
    }

    private InventoryCounter(Map<ExactItemKey, Long> counts) {
        this.counts = Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }

    public static InventoryCounter combine(Iterable<ItemStack> first, Iterable<ItemStack> second) {
        LinkedHashMap<ExactItemKey, Long> combined = new LinkedHashMap<>(new InventoryCounter(first).counts);
        for (Map.Entry<ExactItemKey, Long> entry : new InventoryCounter(second).counts.entrySet()) {
            combined.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
        return new InventoryCounter(combined);
    }

    public long count(ExactItemKey key) {
        return counts.getOrDefault(key, 0L);
    }

    public long count(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0L : count(new ExactItemKey(stack));
    }

    public InventoryCounter without(ExactItemKey key, long amount) {
        if (key == null || amount < 0L) {
            throw new IllegalArgumentException("Invalid inventory reservation");
        }
        LinkedHashMap<ExactItemKey, Long> remaining = new LinkedHashMap<>(counts);
        long next = Math.max(0L, remaining.getOrDefault(key, 0L) - amount);
        if (next == 0L) {
            remaining.remove(key);
        } else {
            remaining.put(key, next);
        }
        return new InventoryCounter(remaining);
    }

    public List<Entry> entries() {
        return counts.entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record Entry(ExactItemKey key, long count) {
        public Entry {
            if (key == null || count < 0) {
                throw new IllegalArgumentException("Invalid inventory count");
            }
        }
    }
}
