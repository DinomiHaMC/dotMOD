package com.dinomiha.dotmod.feature.death;

import com.dinomiha.dotmod.feature.death.model.DeathEffect;
import com.dinomiha.dotmod.feature.death.model.DeathRecord;
import com.dinomiha.dotmod.feature.death.model.DeathSnapshot;
import com.dinomiha.dotmod.feature.death.model.ScreenshotStatus;
import com.dinomiha.dotmod.feature.invsee.MinecraftTestBootstrap;
import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import com.google.gson.GsonBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
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

class DeathRepositoryTest {
    private static final UUID FIRST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SECOND_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID PLAYER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @TempDir
    Path tempDirectory;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void roundTripsCompleteSnapshotAndSupportsUuidAndUniquePrefix() {
        DeathRepository repository = repository(FIRST_ID);
        DeathRecord created = repository.create(snapshot());

        DeathRecord restored = repository.get(FIRST_ID).orElseThrow();
        assertEquals(FIRST_ID, created.id());
        assertEquals(Instant.parse("2026-02-03T04:05:06Z"), restored.diedAt());
        assertEquals(PLAYER_ID, restored.snapshot().playerUuid());
        assertEquals("minecraft:the_nether", restored.snapshot().dimension());
        assertEquals(-14.25, restored.snapshot().x());
        assertEquals(-15, restored.snapshot().blockX());
        assertEquals("Alex was slain by Zombie", restored.snapshot().deathMessage());
        assertEquals("minecraft:mob_attack", restored.snapshot().damageType());
        assertEquals("Zombie", restored.snapshot().attackerName());
        assertEquals(1234, restored.snapshot().totalExperience());
        assertEquals(1, restored.snapshot().effects().size());
        assertEquals("Speed", restored.snapshot().effects().getFirst().name());
        assertEquals("Named sword", restored.snapshot().inventory().getStack(3).getName().getString());
        assertEquals(ScreenshotStatus.PENDING, restored.screenshot().status());
        assertEquals(FIRST_ID, repository.get("10000000-0000").orElseThrow().id());
        assertEquals(FIRST_ID, repository.get(FIRST_ID.toString()).orElseThrow().id());
        assertTrue(Files.exists(tempDirectory.resolve(FIRST_ID + ".json")));
    }

    @Test
    void screenshotFailureRetainsRecordAndSavedPathsRemainRelative() {
        DeathRepository repository = repository(FIRST_ID);
        repository.create(snapshot());

        DeathRecord failed = repository.markScreenshotFailed(FIRST_ID, "Framebuffer unavailable");
        assertEquals(ScreenshotStatus.FAILED, failed.screenshot().status());
        assertEquals("Framebuffer unavailable", repository.get(FIRST_ID).orElseThrow().screenshot().error());
        assertEquals(1, repository.list().size());

        DeathRecord saved = repository.markScreenshotSaved(FIRST_ID, Path.of("screenshots", "death.png"));
        assertEquals("screenshots/death.png", saved.screenshot().relativePath());
        assertEquals(ScreenshotStatus.SAVED, repository.get(FIRST_ID).orElseThrow().screenshot().status());
    }

    @Test
    void rejectsAbsoluteTraversalAndSymlinkEscapeScreenshotPaths() throws Exception {
        DeathRepository repository = repository(FIRST_ID);
        repository.create(snapshot());

        assertEquals(DeathError.INVALID_DATA, assertThrows(
                DeathException.class,
                () -> repository.markScreenshotSaved(FIRST_ID, tempDirectory.resolve("outside.png"))
        ).error());
        assertEquals(DeathError.INVALID_DATA, assertThrows(
                DeathException.class,
                () -> repository.markScreenshotSaved(FIRST_ID, Path.of("..", "outside.png"))
        ).error());

        Files.createSymbolicLink(tempDirectory.resolve("escape"), tempDirectory.getParent());
        assertEquals(DeathError.INVALID_DATA, assertThrows(
                DeathException.class,
                () -> repository.markScreenshotSaved(FIRST_ID, Path.of("escape", "outside.png"))
        ).error());
    }

