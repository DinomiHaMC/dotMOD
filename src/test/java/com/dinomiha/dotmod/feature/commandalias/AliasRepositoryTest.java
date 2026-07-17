package com.dinomiha.dotmod.feature.commandalias;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliasRepositoryTest {
    @TempDir
    Path tempDirectory;

    @Test
    void loadsSavesListsUpsertsRemovesAndReloads() {
        Path path = tempDirectory.resolve("command-aliases.json");
        AliasRepository repository = repository(path, Set.of());
        assertTrue(repository.load().created());

        repository.upsert(new CommandAlias("Home", "server-home", true));
        repository.upsert(new CommandAlias("home", "spawn", false));
        assertEquals(1, repository.list().size());
        assertEquals("spawn", repository.list().getFirst().template());

        AliasRepository reloaded = repository(path, Set.of());
        assertEquals(repository.list(), reloaded.reload().aliases());
        assertTrue(reloaded.remove("HOME"));
        assertFalse(reloaded.remove("home"));
        reloaded.save();
    }

    @Test
    void rejectsSuppliedAndDotmodConflictsAndCycles() {
        AliasRepository repository = repository(tempDirectory.resolve("aliases.json"), Set.of("tp"));
        assertError(AliasError.COMMAND_CONFLICT,
                () -> repository.upsert(new CommandAlias("TP", "teleport", true)));
        assertError(AliasError.COMMAND_CONFLICT,
                () -> repository.upsert(new CommandAlias("dot", "say nope", true)));
        assertError(AliasError.COMMAND_CONFLICT,
                () -> repository.upsert(new CommandAlias("dotmod", "say nope", true)));

        repository.upsert(new CommandAlias("a", "b", true));
        assertError(AliasError.CYCLE, () -> repository.upsert(new CommandAlias("b", "a", true)));
        repository.upsert(new CommandAlias("b", "a", false));
    }

    @Test
    void futureSchemaIsWriteBlockedAndNeverOverwritten() throws Exception {
        Path path = tempDirectory.resolve("aliases.json");
        String future = "{\"schemaVersion\":999,\"aliases\":[]}";
        Files.writeString(path, future);

        AliasRepository repository = repository(path, Set.of());

        assertTrue(repository.load().writeBlocked());
        assertTrue(repository.writeBlocked());
        assertError(AliasError.READ_ONLY,
                () -> repository.upsert(new CommandAlias("safe", "say safe", true)));
        assertEquals(future, Files.readString(path));
    }

    @Test
    void futureBackupAlsoBlocksWrites() throws Exception {
        Path path = tempDirectory.resolve("aliases.json");
        AliasRepository repository = repository(path, Set.of());
        repository.upsert(new CommandAlias("safe", "say safe", true));
        String future = "{\"schemaVersion\":999,\"aliases\":[]}";
        Files.writeString(path.resolveSibling("aliases.json.bak"), future);

        AliasRepository guarded = repository(path, Set.of());

        assertTrue(guarded.load().writeBlocked());
        assertError(AliasError.READ_ONLY,
                () -> guarded.upsert(new CommandAlias("other", "say other", true)));
        assertEquals(future, Files.readString(path.resolveSibling("aliases.json.bak")));
    }

    private static AliasRepository repository(Path path, Set<String> conflicts) {
        return new AliasRepository(path, new GsonBuilder().setPrettyPrinting().create(), conflicts);
    }

    private static void assertError(AliasError expected, Runnable operation) {
        AliasException exception = assertThrows(AliasException.class, operation::run);
        assertEquals(expected, exception.error());
    }
}
