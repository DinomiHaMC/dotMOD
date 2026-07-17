package com.dinomiha.dotmod.feature.commandlist;

import java.util.Locale;
import java.util.Set;

public final class SensitiveCommandFilter {
    private static final Set<String> SENSITIVE_ROOTS = Set.of(
            "login",
            "register",
            "changepassword",
            "password",
            "auth",
            "2fa",
            "otp"
    );

    private SensitiveCommandFilter() {
    }

    public static boolean isSensitive(String command) {
        return SENSITIVE_ROOTS.contains(commandRoot(command));
    }

    static String commandRoot(String command) {
        if (command == null) {
            return "";
        }
        String stripped = command.strip();
        int start = 0;
        while (start < stripped.length()) {
            char character = stripped.charAt(start);
            if (character != '/' && !Character.isWhitespace(character)) {
                break;
            }
            start++;
        }
        int end = start;
        while (end < stripped.length() && !Character.isWhitespace(stripped.charAt(end))) {
            end++;
        }
        return stripped.substring(start, end).toLowerCase(Locale.ROOT);
    }
}
