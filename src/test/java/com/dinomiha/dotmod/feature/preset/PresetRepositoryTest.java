package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.feature.invsee.MinecraftTestBootstrap;
import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetRepositoryTest {
    private static final UUID FIRST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @TempDir
    Path tempDirectory;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void performsCrudWithActiveStateAndRevisionChecks() {
        PresetRepository repository = repository(FIRST_ID, SECOND_ID);
        PresetRecord created = repository.create("Mining Kit", snapshot(Items.STONE, 3));
        assertEquals(FIRST_ID, created.preset().id());
        assertThrows(PresetException.class, () -> repository.create("MINING KIT", VirtualInventorySnapshot.empty()));

        repository.select(FIRST_ID);
        PresetRecord active = repository.active().orElseThrow();
        assertTrue(active.active());

        PresetRecord renamed = repository.rename(FIRST_ID, active.revision(), "Deep Mining");
        assertEquals("Deep Mining", renamed.preset().name());
        assertThrows(PresetException.class, () -> repository.updateInventory(FIRST_ID, active.revision(), snapshot(Items.DIRT, 2)));

        PresetRecord duplicate = repository.duplicate(FIRST_ID, renamed.revision(), "Backup Kit");
        assertEquals(SECOND_ID, duplicate.preset().id());
        assertEquals(3, duplicate.preset().inventory().getStack(0).getCount());
        repository.delete(FIRST_ID, repository.requireByName("Deep Mining").revision());

        assertFalse(repository.active().isPresent());
        assertEquals(List.of("Backup Kit"), repository.list().stream().map(record -> record.preset().name()).toList());
        assertTrue(Files.exists(tempDirectory.resolve("trash")));
    }

    @Test
    void discoversOrphanPresetWhenIndexLosesOrder() throws Exception {
        PresetRepository repository = repository(FIRST_ID);
        repository.create("Orphan", snapshot(Items.DIAMOND, 1));
        Files.writeString(tempDirectory.resolve("index.json"), "{\"schemaVersion\":1,\"order\":[]}");

        assertEquals("Orphan", repository.list().getFirst().preset().name());
    }

    @Test
    void futureIndexMakesRepositoryReadOnlyWithoutDeletingPreset() throws Exception {
        PresetRepository repository = repository(FIRST_ID, SECOND_ID);
        repository.create("Safe", snapshot(Items.STONE, 1));
        Path presetFile = tempDirectory.resolve(FIRST_ID + ".json");
        Files.writeString(tempDirectory.resolve("index.json"), "{\"schemaVersion\":999,\"order\":[]}");

        PresetException error = assertThrows(PresetException.class, () -> repository.create("Blocked", VirtualInventorySnapshot.empty()));
        assertEquals(PresetError.READ_ONLY, error.error());
        assertTrue(Files.exists(presetFile));
    }

    @Test
    void newerIndexBackupBlocksWrites() throws Exception {
        PresetRepository repository = repository(FIRST_ID, SECOND_ID);
        repository.create("Safe", snapshot(Items.STONE, 1));
        Files.writeString(tempDirectory.resolve("index.json.bak"), "{\"schemaVersion\":999,\"order\":[]}");

        PresetException error = assertThrows(PresetException.class, () -> repository.create("Blocked", VirtualInventorySnapshot.empty()));

        assertEquals(PresetError.READ_ONLY, error.error());
        assertTrue(Files.exists(tempDirectory.resolve(FIRST_ID + ".json")));
    }

    @Test
    void newerPresetBackupBlocksMutation() throws Exception {
        PresetRepository repository = repository(FIRST_ID);
        PresetRecord created = repository.create("Safe", snapshot(Items.STONE, 1));
        PresetRecord updated = repository.updateInventory(FIRST_ID, created.revision(), snapshot(Items.STONE, 2));
        Path backup = tempDirectory.resolve(FIRST_ID + ".json.bak");
        String future = "{\"schemaVersion\":999}";
        Files.writeString(backup, future);

        PresetException error = assertThrows(PresetException.class, () -> repository.rename(FIRST_ID, updated.revision(), "Blocked"));

        assertEquals(PresetError.READ_ONLY, error.error());
        assertEquals(future, Files.readString(backup));
    }

    @Test
    void newerNestedInventoryBackupBlocksMutation() throws Exception {
        PresetRepository repository = repository(FIRST_ID);
        PresetRecord created = repository.create("Safe", snapshot(Items.STONE, 1));
        PresetRecord updated = repository.updateInventory(FIRST_ID, created.revision(), snapshot(Items.STONE, 2));
        Path backup = tempDirectory.resolve(FIRST_ID + ".json.bak");
        JsonObject document = JsonParser.parseString(Files.readString(backup)).getAsJsonObject();
        document.getAsJsonObject("inventory").addProperty("schemaVersion", 999);
        Files.writeString(backup, new GsonBuilder().setPrettyPrinting().create().toJson(document));

        PresetException error = assertThrows(PresetException.class, () -> repository.rename(FIRST_ID, updated.revision(), "Blocked"));

        assertEquals(PresetError.READ_ONLY, error.error());
        assertEquals(999, JsonParser.parseString(Files.readString(backup)).getAsJsonObject()
                .getAsJsonObject("inventory").get("schemaVersion").getAsInt());
    }

    @Test
    void futureIndexDisablesBackupOnlyRecovery() throws Exception {
        PresetRepository repository = repository(FIRST_ID);
        repository.create("Safe", snapshot(Items.STONE, 1));
        Path primary = tempDirectory.resolve(FIRST_ID + ".json");
        Path backup = tempDirectory.resolve(FIRST_ID + ".json.bak");
        Files.move(primary, backup);
        Files.writeString(tempDirectory.resolve("index.json"), "{\"schemaVersion\":999,\"order\":[]}");

        repository.list();

        assertFalse(Files.exists(primary));
        assertTrue(Files.exists(backup));
    }

    @Test
    void futurePrimaryWithoutCurrentHeaderIsNeverQuarantined() throws Exception {
        Path preset = tempDirectory.resolve(FIRST_ID + ".json");
        String future = "{\"schemaVersion\":999,\"futureMetadata\":true}";
        Files.writeString(preset, future);

        PresetRepository repository = repository(SECOND_ID);

        assertTrue(repository.list().isEmpty());
        assertEquals(future, Files.readString(preset));
        assertFalse(Files.exists(tempDirectory.resolve(FIRST_ID + ".json.broken")));
    }

    private PresetRepository repository(UUID... ids) {
        Queue<UUID> queue = new ArrayDeque<>(List.of(ids));
        return new PresetRepository(
                tempDirectory,
                new GsonBuilder().setPrettyPrinting().create(),
                MinecraftTestBootstrap.registries(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                queue::remove
        );
    }

    private static VirtualInventorySnapshot snapshot(net.minecraft.item.Item item, int count) {
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        stacks.set(0, new ItemStack(item, count));
        return new VirtualInventorySnapshot(stacks);
    }
}
