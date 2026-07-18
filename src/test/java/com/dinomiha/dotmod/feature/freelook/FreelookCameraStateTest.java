package com.dinomiha.dotmod.feature.freelook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FreelookCameraStateTest {
    @Test
    void appliesRelativeWrappedClampedInvertedOffsets() {
        FreelookCameraState state = new FreelookCameraState();
        state.applyLookDelta(1200.0, 200.0, 0.0, 2.0, false, true);
        assertEquals(0.0, state.yawOffset(), 0.0001);
        assertEquals(-60.0F, state.effectivePitch(0.0F), 0.0001);
        assertEquals(-90.0F, state.effectivePitch(-60.0F), 0.0001);
    }

    @Test
    void rejectsNonFiniteInput() {
        FreelookCameraState state = new FreelookCameraState();
        state.applyLookDelta(Double.NaN, 1.0, 0.0, 1.0, false, false);
        assertEquals(0.0, state.yawOffset());
        assertEquals(0.0, state.pitchOffset());
    }

    @Test
    void wrapsYawAndClampsSensitivityRange() {
        FreelookCameraState state = new FreelookCameraState();
        state.applyLookDelta(2400.0, 100.0, 0.0, 99.0, true, false);
        assertEquals(0.0, state.yawOffset(), 0.0001);
        assertEquals(60.0, state.pitchOffset(), 0.0001);
        state.setOffsets(181.0, Double.POSITIVE_INFINITY);
        assertEquals(-179.0, state.yawOffset(), 0.0001);
        assertEquals(0.0, state.pitchOffset(), 0.0001);
    }

    @Test
    void clampsOffsetAtTheEffectivePitchWithoutAccumulatingExcess() {
        FreelookCameraState state = new FreelookCameraState();
        state.applyLookDelta(0.0, 10_000.0, 30.0, 1.0, false, false);
        assertEquals(60.0, state.pitchOffset(), 0.0001);
        state.applyLookDelta(0.0, -10.0, 30.0, 1.0, false, false);
        assertEquals(58.5, state.pitchOffset(), 0.0001);
    }
}
