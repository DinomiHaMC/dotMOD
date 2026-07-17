package com.dinomiha.dotmod.feature.preset.helper;

import java.util.List;
import java.util.Set;

public record PresetProgress(
        List<PresetRequirement> requirements,
        Set<Integer> missingSlots,
        long required,
        long available,
        long missing,
        int percentage
) {
    public PresetProgress {
        requirements = List.copyOf(requirements);
        missingSlots = Set.copyOf(missingSlots);
        if (required < 0 || available < 0 || missing < 0 || percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Invalid preset progress");
        }
    }

    public boolean complete() {
        return missing == 0L;
    }
}
