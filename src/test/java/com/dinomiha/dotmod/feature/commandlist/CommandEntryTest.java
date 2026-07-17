package com.dinomiha.dotmod.feature.commandlist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandEntryTest {
    @Test
    void normalizesCommandsForDisplay() {
        assertEquals("/say hello", new CommandEntry("  say hello  ").command());
        assertEquals("/say hello", new CommandEntry(" ///  say hello  ").command());
    }

    @Test
    void rejectsEmptyAndOversizedCommands() {
        assertThrows(IllegalArgumentException.class, () -> new CommandEntry(" / / "));
        assertEquals(256, new CommandEntry("x".repeat(255)).command().length());
        assertThrows(IllegalArgumentException.class, () -> new CommandEntry("x".repeat(256)));
    }
}
