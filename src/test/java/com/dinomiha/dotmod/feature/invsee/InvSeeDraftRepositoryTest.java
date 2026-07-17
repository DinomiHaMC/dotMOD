package com.dinomiha.dotmod.feature.invsee;

import com.dinomiha.dotmod.feature.invsee.persistence.InvSeeDraftRepository;
import com.google.gson.GsonBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeDraftRepositoryTest {
    @TempDir
    Path tempDirectory;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void savesBacksUpAndRecoversStrictDrafts() throws Exception {
        Path path = tempDirectory.resolve("invsee-draft.json");
        InvSeeDraftRepository repository = repository(path);
        assertTrue(repository.load().snapshot().isEmpty());
        assertFalse(Files.exists(path));

        assertTrue(repository.save(snapshot(3)));
        assertTrue(repository.save(snapshot(5)));
        assertTrue(Files.exists(path.resolveSibling("invsee-draft.json.bak")));

        Files.writeString(path, "{ broken");
        InvSeeDraftRepository.LoadOutcome recovered = repository(path).load();
        assertTrue(recovered.recovered());
        assertEquals(3, recovered.snapshot().orElseThrow().getStack(0).getCount());
        assertTrue(Files.exists(path.resolveSibling("invsee-draft.json.broken")));
    }

    @Test
    void newerDraftIsReadOnlyAndRemainsUntouched() throws Exception {
        Path path = tempDirectory.resolve("invsee-draft.json");
        Files.writeString(path, "{\"schemaVersion\":999,\"slots\":[]}");
        InvSeeDraftRepository repository = repository(path);

        InvSeeDraftRepository.LoadOutcome outcome = repository.load();

        assertTrue(outcome.writeBlocked());
        assertFalse(repository.save(snapshot(1)));
        assertEquals("{\"schemaVersion\":999,\"slots\":[]}", Files.readString(path));
    }

    @Test
    void externalReplacementAfterLoadIsNeverOverwritten() throws Exception {
        Path path = tempDirectory.resolve("invsee-draft.json");
        InvSeeDraftRepository repository = repository(path);
        repository.load();
        String future = "{\"schemaVersion\":999,\"slots\":[]}";
        Files.writeString(path, future);

        assertFalse(repository.save(snapshot(2)));
        assertEquals(future, Files.readString(path));
    }

    @Test
    void backupWithoutPrimaryIsRecoveredAndNewerBackupBlocksWrites() throws Exception {
        Path path = tempDirectory.resolve("invsee-draft.json");
        Path backup = path.resolveSibling("invsee-draft.json.bak");
        InvSeeDraftRepository repository = repository(path);
        assertTrue(repository.save(snapshot(6)));
        Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);

        InvSeeDraftRepository.LoadOutcome restored = repository(path).load();
        assertTrue(restored.recovered());
        assertEquals(6, restored.snapshot().orElseThrow().getStack(0).getCount());

        Files.delete(path);
        Files.writeString(backup, "{\"schemaVersion\":999,\"slots\":[]}");
        InvSeeDraftRepository futureBackup = repository(path);
        assertTrue(futureBackup.load().writeBlocked());
        assertFalse(futureBackup.save(snapshot(1)));
        assertFalse(Files.exists(path));
    }

    @Test
    void registryDecodeFailureLeavesPrimaryUntouched() throws Exception {
        Path path = tempDirectory.resolve("invsee-draft.json");
        String unknown = "{\"schemaVersion\":1,\"slots\":[{\"index\":0,\"stack\":{\"id\":\"dotmod_test:missing\"}}]}";
        Files.writeString(path, unknown);

        InvSeeDraftRepository.LoadOutcome outcome = repository(path).load();

        assertTrue(outcome.decodeFailed());
        assertTrue(outcome.writeBlocked());
        assertEquals(unknown, Files.readString(path));
        assertFalse(Files.exists(path.resolveSibling("invsee-draft.json.broken")));
    }

    @Test
    void nullSlotsArrayRecoversValidBackup() throws Exception {
        Path path = tempDirectory.resolve("invsee-draft.json");
        InvSeeDraftRepository repository = repository(path);
        assertTrue(repository.save(snapshot(3)));
        assertTrue(repository.save(snapshot(5)));
        Files.writeString(path, "{\"schemaVersion\":1,\"slots\":null}");

        InvSeeDraftRepository.LoadOutcome outcome = repository(path).load();

        assertTrue(outcome.recovered());
        assertEquals(3, outcome.snapshot().orElseThrow().getStack(0).getCount());
    }

    @Test
    void staleRepositoryCannotOverwriteAnotherRepositorySave() {
        Path path = tempDirectory.resolve("invsee-draft.json");
        assertTrue(repository(path).save(snapshot(2)));
        InvSeeDraftRepository first = repository(path);
        InvSeeDraftRepository stale = repository(path);
        first.load();
        stale.load();

        assertTrue(first.save(snapshot(3)));
        assertFalse(stale.save(snapshot(4)));
        assertEquals(3, repository(path).load().snapshot().orElseThrow().getStack(0).getCount());
    }

    @Test
    void validPrimaryNeverOverwritesNewerBackup() throws Exception {
        Path path = tempDirectory.resolve("invsee-draft.json");
        Path backup = path.resolveSibling("invsee-draft.json.bak");
        InvSeeDraftRepository repository = repository(path);
        assertTrue(repository.save(snapshot(2)));
        assertTrue(repository.save(snapshot(3)));
        String future = "{\"schemaVersion\":999,\"slots\":[]}";
        Files.writeString(backup, future);

        InvSeeDraftRepository guarded = repository(path);
        assertTrue(guarded.load().writeBlocked());
        assertFalse(guarded.save(snapshot(4)));
        assertEquals(future, Files.readString(backup));
    }

    private static InvSeeDraftRepository repository(Path path) {
        return new InvSeeDraftRepository(
                path,
                new GsonBuilder().setPrettyPrinting().create(),
                MinecraftTestBootstrap.registries()
        );
    }

    private static VirtualInventorySnapshot snapshot(int amount) {
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        stacks.set(0, new ItemStack(Items.STONE, amount));
        return new VirtualInventorySnapshot(stacks);
    }
}
