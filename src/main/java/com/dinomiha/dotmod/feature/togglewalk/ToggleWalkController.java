package com.dinomiha.dotmod.feature.togglewalk;

public final class ToggleWalkController {
    private boolean movementActive;
    private boolean capturedForward;
    private boolean capturedJump;
    private boolean sneaking;
    private boolean sprintRetentionArmed;
    private MovementReleaseReason lastReleaseReason;

    public boolean toggleWalk(MovementContext context, ForcedKeyState capturedKeys) {
        if (!movementActive && !context.canRemainActive()) {
            return false;
        }
        movementActive = !movementActive;
        if (movementActive) {
            boolean effectiveSprint = context.retainSprint() && capturedKeys.sprint();
            ForcedKeyState capture = capturedKeys.forward() || effectiveSprint || capturedKeys.jump()
                    ? new ForcedKeyState(capturedKeys.forward(), effectiveSprint, capturedKeys.jump(), false)
                    : new ForcedKeyState(true, false, false, false);
            capturedForward = capture.forward();
            capturedJump = capture.jump();
            sprintRetentionArmed = capture.sprint();
        } else {
            capturedForward = false;
            capturedJump = false;
            sprintRetentionArmed = false;
        }
        return movementActive;
    }

    public boolean toggleSneak(MovementContext context) {
        if (!sneaking && !context.canRemainActive()) {
            return false;
        }
        sneaking = !sneaking;
        return sneaking;
    }

    public MovementSnapshot update(MovementContext context) {
        if (!context.canRemainActive()) {
            release(reason(context));
        } else if (movementActive && capturedForward && context.retainSprint() && context.playerSprinting()) {
            sprintRetentionArmed = true;
        } else if (!context.retainSprint()) {
            sprintRetentionArmed = false;
        }
        return snapshot();
    }

    public MovementSnapshot release(MovementReleaseReason reason) {
        movementActive = false;
        capturedForward = false;
        capturedJump = false;
        sneaking = false;
        sprintRetentionArmed = false;
        lastReleaseReason = reason;
        return snapshot();
    }

    public MovementSnapshot deactivateWalk(MovementReleaseReason reason) {
        movementActive = false;
        capturedForward = false;
        capturedJump = false;
        sprintRetentionArmed = false;
        lastReleaseReason = reason;
        return snapshot();
    }

    public MovementSnapshot deactivateSneak(MovementReleaseReason reason) {
        sneaking = false;
        lastReleaseReason = reason;
        return snapshot();
    }

    public MovementSnapshot snapshot() {
        return new MovementSnapshot(
                movementActive,
                sneaking,
                sprintRetentionArmed,
                new ForcedKeyState(
                        movementActive && capturedForward,
                        movementActive && sprintRetentionArmed,
                        movementActive && capturedJump,
                        sneaking
                ),
                lastReleaseReason
        );
    }

    private static MovementReleaseReason reason(MovementContext context) {
        if (!context.enabled()) return MovementReleaseReason.DISABLED;
        if (!context.livePlayer()) return MovementReleaseReason.NO_LIVE_PLAYER;
        if (!context.alive()) return MovementReleaseReason.DEATH;
        if (!context.gameplayAllowed()) return MovementReleaseReason.SCREEN;
        return MovementReleaseReason.FOCUS_LOSS;
    }
}
