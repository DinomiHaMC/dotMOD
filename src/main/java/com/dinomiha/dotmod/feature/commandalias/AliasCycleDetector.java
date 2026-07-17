package com.dinomiha.dotmod.feature.commandalias;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AliasCycleDetector {
    public Set<String> findCycles(Collection<CommandAlias> aliases) {
        Map<String, CommandAlias> enabled = new HashMap<>();
        for (CommandAlias alias : aliases) {
            if (alias.enabled()) {
                enabled.put(alias.name(), alias);
            }
        }
        Set<String> cycles = new LinkedHashSet<>();
        Set<String> complete = new HashSet<>();
        for (String name : enabled.keySet()) {
            visit(name, enabled, new LinkedHashSet<>(), complete, cycles);
        }
        return Set.copyOf(cycles);
    }

    public boolean hasCycle(Collection<CommandAlias> aliases) {
        return !findCycles(aliases).isEmpty();
    }

    private static void visit(
            String name,
            Map<String, CommandAlias> aliases,
            LinkedHashSet<String> path,
            Set<String> complete,
            Set<String> cycles
    ) {
        if (complete.contains(name) || !aliases.containsKey(name)) {
            return;
        }
        if (!path.add(name)) {
            boolean inCycle = false;
            for (String entry : path) {
                inCycle |= entry.equals(name);
                if (inCycle) {
                    cycles.add(entry);
                }
            }
            return;
        }
        List<String> template = AliasTokenizer.tokenize(aliases.get(name).template());
        if (!template.isEmpty()) {
            visit(root(template.getFirst()), aliases, path, complete, cycles);
        }
        path.remove(name);
        complete.add(name);
    }

    private static String root(String value) {
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
