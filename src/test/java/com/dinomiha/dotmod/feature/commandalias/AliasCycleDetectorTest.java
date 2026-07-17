package com.dinomiha.dotmod.feature.commandalias;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliasCycleDetectorTest {
    private final AliasCycleDetector detector = new AliasCycleDetector();

    @Test
    void findsDirectAndIndirectCycles() {
        assertEquals(Set.of("self"), detector.findCycles(List.of(alias("self", "self"))));
        assertEquals(Set.of("a", "b", "c"), detector.findCycles(List.of(
                alias("a", "b"), alias("b", "c value"), alias("c", "a")
        )));
    }

    @Test
    void disabledAliasBreaksCycle() {
        assertTrue(detector.findCycles(List.of(
                alias("a", "b"), new CommandAlias("b", "a", false)
        )).isEmpty());
    }

    private static CommandAlias alias(String name, String template) {
        return new CommandAlias(name, template, true);
    }
}
