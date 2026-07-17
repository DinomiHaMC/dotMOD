package com.dinomiha.dotmod.storage;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicJsonStoreTest {
    @TempDir
    Path tempDirectory;

    @Test
    void createsAndAtomicallyReplacesJson() throws Exception {
        Path path = tempDirectory.resolve("nested/config.json");
        AtomicJsonStore<TestData> store = store(path);

        AtomicJsonStore.LoadResult<TestData> initial = store.load();
        assertTrue(initial.created());
        assertEquals(4, initial.value().value);

        initial.value().value = 12;
        assertTrue(store.save(initial.value()));
        assertEquals(12, store.load().value().value);
        assertFalse(Files.exists(path.resolveSibling("config.json.tmp")));
        assertTrue(Files.exists(path.resolveSibling("config.json.bak")));
    }

    @Test
    void preservesCorruptJsonAndRestoresDefaults() throws Exception {
        Path path = tempDirectory.resolve("config.json");
        Files.writeString(path, "{ broken");
        AtomicJsonStore<TestData> store = store(path);

        AtomicJsonStore.LoadResult<TestData> result = store.load();

        assertTrue(result.recovered());
        assertEquals(4, result.value().value);
        assertTrue(Files.exists(path.resolveSibling("config.json.broken")));
        assertEquals(4, store.load().value().value);
    }

    @Test
    void restoresLastValidBackupBeforeUsingDefaults() throws Exception {
        Path path = tempDirectory.resolve("config.json");
        AtomicJsonStore<TestData> store = store(path);
        TestData original = store.load().value();
        original.value = 8;
        assertTrue(store.save(original));
        Files.writeString(path, "{ broken");

        AtomicJsonStore.LoadResult<TestData> result = store.load();

        assertTrue(result.recovered());
        assertEquals(4, result.value().value);
        assertEquals(4, store.load().value().value);
        assertTrue(Files.exists(path.resolveSibling("config.json.broken")));
    }

    private static AtomicJsonStore<TestData> store(Path path) {
        return new AtomicJsonStore<>(
                new GsonBuilder().setPrettyPrinting().create(),
                path,
                TestData.class,
                TestData::new,
                value -> value.value = Math.max(0, value.value)
        );
    }

    static final class TestData {
        int value = 4;
    }
}
