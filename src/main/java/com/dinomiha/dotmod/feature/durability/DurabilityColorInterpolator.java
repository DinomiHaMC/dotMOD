package com.dinomiha.dotmod.feature.durability;

public final class DurabilityColorInterpolator {
    private DurabilityColorInterpolator() {
    }

    public static int interpolate(double fraction, int lowRgb, int middleRgb, int highRgb) {
        double safe = Double.isFinite(fraction) ? Math.max(0.0D, Math.min(1.0D, fraction)) : 0.0D;
        return safe <= 0.5D
                ? lerp(lowRgb, middleRgb, safe * 2.0D)
                : lerp(middleRgb, highRgb, (safe - 0.5D) * 2.0D);
    }

    private static int lerp(int from, int to, double progress) {
        int red = channel(from, to, 16, progress);
        int green = channel(from, to, 8, progress);
        int blue = channel(from, to, 0, progress);
        return red << 16 | green << 8 | blue;
    }

    private static int channel(int from, int to, int shift, double progress) {
        int first = from >>> shift & 0xFF;
        int second = to >>> shift & 0xFF;
        return (int) Math.round(first + (second - first) * progress);
    }
}
