package com.dinomiha.dotmod.feature.fullbrightness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FullBrightnessStateTest {
    @Test
    void togglesOnlyWhileEnabled() {
        FullBrightnessState state = new FullBrightnessState();
        assertFalse(state.toggle(false).active());
        assertTrue(state.toggle(true).active());
        assertFalse(state.toggle(true).active());
    }

    @Test
    void deactivationIsIdempotent() {
        FullBrightnessState state = new FullBrightnessState();
        state.toggle(true);
        assertTrue(state.deactivate().changed());
        assertFalse(state.deactivate().changed());
    }
}
