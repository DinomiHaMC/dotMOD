package com.dinomiha.dotmod.feature.commandalias;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AliasExpander {
    public static final int MAX_DEPTH = 16;
    public static final int MAX_OUTPUT_LENGTH = 256;

    private final Map<String, CommandAlias> aliases;

    public AliasExpander(Collection<CommandAlias> aliases) {
        this.aliases = new HashMap<>();
        for (CommandAlias alias : aliases) {
            CommandAlias previous = this.aliases.put(alias.name(), alias);
            if (previous != null) {
                throw new AliasException(AliasError.DUPLICATE_NAME, "Duplicate alias " + alias.name());
            }
        }
    }

    public String expand(String input) {
        boolean slash = input != null && input.startsWith("/");
        String command = slash ? input.substring(1) : input;
        String expanded = expand(command, new HashSet<>(), 0);
        String result = slash ? "/" + expanded : expanded;
        if (!expanded.equals(command)) {
            checkLength(result);
        }
        return result;
    }

    private String expand(String input, Set<String> visited, int depth) {
        List<String> tokens = AliasTokenizer.tokenize(input);
        if (tokens.isEmpty()) {
            throw new AliasException(AliasError.INVALID_INPUT, "Command is empty");
        }
        CommandAlias alias = aliases.get(tokens.getFirst());
        if (alias == null || !alias.enabled()) {
            if (depth > 0) {
                checkLength(input);
            }
            return input;
        }
        if (!visited.add(alias.name())) {
            throw new AliasException(AliasError.CYCLE, "Alias cycle at " + alias.name());
        }
        if (depth >= MAX_DEPTH) {
            throw new AliasException(AliasError.MAX_DEPTH, "Alias expansion exceeds depth 16");
        }
        String expanded = apply(alias.template(), tokens.subList(1, tokens.size()));
        checkLength(expanded);
        return expand(expanded, visited, depth + 1);
    }

    private static String apply(String template, List<String> arguments) {
        StringBuilder output = new StringBuilder();
        boolean placeholder = false;
        for (int index = 0; index < template.length(); index++) {
            char character = template.charAt(index);
            if (character != '$' || index + 1 == template.length()) {
                output.append(character);
                continue;
            }
            char marker = template.charAt(index + 1);
            if (marker == '$') {
                output.append('$');
                placeholder = true;
                index++;
            } else if (marker == '*') {
                appendArguments(output, arguments);
                placeholder = true;
                index++;
            } else if (marker >= '1' && marker <= '9') {
                int argument = marker - '1';
                if (argument < arguments.size()) {
                    output.append(AliasTokenizer.escape(arguments.get(argument)));
                }
                placeholder = true;
                index++;
            } else {
                output.append(character);
            }
        }
        if (!placeholder && !arguments.isEmpty()) {
            appendSeparator(output);
            appendArguments(output, arguments);
        }
        return output.toString().strip();
    }

    private static void appendArguments(StringBuilder output, List<String> arguments) {
        for (String argument : arguments) {
            appendSeparator(output);
            output.append(AliasTokenizer.escape(argument));
        }
    }

    private static void appendSeparator(StringBuilder output) {
        if (!output.isEmpty() && !Character.isWhitespace(output.charAt(output.length() - 1))) {
            output.append(' ');
        }
    }

    private static void checkLength(String output) {
        if (output.length() > MAX_OUTPUT_LENGTH) {
            throw new AliasException(AliasError.OUTPUT_TOO_LONG, "Expanded command exceeds 256 characters");
        }
    }
}
