package com.dinomiha.dotmod.feature.death;

import com.dinomiha.dotmod.feature.death.model.DeathEffect;
import com.dinomiha.dotmod.feature.death.model.DeathSnapshot;
import com.dinomiha.dotmod.feature.invsee.MinecraftTestBootstrap;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DeathModelTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void rejectsNonFiniteCoordinatesAndTooManyEffects() {
        assertThrows(IllegalArgumentException.class, () -> snapshot(Double.NaN, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> snapshot(
                1.0,
                Collections.nCopies(DeathSnapshot.MAX_EFFECTS + 1, new DeathEffect("minecraft:speed", 0, 1, false, true, true))
        ));
    }

    private static DeathSnapshot snapshot(double x, java.util.List<DeathEffect> effects) {
        return new DeathSnapshot(
                UUID.randomUUID(), "Player", "minecraft:overworld",
                x, 0, 0, 0, 0, 0, "message", null, null, null,
                0, 0, 0, 0, effects, VirtualInventorySnapshot.empty()
        );
    }
}
