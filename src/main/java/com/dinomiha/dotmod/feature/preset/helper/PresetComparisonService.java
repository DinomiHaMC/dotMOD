package com.dinomiha.dotmod.feature.preset.helper;

import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PresetComparisonService {
    public PresetProgress compare(VirtualInventorySnapshot preset, InventoryCounter available) {
        if (preset == null || available == null) {
            throw new IllegalArgumentException("Preset and inventory counts are required");
        }
        LinkedHashMap<ExactItemKey, Long> requiredCounts = new LinkedHashMap<>();
        List<ItemStack> stacks = preset.copyStacks();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                requiredCounts.merge(new ExactItemKey(stack), (long) stack.getCount(), Long::sum);
            }
        }

        List<PresetRequirement> requirements = new ArrayList<>();
        long requiredTotal = 0L;
        long satisfiedTotal = 0L;
        for (Map.Entry<ExactItemKey, Long> entry : requiredCounts.entrySet()) {
            PresetRequirement requirement = new PresetRequirement(
                    entry.getKey(), entry.getValue(), available.count(entry.getKey())
            );
            requirements.add(requirement);
            requiredTotal += requirement.required();
            satisfiedTotal += requirement.satisfied();
        }

        Map<ExactItemKey, Long> remaining = new LinkedHashMap<>();
        for (PresetRequirement requirement : requirements) {
            remaining.put(requirement.key(), requirement.available());
        }
        Set<Integer> missingSlots = new HashSet<>();
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ExactItemKey key = new ExactItemKey(stack);
            long present = remaining.getOrDefault(key, 0L);
            if (present < stack.getCount()) {
                missingSlots.add(slot);
            }
            remaining.put(key, Math.max(0L, present - stack.getCount()));
        }

        long missingTotal = requiredTotal - satisfiedTotal;
        int percentage = requiredTotal == 0L || missingTotal == 0L
                ? 100
                : (int) (satisfiedTotal * 100L / requiredTotal);
        return new PresetProgress(
                requirements, missingSlots, requiredTotal, satisfiedTotal, missingTotal, percentage
        );
    }
}
