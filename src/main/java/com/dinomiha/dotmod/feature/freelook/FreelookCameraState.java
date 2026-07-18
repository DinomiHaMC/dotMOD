package com.dinomiha.dotmod.feature.freelook;

public final class FreelookCameraState {
    private double yawOffset;
    private double pitchOffset;

    public void applyLookDelta(
            double yawDelta,
            double pitchDelta,
            double basePitch,
            double sensitivity,
            boolean invertX,
            boolean invertY
    ) {
        if (!Double.isFinite(yawDelta) || !Double.isFinite(pitchDelta)
                || !Double.isFinite(basePitch) || !Double.isFinite(sensitivity)) {
            return;
        }
        double multiplier = Math.max(0.1, Math.min(4.0, sensitivity));
        yawOffset = wrapDegrees(yawOffset + yawDelta * 0.15 * multiplier * (invertX ? -1.0 : 1.0));
        double effectivePitch = basePitch + pitchOffset
                + pitchDelta * 0.15 * multiplier * (invertY ? -1.0 : 1.0);
        pitchOffset = Math.max(-90.0, Math.min(90.0, effectivePitch)) - basePitch;
    }

    public float effectiveYaw(float baseYaw) {
        if (!Float.isFinite(baseYaw)) return 0.0F;
        return (float) wrapDegrees(baseYaw + yawOffset);
    }

    public float effectivePitch(float basePitch) {
        if (!Float.isFinite(basePitch)) return 0.0F;
        return (float) Math.max(-90.0, Math.min(90.0, basePitch + pitchOffset));
    }

    public double yawOffset() {
        return yawOffset;
    }

    public double pitchOffset() {
        return pitchOffset;
    }

    public void setOffsets(double yawOffset, double pitchOffset) {
        this.yawOffset = Double.isFinite(yawOffset) ? wrapDegrees(yawOffset) : 0.0;
        this.pitchOffset = Double.isFinite(pitchOffset) ? pitchOffset : 0.0;
    }

    public void reset() {
        yawOffset = 0.0;
        pitchOffset = 0.0;
    }

    public static double wrapDegrees(double value) {
        if (!Double.isFinite(value)) return 0.0;
        double wrapped = value % 360.0;
        if (wrapped >= 180.0) wrapped -= 360.0;
        if (wrapped < -180.0) wrapped += 360.0;
        return wrapped;
    }
}
