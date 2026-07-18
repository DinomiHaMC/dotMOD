package com.dinomiha.dotmod.feature.togglewalk;

public record ForcedKeyState(boolean forward, boolean sprint, boolean sneak) {
    public static final ForcedKeyState NONE = new ForcedKeyState(false, false, false);
}
