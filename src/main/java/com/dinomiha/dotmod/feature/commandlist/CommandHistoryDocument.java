package com.dinomiha.dotmod.feature.commandlist;

import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CommandHistoryDocument {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_ENTRIES_PER_LIST = 500;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public List<CommandEntry> recent = new ArrayList<>();
    public List<CommandEntry> pinned = new ArrayList<>();

    public void validate() {
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedDataVersionException("Unsupported command history schema " + schemaVersion);
        }
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported command history schema " + schemaVersion);
        }
        if (recent == null || pinned == null) {
            throw new IllegalArgumentException("Command history lists are missing");
        }
        if (recent.size() > MAX_ENTRIES_PER_LIST || pinned.size() > MAX_ENTRIES_PER_LIST) {
            throw new IllegalArgumentException("Command history contains too many entries");
        }
        Set<String> commands = new HashSet<>();
        validateEntries(recent, commands);
        validateEntries(pinned, commands);
    }

    private static void validateEntries(List<CommandEntry> entries, Set<String> commands) {
        for (CommandEntry entry : entries) {
            if (entry == null || entry.command() == null) {
                throw new IllegalArgumentException("Invalid command history entry");
            }
            String normalized = CommandEntry.normalize(entry.command());
            if (!normalized.equals(entry.command())) {
                throw new IllegalArgumentException("Command history entry is not normalized");
            }
            if (SensitiveCommandFilter.isSensitive(normalized)) {
                throw new IllegalArgumentException("Sensitive commands cannot be persisted");
            }
            if (!commands.add(normalized)) {
                throw new IllegalArgumentException("Duplicate command history entry");
            }
        }
    }
}
