package com.dinomiha.dotmod.feature.durability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurabilityColorInterpolatorTest {
    @Test
    void coversEndpointsAndIntermediateValues() {
        assertEquals(0xFF0000, color(0.0));
        assertEquals(0xFF8000, color(0.25));
        assertEquals(0xFFFF00, color(0.5));
        assertEquals(0x80FF00, color(0.75));
        assertEquals(0x00FF00, color(1.0));
    }

    @Test
    void clampsAndHandlesNan() {
        assertEquals(0xFF0000, color(-1.0));
        assertEquals(0x00FF00, color(2.0));
        assertEquals(0xFF0000, color(Double.NaN));
    }

    private static int color(double fraction) {
        return DurabilityColorInterpolator.interpolate(fraction, 0xFF0000, 0xFFFF00, 0x00FF00);
    }
}
