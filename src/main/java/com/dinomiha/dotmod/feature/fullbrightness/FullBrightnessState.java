package com.dinomiha.dotmod.feature.fullbrightness;

public final class FullBrightnessState {
    private boolean active;

    public Update toggle(boolean enabled) {
        boolean previous = active;
        active = enabled && !active;
        return new Update(active, previous != active);
    }

    public Update deactivate() {
        boolean previous = active;
        active = false;
        return new Update(false, previous);
    }

    public boolean active() {
        return active;
    }

    public record Update(boolean active, boolean changed) {
    }
}
