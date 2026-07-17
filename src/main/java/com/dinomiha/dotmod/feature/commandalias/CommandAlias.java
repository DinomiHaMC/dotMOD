package com.dinomiha.dotmod.feature.commandalias;

import java.util.Locale;
import java.util.regex.Pattern;

public record CommandAlias(String name, String template, boolean enabled) {
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_TEMPLATE_LENGTH = 256;
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9_-]{1,32}");

    public CommandAlias {
        name = normalizeName(name);
        template = normalizeTemplate(template);
        if (template.isBlank() || template.length() > MAX_TEMPLATE_LENGTH
                || template.codePoints().anyMatch(Character::isISOControl)) {
            throw new AliasException(AliasError.INVALID_TEMPLATE, "Alias template must contain 1 to 256 printable characters");
        }
        try {
            AliasTokenizer.tokenize(template);
        } catch (AliasException exception) {
            throw new AliasException(AliasError.INVALID_TEMPLATE, "Alias template has invalid quoting or escaping");
        }
    }

    private static String normalizeTemplate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.strip();
        return normalized.startsWith("/") ? normalized.substring(1).stripLeading() : normalized;
    }

    public static String normalizeName(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new AliasException(AliasError.INVALID_NAME, "Alias name must match [a-z0-9_-]{1,32}");
        }
        return normalized;
    }

    static String normalizeLookupName(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
