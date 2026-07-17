package com.dinomiha.dotmod.feature.commandlist;

import java.util.Objects;

public record CommandEntry(String command) {
    public static final int MAX_COMMAND_LENGTH = 256;

    public CommandEntry {
        command = normalize(command);
    }

    public static String normalize(String command) {
        Objects.requireNonNull(command, "command");
        String stripped = command.strip();
        int start = 0;
        while (start < stripped.length()) {
            char character = stripped.charAt(start);
            if (character != '/' && !Character.isWhitespace(character)) {
                break;
            }
            start++;
        }
        if (start == stripped.length()) {
            throw new IllegalArgumentException("Command is empty");
        }
        String normalized = "/" + stripped.substring(start).stripTrailing();
        if (normalized.length() > MAX_COMMAND_LENGTH) {
            throw new IllegalArgumentException("Command exceeds " + MAX_COMMAND_LENGTH + " characters");
        }
        return normalized;
    }
}
