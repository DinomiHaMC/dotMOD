package com.dinomiha.dotmod.feature.durability;

public record DurabilityReading(
        DurabilitySlot slot,
        int maxDamage,
        int damage,
        int remaining,
        double remainingFraction
) {
    public static DurabilityReading of(DurabilitySlot slot, int maxDamage, int damage) {
        if (slot == null || maxDamage <= 0) {
            throw new IllegalArgumentException("Damageable durability requires a positive maximum");
        }
        int safeDamage = Math.max(0, Math.min(maxDamage, damage));
        int remaining = maxDamage - safeDamage;
        return new DurabilityReading(slot, maxDamage, safeDamage, remaining, (double) remaining / maxDamage);
    }
}
