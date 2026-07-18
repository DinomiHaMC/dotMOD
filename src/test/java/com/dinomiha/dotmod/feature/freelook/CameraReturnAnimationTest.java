package com.dinomiha.dotmod.feature.freelook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraReturnAnimationTest {
    @Test
    void smoothstepReturnIsMonotonicAndCompletes() {
        CameraReturnAnimation animation = new CameraReturnAnimation(100.0, -50.0, 1_000_000_000L, 200);
        double previous = Double.POSITIVE_INFINITY;
        for (long time = 1_000_000_000L; time <= 1_200_000_000L; time += 20_000_000L) {
            var sample = animation.sample(time);
            assertTrue(Math.abs(sample.yawOffset()) <= previous);
            previous = Math.abs(sample.yawOffset());
        }
        assertTrue(animation.sample(1_200_000_000L).complete());
        assertEquals(0.0, animation.sample(1_200_000_000L).yawOffset());
    }

    @Test
    void zeroDurationCompletesImmediately() {
        var sample = new CameraReturnAnimation(10.0, 10.0, 0, 0).sample(0);
        assertTrue(sample.complete());
        assertEquals(0.0, sample.pitchOffset());
    }
}
