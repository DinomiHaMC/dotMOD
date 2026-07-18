package com.dinomiha.dotmod.feature.togglewalk;

public record MovementSnapshot(
        boolean walking,
        boolean sneaking,
        boolean sprintRetentionArmed,
        ForcedKeyState forcedKeys,
        MovementReleaseReason lastReleaseReason
) {
}
