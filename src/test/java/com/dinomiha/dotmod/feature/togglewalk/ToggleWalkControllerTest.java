package com.dinomiha.dotmod.feature.togglewalk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToggleWalkControllerTest {
    private static MovementContext context(boolean sprinting) {
        return new MovementContext(true, true, true, true, true, true, sprinting);
    }

    @Test
    void walkingForcesForwardAndRetainsSprintOnlyAfterObservation() {
        ToggleWalkController controller = new ToggleWalkController();
        assertTrue(controller.toggleWalk(context(false)));
        assertEquals(new ForcedKeyState(true, false, false), controller.update(context(false)).forcedKeys());
        assertEquals(new ForcedKeyState(true, true, false), controller.update(context(true)).forcedKeys());
        assertEquals(new ForcedKeyState(true, true, false), controller.update(context(false)).forcedKeys());
    }

    @Test
    void lifecycleAndEmergencyReleaseEveryOwnedState() {
        ToggleWalkController controller = new ToggleWalkController();
        controller.toggleWalk(context(true));
        controller.toggleSneak(context(true));
        controller.update(context(true));
        MovementSnapshot released = controller.release(MovementReleaseReason.EMERGENCY);
        assertEquals(ForcedKeyState.NONE, released.forcedKeys());
        assertEquals(MovementReleaseReason.EMERGENCY, released.lastReleaseReason());
    }

    @Test
    void invalidContextCannotActivateAndClearsActiveState() {
        ToggleWalkController controller = new ToggleWalkController();
        MovementContext invalid = new MovementContext(true, false, false, true, true, true, false);
        assertFalse(controller.toggleWalk(invalid));
        controller.toggleWalk(context(false));
        assertEquals(ForcedKeyState.NONE, controller.update(invalid).forcedKeys());
    }

    @Test
    void featureDisableCanReleaseOneMovementModeWithoutTheOther() {
        ToggleWalkController controller = new ToggleWalkController();
        controller.toggleWalk(context(false));
        controller.toggleSneak(context(false));
        assertTrue(controller.deactivateWalk(MovementReleaseReason.DISABLED).sneaking());
        assertFalse(controller.snapshot().walking());
        assertEquals(ForcedKeyState.NONE, controller.deactivateSneak(MovementReleaseReason.DISABLED).forcedKeys());
    }
}
