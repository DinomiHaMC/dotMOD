package com.dinomiha.dotmod.util;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static int parseRgb(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() == 8) {
            normalized = normalized.substring(2);
        }
        try {
            return Integer.parseInt(normalized, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static String normalizeHex(String value, String fallback) {
        int rgb = parseRgb(value, parseRgb(fallback, 0xFFFFFF));
        return String.format("#%06X", rgb);
    }
}
