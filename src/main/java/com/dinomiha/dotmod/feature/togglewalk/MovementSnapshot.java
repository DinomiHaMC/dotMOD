package com.dinomiha.dotmod.feature.togglewalk;

public record MovementSnapshot(
        boolean movementActive,
        boolean sneaking,
        boolean toggleSprintActive,
        boolean sprintRetentionArmed,
        ForcedKeyState forcedKeys,
        MovementReleaseReason lastReleaseReason
) {
}