    @Test
    void malformedRecordDoesNotPreventListingValidRecords() throws Exception {
        DeathRepository repository = repository(FIRST_ID);
        repository.create(snapshot());
        Files.writeString(tempDirectory.resolve(SECOND_ID + ".json"), "{not json");

        List<DeathRecord> records = repository.list();

        assertEquals(List.of(FIRST_ID), records.stream().map(DeathRecord::id).toList());
        assertEquals("{not json", Files.readString(tempDirectory.resolve(SECOND_ID + ".json")));
    }

    @Test
    void futureSchemaIsNotOverwrittenOrMovedByClear() throws Exception {
        DeathRepository repository = repository(FIRST_ID);
        repository.create(snapshot());
        String future = "{\"schemaVersion\":999,\"futureValue\":true}";
        Path futurePath = tempDirectory.resolve(SECOND_ID + ".json");
        Files.writeString(futurePath, future);

        assertEquals(1, repository.clearToTrash());
        assertEquals(future, Files.readString(futurePath));
        assertFalse(Files.exists(tempDirectory.resolve(FIRST_ID + ".json")));
        assertTrue(Files.list(tempDirectory.resolve("trash")).anyMatch(path -> path.getFileName().toString().startsWith(FIRST_ID.toString())));

        DeathException error = assertThrows(DeathException.class, () -> repository.markScreenshotFailed(SECOND_ID, "nope"));
        assertEquals(DeathError.READ_ONLY, error.error());
        assertEquals(future, Files.readString(futurePath));
    }

    @Test
    void detectsAmbiguousPrefixesAndMovesDeleteToGeneratedTrashName() {
        DeathRepository repository = repository(FIRST_ID, SECOND_ID);
        repository.create(snapshot());
        repository.create(snapshot());

        assertEquals(DeathError.AMBIGUOUS_ID, assertThrows(
                DeathException.class, () -> repository.get("10000000")
        ).error());

        repository.deleteToTrash(FIRST_ID);
        assertTrue(repository.get(FIRST_ID).isEmpty());
        assertEquals(List.of(SECOND_ID), repository.list().stream().map(DeathRecord::id).toList());
        assertTrue(Files.exists(tempDirectory.resolve("trash")));
    }

    @Test
    void deleteMovesSavedScreenshotWithItsRecord() throws Exception {
        DeathRepository repository = repository(FIRST_ID);
        repository.create(snapshot());
        Path image = tempDirectory.resolve("images").resolve(FIRST_ID + ".png");
        Files.createDirectories(image.getParent());
        Files.write(image, new byte[]{1, 2, 3});
        repository.markScreenshotSaved(FIRST_ID, Path.of("images", FIRST_ID + ".png"));

        repository.deleteToTrash(FIRST_ID);

        assertFalse(Files.exists(image));
        assertTrue(Files.list(tempDirectory.resolve("trash"))
                .anyMatch(path -> path.getFileName().toString().endsWith(".json.png")));
    }

    private DeathRepository repository(UUID... ids) {
        Queue<UUID> queue = new ArrayDeque<>(List.of(ids));
        return new DeathRepository(
                tempDirectory,
                new GsonBuilder().setPrettyPrinting().create(),
                MinecraftTestBootstrap.registries(),
                Clock.fixed(Instant.parse("2026-02-03T04:05:06Z"), ZoneOffset.UTC),
                queue::remove
        );
    }

    private static DeathSnapshot snapshot() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Named sword"));
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        stacks.set(3, sword);
        return new DeathSnapshot(
                PLAYER_ID,
                "Alex",
                "minecraft:the_nether",
                -14.25,
                64.5,
                20.75,
                -15,
                64,
                20,
                "Alex was slain by Zombie",
                "minecraft:mob_attack",
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                "Zombie",
                3,
                27,
                1234,
                0.5F,
                List.of(new DeathEffect("minecraft:speed", "Speed", 1, 200, false, true, true)),
                new VirtualInventorySnapshot(stacks)
        );
    }
}
