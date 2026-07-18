package com.dinomiha.dotmod.feature.fullbrightness;

public final class GammaOwnershipState {
    public static final double MAX_GAMMA = 1.0;

    private boolean active;
    private boolean suspended;
    private double restoreGamma;

    public Update toggle(double currentGamma) {
        if (active) {
            return deactivate(currentGamma);
        }
        active = true;
        suspended = false;
        restoreGamma = currentGamma;
        return update(MAX_GAMMA);
    }

    public Update observe(double currentGamma, boolean videoOptionsOpen, boolean enabled) {
        if (!active) return update(null);
        if (!enabled) return deactivate(currentGamma);
        if (videoOptionsOpen) {
            if (!suspended) {
                suspended = true;
                return update(restoreGamma);
            }
            restoreGamma = currentGamma;
            return update(null);
        }
        if (suspended) {
            restoreGamma = currentGamma;
            suspended = false;
            return update(MAX_GAMMA);
        }
        if (Double.compare(currentGamma, MAX_GAMMA) != 0) {
            restoreGamma = currentGamma;
            return update(MAX_GAMMA);
        }
        return update(null);
    }

    public Update deactivate(double currentGamma) {
        if (!active) return update(null);
        if (suspended) restoreGamma = currentGamma;
        active = false;
        suspended = false;
        return update(restoreGamma);
    }

    public boolean active() {
        return active;
    }

    public boolean suspended() {
        return suspended;
    }

    private Update update(Double gamma) {
        return new Update(active, suspended, gamma);
    }

    public record Update(boolean active, boolean suspended, Double gammaToApply) {
    }
}
