package com.dinomiha.dotmod.feature.freelook;

public final class CameraReturnAnimation {
    private final double startYaw;
    private final double startPitch;
    private final long startTimeNanos;
    private final long durationNanos;

    public CameraReturnAnimation(double startYaw, double startPitch, long startTimeNanos, int durationMs) {
        this.startYaw = Double.isFinite(startYaw) ? startYaw : 0.0;
        this.startPitch = Double.isFinite(startPitch) ? startPitch : 0.0;
        this.startTimeNanos = startTimeNanos;
        this.durationNanos = Math.max(0, durationMs) * 1_000_000L;
    }

    public Sample sample(long timeNanos) {
        double progress = durationNanos == 0 ? 1.0 : Math.max(0.0,
                Math.min(1.0, (timeNanos - startTimeNanos) / (double) durationNanos));
        double eased = progress * progress * (3.0 - 2.0 * progress);
        return new Sample(startYaw * (1.0 - eased), startPitch * (1.0 - eased), progress >= 1.0);
    }

    public record Sample(double yawOffset, double pitchOffset, boolean complete) {
    }
}
