package com.dinomiha.dotmod.feature.playercolor;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class StrictHexColor {
    private static final Pattern RGB = Pattern.compile("#?[0-9a-fA-F]{6}");

    private StrictHexColor() {
    }

    public static Optional<String> parse(String value) {
        if (value == null || !RGB.matcher(value).matches()) {
            return Optional.empty();
        }
        String digits = value.charAt(0) == '#' ? value.substring(1) : value;
        return Optional.of("#" + digits.toUpperCase(Locale.ROOT));
    }
}
