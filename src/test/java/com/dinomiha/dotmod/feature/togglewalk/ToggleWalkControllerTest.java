package com.dinomiha.dotmod.feature.togglewalk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToggleWalkControllerTest {
    private static final ForcedKeyState NONE = ForcedKeyState.NONE;

    private static MovementContext context(boolean sprinting) {
        return new MovementContext(true, true, true, true, true, true, sprinting);
    }

    @Test
    void walkingForcesForwardAndRetainsSprintOnlyAfterObservation() {
        ToggleWalkController controller = new ToggleWalkController();
        assertTrue(controller.toggleWalk(context(false), NONE));
        assertEquals(new ForcedKeyState(true, false, false, false), controller.update(context(false)).forcedKeys());
        assertEquals(new ForcedKeyState(true, true, false, false), controller.update(context(true)).forcedKeys());
        assertEquals(new ForcedKeyState(true, true, false, false), controller.update(context(false)).forcedKeys());
    }

    @Test
    void capturesEverySupportedPhysicalCombination() {
        assertCapture(new ForcedKeyState(false, false, true, false));
        assertCapture(new ForcedKeyState(false, true, true, false));
        assertCapture(new ForcedKeyState(true, false, true, false));
        assertCapture(new ForcedKeyState(true, false, false, false));
    }

    @Test
    void capturedSprintIsDiscardedWhenRetentionIsDisabled() {
        ToggleWalkController controller = new ToggleWalkController();
        MovementContext noRetention = new MovementContext(true, true, true, true, true, false, true);
        controller.toggleWalk(noRetention, new ForcedKeyState(false, true, true, false));
        assertEquals(new ForcedKeyState(false, false, true, false), controller.update(noRetention).forcedKeys());
    }

    @Test
    void toggleOffReleasesCapturedJumpAndOtherMovement() {
        ToggleWalkController controller = new ToggleWalkController();
        controller.toggleWalk(context(false), new ForcedKeyState(true, true, true, false));
        assertFalse(controller.toggleWalk(context(false), NONE));
        assertEquals(ForcedKeyState.NONE, controller.snapshot().forcedKeys());
    }

    @Test
    void lifecycleAndEmergencyReleaseEveryOwnedState() {
        ToggleWalkController controller = new ToggleWalkController();
        controller.toggleWalk(context(true), NONE);
        controller.toggleSprint(context(true));
        controller.toggleSneak(context(true));
        controller.update(context(true));
        MovementSnapshot released = controller.release(MovementReleaseReason.EMERGENCY);
        assertFalse(released.toggleSprintActive());
        assertEquals(ForcedKeyState.NONE, released.forcedKeys());
        assertEquals(MovementReleaseReason.EMERGENCY, released.lastReleaseReason());
    }

    @Test
    void invalidContextCannotActivateAndClearsActiveState() {
        ToggleWalkController controller = new ToggleWalkController();
        MovementContext invalid = new MovementContext(true, false, false, true, true, true, false);
        assertFalse(controller.toggleWalk(invalid, NONE));
        controller.toggleWalk(context(false), NONE);
        assertEquals(ForcedKeyState.NONE, controller.update(invalid).forcedKeys());
    }

    @Test
    void featureDisableCanReleaseOneMovementModeWithoutTheOther() {
        ToggleWalkController controller = new ToggleWalkController();
        controller.toggleWalk(context(false), NONE);
        controller.toggleSneak(context(false));
        assertTrue(controller.deactivateWalk(MovementReleaseReason.DISABLED).sneaking());
        assertFalse(controller.snapshot().movementActive());
        assertEquals(ForcedKeyState.NONE, controller.deactivateSneak(MovementReleaseReason.DISABLED).forcedKeys());
    }

    @Test
    void sprintOnlyToggleImmediatelyForcesSprintAndSecondToggleReleasesIt() {
        ToggleWalkController controller = new ToggleWalkController();

        assertTrue(controller.toggleSprint(context(false)));
        assertEquals(new ForcedKeyState(false, true, false, false), controller.snapshot().forcedKeys());
        assertFalse(controller.snapshot().movementActive());

        assertFalse(controller.toggleSprint(context(false)));
        assertEquals(ForcedKeyState.NONE, controller.snapshot().forcedKeys());
    }

    @Test
    void sprintToggleIsIndependentFromCapturedSprintRetention() {
        ToggleWalkController controller = new ToggleWalkController();
        controller.toggleWalk(context(false), new ForcedKeyState(true, true, false, false));
        controller.toggleSprint(context(false));

        assertFalse(controller.toggleSprint(context(false)));
        assertFalse(controller.snapshot().toggleSprintActive());
        assertTrue(controller.snapshot().sprintRetentionArmed());
        assertEquals(new ForcedKeyState(true, true, false, false), controller.snapshot().forcedKeys());
    }

    @Test
    void lifecycleInvalidationClearsSprintToggle() {
        ToggleWalkController controller = new ToggleWalkController();
        controller.toggleSprint(context(false));
        MovementContext focusLost = new MovementContext(true, true, true, true, false, true, false);

        MovementSnapshot released = controller.update(focusLost);

        assertFalse(released.toggleSprintActive());
        assertEquals(ForcedKeyState.NONE, released.forcedKeys());
        assertEquals(MovementReleaseReason.FOCUS_LOSS, released.lastReleaseReason());
    }

    @Test
    void sprintFeatureDisableClearsOnlySprintLatch() {
        ToggleWalkController controller = new ToggleWalkController();
        controller.toggleWalk(context(false), new ForcedKeyState(true, false, true, false));
        controller.toggleSneak(context(false));
        controller.toggleSprint(context(false));

        MovementSnapshot snapshot = controller.deactivateSprint(MovementReleaseReason.DISABLED);

        assertFalse(snapshot.toggleSprintActive());
        assertTrue(snapshot.movementActive());
        assertTrue(snapshot.sneaking());
        assertEquals(new ForcedKeyState(true, false, true, true), snapshot.forcedKeys());
    }

    @Test
    void sprintOnlyCaptureFallsBackToWalkWhenRetentionIsDisabled() {
        ToggleWalkController controller = new ToggleWalkController();
        MovementContext noRetention = new MovementContext(true, true, true, true, true, false, false);
        controller.toggleWalk(noRetention, new ForcedKeyState(false, true, false, false));
        assertEquals(new ForcedKeyState(true, false, false, false), controller.snapshot().forcedKeys());
    }

    private static void assertCapture(ForcedKeyState capture) {
        ToggleWalkController controller = new ToggleWalkController();
        controller.toggleWalk(context(false), capture);
        assertTrue(controller.snapshot().movementActive());
        assertEquals(capture, controller.snapshot().forcedKeys());
    }
}
