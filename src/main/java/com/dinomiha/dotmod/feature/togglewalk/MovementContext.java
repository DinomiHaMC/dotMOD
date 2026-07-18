package com.dinomiha.dotmod.feature.togglewalk;

public record MovementContext(
        boolean enabled,
        boolean livePlayer,
        boolean alive,
        boolean gameplayAllowed,
        boolean focused,
        boolean retainSprint,
        boolean playerSprinting
) {
    public boolean canRemainActive() {
        return enabled && livePlayer && alive && gameplayAllowed && focused;
    }
}
