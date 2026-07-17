package com.dinomiha.dotmod.feature.commandalias;

import java.util.ArrayList;
import java.util.List;

final class AliasTokenizer {
    private AliasTokenizer() {
    }

    static List<String> tokenize(String input) {
        if (input == null || input.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid("Command contains control characters");
        }
        List<String> tokens = new ArrayList<>();
        int index = 0;
        while (index < input.length()) {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
            if (index == input.length()) {
                break;
            }
            StringBuilder token = new StringBuilder();
            char quote = 0;
            while (index < input.length()) {
                char character = input.charAt(index++);
                if (quote == 0 && Character.isWhitespace(character)) {
                    break;
                }
                if (quote == 0 && (character == '\'' || character == '"')) {
                    quote = character;
                    continue;
                }
                if (quote != 0 && character == quote) {
                    quote = 0;
                    continue;
                }
                if (character == '\\') {
                    if (index == input.length()) {
                        throw invalid("Command ends with an escape");
                    }
                    char escaped = input.charAt(index++);
                    if (quote != 0 && escaped != quote && escaped != '\\') {
                        throw invalid("Invalid quoted escape");
                    }
                    token.append(escaped);
                    continue;
                }
                token.append(character);
            }
            if (quote != 0) {
                throw invalid("Unclosed quoted argument");
            }
            tokens.add(token.toString());
        }
        return List.copyOf(tokens);
    }

    static String escape(String value) {
        if (value.isEmpty()) {
            return "\"\"";
        }
        boolean simple = value.chars().allMatch(AliasTokenizer::isAllowedInUnquotedString);
        if (simple) {
            return value;
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static boolean isAllowedInUnquotedString(int character) {
        return character >= '0' && character <= '9'
                || character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character == '_' || character == '-' || character == '.' || character == '+';
    }

    private static AliasException invalid(String message) {
        return new AliasException(AliasError.INVALID_INPUT, message);
    }
}
