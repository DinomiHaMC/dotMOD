package com.dinomiha.dotmod.feature.fullbrightness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GammaOwnershipStateTest {
    @Test
    void togglesMaximumAndRestoresExactOriginalGamma() {
        GammaOwnershipState state = new GammaOwnershipState();
        assertEquals(1.0, state.toggle(0.37).gammaToApply());
        assertTrue(state.active());
        assertEquals(0.37, state.toggle(1.0).gammaToApply());
        assertFalse(state.active());
    }

    @Test
    void suspendsForVideoOptionsAndRetainsSelectedUserValue() {
        GammaOwnershipState state = new GammaOwnershipState();
        state.toggle(0.25);
        assertEquals(0.25, state.observe(1.0, true, true).gammaToApply());
        assertTrue(state.suspended());
        assertNull(state.observe(0.64, true, true).gammaToApply());
        assertEquals(1.0, state.observe(0.64, false, true).gammaToApply());
        assertEquals(0.64, state.toggle(1.0).gammaToApply());
    }

    @Test
    void externalGammaChangesBecomeTheSafeRestoreValue() {
        GammaOwnershipState state = new GammaOwnershipState();
        state.toggle(0.1);
        assertEquals(1.0, state.observe(0.8, false, true).gammaToApply());
        assertEquals(0.8, state.deactivate(1.0).gammaToApply());
        assertNull(state.deactivate(0.8).gammaToApply());
    }

    @Test
    void disableWhileSuspendedKeepsCurrentUserSelection() {
        GammaOwnershipState state = new GammaOwnershipState();
        state.toggle(0.2);
        state.observe(1.0, true, true);
        assertEquals(0.7, state.observe(0.7, true, false).gammaToApply());
        assertFalse(state.active());
    }
}
