package com.dinomiha.dotmod.feature.durability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurabilityReadingTest {
    @Test
    void calculatesAndClampsRemainingDurability() {
        DurabilityReading reading = DurabilityReading.of(DurabilitySlot.MAIN_HAND, 100, 25);
        assertEquals(75, reading.remaining());
        assertEquals(0.75D, reading.remainingFraction());

        assertEquals(0, DurabilityReading.of(DurabilitySlot.HEAD, 100, 500).remaining());
        assertEquals(100, DurabilityReading.of(DurabilitySlot.FEET, 100, -10).remaining());
    }

    @Test
    void rejectsNonDamageableMaximum() {
        assertThrows(IllegalArgumentException.class, () -> DurabilityReading.of(DurabilitySlot.CHEST, 0, 0));
    }
}
