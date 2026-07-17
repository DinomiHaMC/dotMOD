package com.dinomiha.dotmod.feature.preset;

import java.text.Normalizer;
import java.util.Locale;

public final class PresetNameValidator {
    public static final int MAX_LENGTH = 64;

    private PresetNameValidator() {
    }

    public static String normalize(String input) {
        if (input == null) {
            throw invalid("Preset name is missing");
        }
        String name = Normalizer.normalize(input.strip(), Normalizer.Form.NFKC);
        int length = name.codePointCount(0, name.length());
        if (length < 1 || length > MAX_LENGTH) {
            throw invalid("Preset name length must be 1.." + MAX_LENGTH);
        }
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            throw invalid("Preset names cannot contain path separators");
        }
        if (name.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid("Preset names cannot contain control characters");
        }
        return name;
    }

    public static String conflictKey(String name) {
        return normalize(name).toLowerCase(Locale.ROOT);
    }

    private static PresetException invalid(String message) {
        return new PresetException(PresetError.INVALID_NAME, message);
    }
}
