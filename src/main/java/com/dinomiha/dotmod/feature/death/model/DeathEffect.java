package com.dinomiha.dotmod.feature.death.model;

import java.util.Objects;

public record DeathEffect(
        String id,
        String name,
        int amplifier,
        int durationTicks,
        boolean ambient,
        boolean showParticles,
        boolean showIcon
) {
    public DeathEffect {
        id = Objects.requireNonNull(id, "id").trim();
        if (id.isEmpty() || id.length() > 256) {
            throw new IllegalArgumentException("Effect id must be between 1 and 256 characters");
        }
        if (amplifier < 0 || durationTicks < -1) {
            throw new IllegalArgumentException("Invalid effect amplifier or duration");
        }
        name = name == null || name.isBlank() ? null : name.strip();
        if (name != null && name.length() > 256) {
            name = name.substring(0, 256);
        }
    }

    public DeathEffect(String id, int amplifier, int durationTicks, boolean ambient, boolean showParticles, boolean showIcon) {
        this(id, null, amplifier, durationTicks, ambient, showParticles, showIcon);
    }
}
