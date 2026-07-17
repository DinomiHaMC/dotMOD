package com.dinomiha.dotmod.feature.durability;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class DurabilityWarningService {
    private final Map<String, Long> lastWarnings = new HashMap<>();
    private final Map<String, Double> lastFractions = new HashMap<>();

    public boolean shouldWarn(
            String identity,
            double fraction,
            double threshold,
            long nowNanos,
            long cooldownNanos
    ) {
        if (identity == null || identity.isBlank() || !Double.isFinite(fraction)
                || !Double.isFinite(threshold) || cooldownNanos < 0L) {
            return false;
        }
        Double previousFraction = lastFractions.put(identity, fraction);
        if (fraction > threshold) {
            lastWarnings.remove(identity);
            return false;
        }
        if (previousFraction != null && fraction > previousFraction + 0.05D) {
            lastWarnings.remove(identity);
        }
        Long previous = lastWarnings.get(identity);
        if (previous != null && nowNanos - previous < cooldownNanos) {
            return false;
        }
        lastWarnings.put(identity, nowNanos);
        return true;
    }

    public void retain(Set<String> activeIdentities) {
        lastWarnings.keySet().retainAll(activeIdentities);
        lastFractions.keySet().retainAll(activeIdentities);
    }

    public void clear() {
        lastWarnings.clear();
        lastFractions.clear();
    }
}
