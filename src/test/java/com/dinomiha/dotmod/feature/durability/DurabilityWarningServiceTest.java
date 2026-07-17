package com.dinomiha.dotmod.feature.durability;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurabilityWarningServiceTest {
    @Test
    void cooldownUsesMonotonicTimeWithIrregularTicks() {
        DurabilityWarningService service = new DurabilityWarningService();

        assertTrue(service.shouldWarn("head:item", 0.1, 0.15, 100, 50));
        assertFalse(service.shouldWarn("head:item", 0.1, 0.15, 149, 50));
        assertTrue(service.shouldWarn("head:item", 0.1, 0.15, 150, 50));
        assertFalse(service.shouldWarn("head:item", 0.5, 0.15, 1000, 50));
    }

    @Test
    void slotsAreIndependentAndStaleKeysCanBeRemoved() {
        DurabilityWarningService service = new DurabilityWarningService();
        assertTrue(service.shouldWarn("head:a", 0.1, 0.15, 10, 100));
        assertTrue(service.shouldWarn("feet:b", 0.1, 0.15, 10, 100));

        service.retain(Set.of("feet:b"));

        assertTrue(service.shouldWarn("head:a", 0.1, 0.15, 11, 100));
        assertFalse(service.shouldWarn("feet:b", 0.1, 0.15, 11, 100));
    }

    @Test
    void recoveryAboveThresholdRearmsWarning() {
        DurabilityWarningService service = new DurabilityWarningService();
        assertTrue(service.shouldWarn("main:a", 0.1, 0.15, 10, 1000));
        assertFalse(service.shouldWarn("main:a", 0.9, 0.15, 11, 1000));
        assertTrue(service.shouldWarn("main:a", 0.1, 0.15, 12, 1000));
    }
}
