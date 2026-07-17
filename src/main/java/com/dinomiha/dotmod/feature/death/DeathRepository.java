package com.dinomiha.dotmod.feature.death;

import com.dinomiha.dotmod.feature.death.model.DeathRecord;
import com.dinomiha.dotmod.feature.death.model.DeathScreenshot;
import com.dinomiha.dotmod.feature.death.model.DeathSnapshot;
import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventoryDocument;
import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventorySerializer;
import com.dinomiha.dotmod.storage.AtomicJsonStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.registry.RegistryWrapper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Versioned one-file-per-death storage. Malformed records are isolated during scans. */
public final class DeathRepository {
    private static final Object PROCESS_LOCK = new Object();
    private static final Pattern RECORD_FILE = Pattern.compile("^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\.json$");

    private final Path root;
    private final Path trash;
    private final Gson gson;
    private final RegistryWrapper.WrapperLookup registries;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;
    private final VirtualInventorySerializer inventorySerializer = new VirtualInventorySerializer();

    public DeathRepository(Path root, RegistryWrapper.WrapperLookup registries) {
        this(root, new GsonBuilder().setPrettyPrinting().create(), registries, Clock.systemUTC(), UUID::randomUUID);
    }

    public DeathRepository(
            Path root,
            Gson gson,
            RegistryWrapper.WrapperLookup registries,
            Clock clock,
            Supplier<UUID> uuidSupplier
    ) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        this.trash = this.root.resolve("trash");
        this.gson = Objects.requireNonNull(gson, "gson");
        this.registries = Objects.requireNonNull(registries, "registries");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.uuidSupplier = Objects.requireNonNull(uuidSupplier, "uuidSupplier");
    }

    public DeathRecord create(DeathSnapshot snapshot) {
        return locked(() -> {
            UUID id = allocateId();
            DeathRecord record = new DeathRecord(id, clock.instant(), snapshot, DeathScreenshot.pending());
            write(record, true);
            return record;
        });
    }

    public List<DeathRecord> list() {
        return locked(this::scan);
    }

    public Optional<DeathRecord> get(UUID id) {
        Objects.requireNonNull(id, "id");
        return locked(() -> readRecord(recordPath(id), id));
    }

    public Optional<DeathRecord> get(String idOrPrefix) {
        String prefix = Objects.requireNonNull(idOrPrefix, "idOrPrefix").strip().toLowerCase(Locale.ROOT);
        if (prefix.isEmpty() || !prefix.matches("[0-9a-f-]+")) {
            throw new DeathException(DeathError.INVALID_DATA, "Invalid death id or prefix");
        }
        List<DeathRecord> matches = list().stream()
                .filter(record -> record.id().toString().startsWith(prefix))
                .toList();
        if (matches.size() > 1) {
            throw new DeathException(DeathError.AMBIGUOUS_ID, "Death id prefix is ambiguous");
        }
        return matches.stream().findFirst();
    }

    public DeathRecord updateScreenshot(UUID id, DeathScreenshot screenshot) {
        Objects.requireNonNull(screenshot, "screenshot");
        return locked(() -> {
            DeathRecord current = requireWritable(id);
            if (screenshot.relativePath() != null) {
                ensureSafeRelativePath(Path.of(screenshot.relativePath()));
            }
            DeathRecord updated = current.withScreenshot(screenshot);
            write(updated, false);
            return updated;
        });
    }

    public DeathRecord markScreenshotSaved(UUID id, Path relativePath) {
        ensureSafeRelativePath(relativePath);
        return updateScreenshot(id, DeathScreenshot.saved(relativePath));
    }

    public DeathRecord markScreenshotFailed(UUID id, String error) {
        return updateScreenshot(id, DeathScreenshot.failed(error));
    }

    public void deleteToTrash(UUID id) {
        locked(() -> {
            moveRecordArtifacts(requireWritable(id));
            return null;
        });
    }

    public int clearToTrash() {
        return locked(() -> {
            List<DeathRecord> records = scan();
            for (DeathRecord record : records) {
                requireWritable(record.id());
            }
            int moved = 0;
            for (DeathRecord record : records) {
                moveRecordArtifacts(record);
                moved++;
            }
            return moved;
        });
    }

    private List<DeathRecord> scan() throws IOException {
        Files.createDirectories(root);
        List<DeathRecord> records = new ArrayList<>();
        try (var files = Files.list(root)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                Matcher matcher = RECORD_FILE.matcher(path.getFileName().toString());
                if (!matcher.matches()) {
                    return;
                }
                UUID id = UUID.fromString(matcher.group(1));
                readRecord(path, id).ifPresent(records::add);
            });
        }
        records.sort(Comparator.comparing(DeathRecord::diedAt).reversed().thenComparing(record -> record.id().toString()));
        return List.copyOf(records);
    }

    private Optional<DeathRecord> readRecord(Path path, UUID expectedId) {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            DeathDocument document = gson.fromJson(reader, DeathDocument.class);
            if (document == null || document.schemaVersion > DeathDocument.CURRENT_SCHEMA_VERSION) {
                return Optional.empty();
            }
            DeathRecord record = document.toRecord(inventorySerializer, registries);
            return record.id().equals(expectedId) ? Optional.of(record) : Optional.empty();
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private DeathRecord requireWritable(UUID id) {
        Path path = recordPath(id);
        if (usesFutureSchema(path) || usesFutureSchema(path.resolveSibling(path.getFileName() + ".bak"))) {
            throw new DeathException(DeathError.READ_ONLY, "Death record uses a newer schema");
        }
        return readRecord(path, id).orElseThrow(() -> new DeathException(DeathError.NOT_FOUND, "Death record not found"));
    }

    private boolean usesFutureSchema(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            DeathDocument document = gson.fromJson(reader, DeathDocument.class);
            return document != null && (document.schemaVersion > DeathDocument.CURRENT_SCHEMA_VERSION
                    || document.inventory != null && document.inventory.schemaVersion > VirtualInventoryDocument.CURRENT_SCHEMA_VERSION);
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    private UUID allocateId() {
        for (int attempt = 0; attempt < 100; attempt++) {
            UUID id = Objects.requireNonNull(uuidSupplier.get(), "generated UUID");
            Path path = recordPath(id);
            if (!Files.exists(path) && !Files.exists(path.resolveSibling(path.getFileName() + ".bak"))) {
                return id;
            }
        }
        throw new DeathException(DeathError.IO_FAILURE, "Could not allocate death UUID");
    }

    private void write(DeathRecord record, boolean create) {
        Path path = recordPath(record.id());
        if (create && (Files.exists(path) || Files.exists(path.resolveSibling(path.getFileName() + ".bak")))) {
            throw new DeathException(DeathError.READ_ONLY, "Death record file already exists");
        }
        if (!create && (usesFutureSchema(path) || usesFutureSchema(path.resolveSibling(path.getFileName() + ".bak")))) {
            throw new DeathException(DeathError.READ_ONLY, "Death record uses a newer schema");
        }
        DeathDocument document = DeathDocument.fromRecord(record, inventorySerializer, registries);
        AtomicJsonStore<DeathDocument> store = new AtomicJsonStore<>(
                gson, path, DeathDocument.class, DeathDocument::new,
                value -> value.toRecord(inventorySerializer, registries)
        );
        if (!store.save(document)) {
            throw new DeathException(DeathError.IO_FAILURE, "Could not save death record");
        }
    }

    private void moveRecordArtifacts(DeathRecord record) throws IOException {
        Files.createDirectories(trash);
        UUID id = record.id();
        Path source = recordPath(id);
        Path target = uniqueTrashPath(id);
        if (record.screenshot().relativePath() != null) {
            Path image = root.resolve(record.screenshot().relativePath()).normalize();
            ensureSafeRelativePath(Path.of(record.screenshot().relativePath()));
            if (Files.isRegularFile(image) && !Files.isSymbolicLink(image)) {
                move(image, target.resolveSibling(target.getFileName() + ".png"));
            }
        }
        move(source, target);
        Path backup = source.resolveSibling(source.getFileName() + ".bak");
        if (Files.exists(backup)) {
            move(backup, target.resolveSibling(target.getFileName() + ".bak"));
        }
    }

    private Path uniqueTrashPath(UUID id) {
        String base = id + "-" + clock.millis();
        Path candidate = trash.resolve(base + ".json");
        int suffix = 1;
        while (Files.exists(candidate) || Files.exists(candidate.resolveSibling(candidate.getFileName() + ".bak"))) {
            candidate = trash.resolve(base + "-" + suffix++ + ".json");
        }
        return candidate;
    }

    private Path recordPath(UUID id) {
        Path path = root.resolve(id.toString() + ".json").normalize();
        if (!path.getParent().equals(root)) {
            throw new DeathException(DeathError.INVALID_DATA, "Unsafe death record path");
        }
        return path;
    }

    private void ensureSafeRelativePath(Path relative) {
        Objects.requireNonNull(relative, "relative");
        if (relative.isAbsolute() || relative.normalize().startsWith("..")) {
            throw new DeathException(DeathError.INVALID_DATA, "Screenshot path is outside the repository root");
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new DeathException(DeathError.INVALID_DATA, "Screenshot path is outside the repository root");
        }
        try {
            Files.createDirectories(root);
            Path realRoot = root.toRealPath();
            Path existing = target;
            while (existing != null && !Files.exists(existing)) {
                existing = existing.getParent();
            }
            if (existing == null || !existing.toRealPath().startsWith(realRoot)) {
                throw new DeathException(DeathError.INVALID_DATA, "Screenshot path crosses outside the repository root");
            }
        } catch (IOException exception) {
            throw new DeathException(DeathError.IO_FAILURE, "Could not validate screenshot path", exception);
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target);
        }
    }

    private <T> T locked(IoSupplier<T> operation) {
        synchronized (PROCESS_LOCK) {
            try {
                Files.createDirectories(root);
                return operation.get();
            } catch (DeathException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new DeathException(DeathError.IO_FAILURE, "Death repository operation failed", exception);
            }
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws Exception;
    }
}
