package com.dinomiha.dotmod.feature.commandlist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHistoryServiceTest {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @TempDir
    Path tempDirectory;

    @Test
    void recordsMruCommandsWithGlobalDedupeAndLimit() {
        CommandHistoryService service = service(2);

        assertTrue(service.record("home"));
        assertTrue(service.record("/spawn"));
        assertTrue(service.record(" home "));
        assertEquals(List.of("/home", "/spawn"), commands(service.recent()));

        assertTrue(service.record("warp mine"));
        assertEquals(List.of("/warp mine", "/home"), commands(service.recent()));
    }

    @Test
    void retainsPinsSeparatelyFromBoundedRecentHistory() {
        CommandHistoryService service = service(1);
        service.record("home");
        service.record("spawn");

        assertTrue(service.pin("home"));
        assertEquals(List.of("/spawn"), commands(service.recent()));
        assertEquals(List.of("/home"), commands(service.pinned()));

        assertTrue(service.record("home"));
        assertEquals(List.of("/spawn"), commands(service.recent()));
        assertEquals(List.of("/home"), commands(service.pinned()));
        assertTrue(service.clearRecent());
        assertTrue(service.recent().isEmpty());
        assertEquals(List.of("/home"), commands(service.pinned()));
    }

    @Test
    void zeroLimitStillPersistsPinsAndLimitsAreClamped() {
        CommandHistoryService zero = service(0);
        assertTrue(zero.record("home"));
        assertTrue(zero.recent().isEmpty());
        assertTrue(zero.pin("spawn"));
        assertEquals(List.of("/spawn"), commands(zero.pinned()));
        assertEquals(0, zero.historyLimit());

        assertEquals(0, new CommandHistoryService(tempDirectory.resolve("negative.json"), gson, -2).historyLimit());
        assertEquals(500, new CommandHistoryService(tempDirectory.resolve("large.json"), gson, 900).historyLimit());
    }

    @Test
    void neverRetainsOrPersistsSensitiveCommands() throws Exception {
        Path path = tempDirectory.resolve("history.json");
        CommandHistoryService service = new CommandHistoryService(path, gson, 20);
        service.record("say hello");

        for (String command : List.of("login secret", " / REGISTER secret", "///\tOtp 123456")) {
            assertFalse(service.record(command));
            assertFalse(service.pin(command));
        }

        assertEquals(List.of("/say hello"), commands(service.recent()));
        String persisted = Files.readString(path).toLowerCase();
        assertFalse(persisted.contains("secret"));
        assertFalse(persisted.contains("123456"));
    }

    @Test
    void persistsRecentAndPinnedEntriesAcrossInstances() {
        Path path = tempDirectory.resolve("history.json");
        CommandHistoryService first = new CommandHistoryService(path, gson, 20);
        first.record("home");
        first.record("spawn");
        first.pin("home");

        CommandHistoryService second = new CommandHistoryService(path, gson, 20);

        assertEquals(List.of("/spawn"), commands(second.recent()));
        assertEquals(List.of("/home"), commands(second.pinned()));
    }

    @Test
    void reloadReadsExternalChangesAndAppliesInjectedLimit() throws Exception {
        Path path = tempDirectory.resolve("history.json");
        CommandHistoryDocument document = new CommandHistoryDocument();
        document.recent = List.of(new CommandEntry("one"), new CommandEntry("two"), new CommandEntry("three"));
        Files.writeString(path, gson.toJson(document));
        CommandHistoryService service = new CommandHistoryService(path, gson, 2);

        assertEquals(List.of("/one", "/two"), commands(service.recent()));
        assertEquals(2, gson.fromJson(Files.readString(path), CommandHistoryDocument.class).recent.size());

        CommandHistoryDocument replacement = new CommandHistoryDocument();
        replacement.recent = List.of(new CommandEntry("external"));
        Files.writeString(path, gson.toJson(replacement));
        service.reload();
        assertEquals(List.of("/external"), commands(service.recent()));
    }

    @Test
    void futurePrimaryBlocksWritesAndRemainsUntouchedAcrossReload() throws Exception {
        Path path = tempDirectory.resolve("history.json");
        String future = "{\"schemaVersion\":999,\"recent\":[],\"pinned\":[],\"futureData\":true}";
        Files.writeString(path, future);

        CommandHistoryService service = new CommandHistoryService(path, gson, 20);

        assertTrue(service.writeBlocked());
        assertFalse(service.record("home"));
        assertFalse(service.pin("spawn"));
        assertEquals(future, Files.readString(path));
        assertTrue(service.reload().writeBlocked());
    }

    @Test
    void futureBackupBlocksWritesWithoutReplacingValidPrimary() throws Exception {
        Path path = tempDirectory.resolve("history.json");
        CommandHistoryService service = new CommandHistoryService(path, gson, 20);
        assertTrue(service.record("home"));
        String primary = Files.readString(path);
        String future = "{\"schemaVersion\":999,\"recent\":[],\"pinned\":[]}";
        Files.writeString(path.resolveSibling("history.json.bak"), future);

        CommandHistoryService guarded = new CommandHistoryService(path, gson, 20);

        assertTrue(guarded.writeBlocked());
        assertFalse(guarded.record("spawn"));
        assertEquals(primary, Files.readString(path));
        assertEquals(future, Files.readString(path.resolveSibling("history.json.bak")));
    }

    @Test
    void futureReplacementAfterLoadIsBlockedBeforeSave() throws Exception {
        Path path = tempDirectory.resolve("history.json");
        CommandHistoryService service = new CommandHistoryService(path, gson, 20);
        assertTrue(service.record("home"));
        String future = "{\"schemaVersion\":999,\"recent\":[],\"pinned\":[]}";
        Files.writeString(path, future);

        assertFalse(service.record("spawn"));
        assertTrue(service.writeBlocked());
        assertEquals(future, Files.readString(path));
    }

    @Test
    void boundsPinnedEntries() {
        CommandHistoryService service = service(100);
        for (int index = 0; index < CommandHistoryService.MAX_PINNED + 10; index++) {
            assertTrue(service.pin("/say " + index));
        }

        assertEquals(CommandHistoryService.MAX_PINNED, service.pinned().size());
        assertEquals("/say 109", service.pinned().getFirst().command());
        assertEquals("/say 10", service.pinned().getLast().command());
    }

    private CommandHistoryService service(int limit) {
        return new CommandHistoryService(tempDirectory.resolve("history-" + limit + ".json"), gson, limit);
    }

    private static List<String> commands(List<CommandEntry> entries) {
        return entries.stream().map(CommandEntry::command).toList();
    }
}
