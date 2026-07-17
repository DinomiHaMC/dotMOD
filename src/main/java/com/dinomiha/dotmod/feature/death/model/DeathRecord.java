package com.dinomiha.dotmod.feature.death.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DeathRecord(UUID id, Instant diedAt, DeathSnapshot snapshot, DeathScreenshot screenshot) {
    public DeathRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(diedAt, "diedAt");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(screenshot, "screenshot");
    }

    public Instant time() {
        return diedAt;
    }

    public DeathRecord withScreenshot(DeathScreenshot value) {
        return new DeathRecord(id, diedAt, snapshot, value);
    }
}
