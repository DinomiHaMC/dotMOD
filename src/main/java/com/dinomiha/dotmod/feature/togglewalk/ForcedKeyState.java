package com.dinomiha.dotmod.feature.togglewalk;

public record ForcedKeyState(boolean forward, boolean sprint, boolean jump, boolean sneak) {
    public static final ForcedKeyState NONE = new ForcedKeyState(false, false, false, false);

    public boolean hasCapturedMovement() {
        return forward || sprint || jump;
    }
}
