package com.dinomiha.dotmod.feature.death.model;

import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DeathSnapshot {
    public static final int MAX_EFFECTS = 64;

    private final UUID playerId;
    private final String playerName;
    private final String dimension;
    private final double x;
    private final double y;
    private final double z;
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final String deathMessage;
    private final String damageType;
    private final UUID attackerId;
    private final String attackerName;
    private final int selectedSlot;
    private final int experienceLevel;
    private final int totalExperience;
    private final float experienceProgress;
    private final List<DeathEffect> effects;
    private final VirtualInventorySnapshot inventory;

    public DeathSnapshot(
            UUID playerId,
            String playerName,
            String dimension,
            double x,
            double y,
            double z,
            int blockX,
            int blockY,
            int blockZ,
            String deathMessage,
            String damageType,
            UUID attackerId,
            String attackerName,
            int selectedSlot,
            int experienceLevel,
            int totalExperience,
            float experienceProgress,
            List<DeathEffect> effects,
            VirtualInventorySnapshot inventory
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.playerName = bounded(playerName, "playerName", 64, false);
        this.dimension = bounded(dimension, "dimension", 256, false);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Death coordinates must be finite");
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.deathMessage = bounded(deathMessage, "deathMessage", 4096, true);
        this.damageType = nullableBounded(damageType, "damageType", 256);
        this.attackerId = attackerId;
        this.attackerName = nullableBounded(attackerName, "attackerName", 256);
        if (selectedSlot < 0 || selectedSlot > 8) {
            throw new IllegalArgumentException("Selected slot must be in the hotbar");
        }
        if (experienceLevel < 0 || totalExperience < 0 || !Float.isFinite(experienceProgress)
                || experienceProgress < 0.0F || experienceProgress > 1.0F) {
            throw new IllegalArgumentException("Invalid experience values");
        }
        this.selectedSlot = selectedSlot;
        this.experienceLevel = experienceLevel;
        this.totalExperience = totalExperience;
        this.experienceProgress = experienceProgress;
        Objects.requireNonNull(effects, "effects");
        if (effects.size() > MAX_EFFECTS || effects.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Death snapshot has invalid effects");
        }
        this.effects = List.copyOf(effects);
        VirtualInventorySnapshot source = Objects.requireNonNull(inventory, "inventory");
        this.inventory = new VirtualInventorySnapshot(source.copyStacks());
    }

    public UUID playerId() { return playerId; }
    public UUID playerUuid() { return playerId; }
    public String playerName() { return playerName; }
    public String dimension() { return dimension; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public int blockX() { return blockX; }
    public int blockY() { return blockY; }
    public int blockZ() { return blockZ; }
    public String deathMessage() { return deathMessage; }
    public String damageType() { return damageType; }
    public UUID attackerId() { return attackerId; }
    public UUID attackerUuid() { return attackerId; }
    public String attackerName() { return attackerName; }
    public int selectedSlot() { return selectedSlot; }
    public int experienceLevel() { return experienceLevel; }
    public int totalExperience() { return totalExperience; }
    public float experienceProgress() { return experienceProgress; }
    public List<DeathEffect> effects() { return effects; }
    public VirtualInventorySnapshot inventory() { return new VirtualInventorySnapshot(inventory.copyStacks()); }

    private static String nullableBounded(String value, String field, int maximum) {
        return value == null || value.isBlank() ? null : bounded(value, field, maximum, true);
    }

    private static String bounded(String value, String field, int maximum, boolean allowEmpty) {
        String result = Objects.requireNonNull(value, field).strip();
        if ((!allowEmpty && result.isEmpty()) || result.length() > maximum) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return result;
    }
}
