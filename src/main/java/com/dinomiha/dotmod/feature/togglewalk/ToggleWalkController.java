package com.dinomiha.dotmod.feature.togglewalk;

public final class ToggleWalkController {
    private boolean walking;
    private boolean sneaking;
    private boolean sprintRetentionArmed;
    private MovementReleaseReason lastReleaseReason;

    public boolean toggleWalk(MovementContext context) {
        if (!walking && !context.canRemainActive()) {
            return false;
        }
        walking = !walking;
        if (!walking) {
            sprintRetentionArmed = false;
        }
        return walking;
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
        } else if (walking && context.retainSprint() && context.playerSprinting()) {
            sprintRetentionArmed = true;
        } else if (!context.retainSprint()) {
            sprintRetentionArmed = false;
        }
        return snapshot();
    }

    public MovementSnapshot release(MovementReleaseReason reason) {
        walking = false;
        sneaking = false;
        sprintRetentionArmed = false;
        lastReleaseReason = reason;
        return snapshot();
    }

    public MovementSnapshot deactivateWalk(MovementReleaseReason reason) {
        walking = false;
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
                walking,
                sneaking,
                sprintRetentionArmed,
                new ForcedKeyState(walking, walking && sprintRetentionArmed, sneaking),
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
