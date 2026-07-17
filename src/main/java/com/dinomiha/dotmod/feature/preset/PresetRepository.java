package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventorySerializer;
import com.dinomiha.dotmod.storage.AtomicJsonStore;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;
import com.google.gson.Gson;
import net.minecraft.registry.RegistryWrapper;

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** UUID-addressed, one-file-per-preset repository with an atomic active/order index. */
public final class PresetRepository {
    private static final Object PROCESS_LOCK = new Object();
    private static final Pattern PRESET_FILE = Pattern.compile("^([0-9a-fA-F-]{36})\\.json$");
    private static final Pattern PRESET_BACKUP_FILE = Pattern.compile("^([0-9a-fA-F-]{36})\\.json\\.bak$");

    private final Path directory;
    private final Path indexPath;
    private final Path lockPath;
    private final Path trashDirectory;
    private final Gson gson;
    private final RegistryWrapper.WrapperLookup registries;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;
    private final VirtualInventorySerializer inventorySerializer = new VirtualInventorySerializer();
    private final AtomicJsonStore<PresetIndexDocument> indexStore;

    public PresetRepository(
            Path directory,
            Gson gson,
            RegistryWrapper.WrapperLookup registries,
            Clock clock,
            Supplier<UUID> uuidSupplier
    ) {
        this.directory = directory.toAbsolutePath().normalize();
        this.indexPath = this.directory.resolve("index.json");
        this.lockPath = this.directory.resolve(".lock");
        this.trashDirectory = this.directory.resolve("trash");
        this.gson = gson;
        this.registries = registries;
        this.clock = clock;
        this.uuidSupplier = uuidSupplier;
        this.indexStore = new AtomicJsonStore<>(
                gson,
                indexPath,
                PresetIndexDocument.class,
                PresetIndexDocument::new,
                PresetIndexDocument::validate
        );
    }

    public List<PresetRecord> list() {
        return locked(() -> stateLocked().records());
    }

    public Optional<PresetRecord> findByName(String name) {
        String key = PresetNameValidator.conflictKey(name);
        return list().stream().filter(record -> PresetNameValidator.conflictKey(record.preset().name()).equals(key)).findFirst();
    }

    public Optional<PresetRecord> active() {
        return list().stream().filter(PresetRecord::active).findFirst();
    }

    public PresetRecord requireByName(String name) {
        return findByName(name).orElseThrow(() -> new PresetException(PresetError.NOT_FOUND, "Preset not found: " + name));
    }

    public PresetRecord create(String name, VirtualInventorySnapshot snapshot) {
        return locked(() -> {
            RepositoryState state = writableStateLocked();
            String normalized = ensureNameAvailable(state, name, null);
            UUID id = nextUnusedId(state.entries().keySet());
            Instant now = clock.instant();
            InventoryPreset preset = new InventoryPreset(id, normalized, "", List.of(), now, now, snapshot);
            writeNewPresetLocked(preset);
            try {
                PresetIndexDocument index = copyIndex(state.index());
                index.order.add(id.toString());
                writeIndexLocked(index);
            } catch (RuntimeException exception) {
                rollbackCreatedPreset(id, exception);
            }
            return new PresetRecord(preset, revision(presetPath(id)), false);
        });
    }

    public PresetRecord importPreset(InventoryPreset imported) {
        return locked(() -> {
            RepositoryState state = writableStateLocked();
            ensureNameAvailable(state, imported.name(), null);
            if (state.entries().containsKey(imported.id()) || Files.exists(presetPath(imported.id()))) {
                throw new PresetException(PresetError.STALE_DATA, "Preset UUID already exists");
            }
            writeNewPresetLocked(imported);
            try {
                PresetIndexDocument index = copyIndex(state.index());
                index.order.add(imported.id().toString());
                writeIndexLocked(index);
            } catch (RuntimeException exception) {
                rollbackCreatedPreset(imported.id(), exception);
            }
            return new PresetRecord(imported, revision(presetPath(imported.id())), false);
        });
    }

    public PresetRecord rename(UUID id, String expectedRevision, String newName) {
        return locked(() -> {
            RepositoryState state = writableStateLocked();
            PresetRecord current = requireRecord(state, id);
            requireWritableEntry(state, id);
            requireRevision(current, expectedRevision);
            String normalized = ensureNameAvailable(state, newName, id);
            InventoryPreset renamed = current.preset().withName(normalized, clock.instant());
            writePresetLocked(renamed);
            return new PresetRecord(renamed, revision(presetPath(id)), current.active());
        });
    }

    public PresetRecord updateInventory(UUID id, String expectedRevision, VirtualInventorySnapshot snapshot) {
        return locked(() -> {
            RepositoryState state = writableStateLocked();
            PresetRecord current = requireRecord(state, id);
            requireWritableEntry(state, id);
            requireRevision(current, expectedRevision);
            InventoryPreset updated = current.preset().withInventory(snapshot, clock.instant());
            writePresetLocked(updated);
            return new PresetRecord(updated, revision(presetPath(id)), current.active());
        });
    }

    public PresetRecord duplicate(UUID sourceId, String expectedRevision, String newName) {
        return locked(() -> {
            RepositoryState state = writableStateLocked();
            PresetRecord source = requireRecord(state, sourceId);
            requireWritableEntry(state, sourceId);
            requireRevision(source, expectedRevision);
            String normalized = ensureNameAvailable(state, newName, null);
            UUID id = nextUnusedId(state.entries().keySet());
            Instant now = clock.instant();
            InventoryPreset copy = new InventoryPreset(
                    id,
                    normalized,
                    source.preset().description(),
                    source.preset().tags(),
                    now,
                    now,
                    source.preset().inventory()
            );
            writeNewPresetLocked(copy);
            try {
                PresetIndexDocument index = copyIndex(state.index());
                index.order.add(id.toString());
                writeIndexLocked(index);
            } catch (RuntimeException exception) {
                rollbackCreatedPreset(id, exception);
            }
            return new PresetRecord(copy, revision(presetPath(id)), false);
        });
    }

    public void select(UUID id) {
        locked(() -> {
            RepositoryState state = writableStateLocked();
            requireRecord(state, id);
            PresetIndexDocument index = copyIndex(state.index());
            index.activePresetId = id.toString();
            writeIndexLocked(index);
            return null;
        });
    }

    public void delete(UUID id, String expectedRevision) {
        locked(() -> {
            RepositoryState state = writableStateLocked();
            PresetRecord current = requireRecord(state, id);
            requireWritableEntry(state, id);
            requireRevision(current, expectedRevision);
            Files.createDirectories(trashDirectory);
            Path source = presetPath(id);
            Path sourceBackup = source.resolveSibling(source.getFileName() + ".bak");
            Path target = uniquePresetArtifactPath(trashDirectory, id + "-" + clock.millis());
            Path targetBackup = target.resolveSibling(target.getFileName() + ".bak");
            boolean primaryMoved = false;
            boolean backupMoved = false;
            try {
                if (Files.exists(sourceBackup)) {
                    move(sourceBackup, targetBackup);
                    backupMoved = true;
                }
                move(source, target);
                primaryMoved = true;
                PresetIndexDocument index = copyIndex(state.index());
                index.order.remove(id.toString());
                if (id.toString().equals(index.activePresetId)) {
                    index.activePresetId = null;
                }
                writeIndexLocked(index);
            } catch (Exception exception) {
                if (primaryMoved && Files.exists(target)) {
                    try {
                        move(target, source);
                    } catch (IOException rollbackException) {
                        exception.addSuppressed(rollbackException);
                    }
                }
                if (backupMoved && Files.exists(targetBackup)) {
                    try {
                        move(targetBackup, sourceBackup);
                    } catch (IOException rollbackException) {
                        exception.addSuppressed(rollbackException);
                    }
                }
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new PresetException(PresetError.IO_FAILURE, "Could not move preset to trash", exception);
            }
            return null;
        });
    }

    private RepositoryState writableStateLocked() {
        RepositoryState state = stateLocked();
        if (state.indexWriteBlocked()) {
            throw new PresetException(PresetError.READ_ONLY, "Preset index is read-only");
        }
        return state;
    }

    private RepositoryState stateLocked() {
        AtomicJsonStore.LoadResult<PresetIndexDocument> indexResult = indexStore.load();
        PresetIndexDocument index = indexResult.value();
        boolean indexWriteBlocked = indexResult.writeBlocked() || indexBackupUsesNewerSchema();
        Map<UUID, FileEntry> entries = scanPresetFilesLocked(!indexWriteBlocked);

        LinkedHashSet<UUID> normalizedOrder = new LinkedHashSet<>();
        for (String value : index.order) {
            UUID id = UUID.fromString(value);
            if (entries.containsKey(id)) {
                normalizedOrder.add(id);
            }
        }
        entries.keySet().stream().sorted(Comparator.comparing(UUID::toString)).forEach(normalizedOrder::add);

        UUID activeId = index.activePresetId == null ? null : UUID.fromString(index.activePresetId);
        if (activeId != null && !entries.containsKey(activeId)) {
            activeId = null;
        }

        PresetIndexDocument normalizedIndex = new PresetIndexDocument();
        normalizedIndex.activePresetId = activeId == null ? null : activeId.toString();
        normalizedIndex.order = normalizedOrder.stream().map(UUID::toString).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        boolean changed = !normalizedIndex.order.equals(index.order)
                || !java.util.Objects.equals(normalizedIndex.activePresetId, index.activePresetId);
        if (changed && !indexWriteBlocked) {
            if (!indexStore.save(normalizedIndex)) {
                throw new PresetException(PresetError.IO_FAILURE, "Could not normalize preset index");
            }
        }

        List<PresetRecord> records = new ArrayList<>();
        for (UUID id : normalizedOrder) {
            FileEntry entry = entries.get(id);
            if (entry.preset() != null) {
                records.add(new PresetRecord(entry.preset(), entry.revision(), id.equals(activeId)));
            }
        }
        return new RepositoryState(List.copyOf(records), Map.copyOf(entries), normalizedIndex, indexWriteBlocked);
    }

    private Map<UUID, FileEntry> scanPresetFilesLocked(boolean allowRecovery) {
        Map<UUID, FileEntry> entries = new HashMap<>();
        try {
            Files.createDirectories(directory);
            try (var files = Files.list(directory)) {
                files.filter(Files::isRegularFile).forEach(path -> {
                    Matcher matcher = PRESET_FILE.matcher(path.getFileName().toString());
                    Matcher backupMatcher = PRESET_BACKUP_FILE.matcher(path.getFileName().toString());
                    if (!matcher.matches() && !backupMatcher.matches()) {
                        return;
                    }
                    try {
                        boolean backupOnly = !matcher.matches();
                        String rawId = backupOnly ? backupMatcher.group(1) : matcher.group(1);
                        UUID id = UUID.fromString(rawId);
                        String canonical = id + (backupOnly ? ".json.bak" : ".json");
                        if (!path.getFileName().toString().equals(canonical)) {
                            FileEntry existing = entries.get(id);
                            entries.put(id, existing == null
                                    ? new FileEntry(null, null, revision(path), true, true)
                                    : new FileEntry(existing.preset(), existing.name(), existing.revision(), true, true));
                            return;
                        }
                        if (backupOnly && Files.exists(presetPath(id))) {
                            return;
                        }
                        if (entries.containsKey(id)) {
                            FileEntry existing = entries.get(id);
                            entries.put(id, new FileEntry(existing.preset(), existing.name(), existing.revision(), true, true));
                            return;
                        }
                        if (backupOnly) {
                            FileEntry backupEntry = readPresetLocked(path, id, false);
                            if (allowRecovery && backupEntry != null && backupEntry.preset() != null && !backupEntry.writeBlocked()) {
                                Path primary = presetPath(id);
                                Files.copy(path, primary, StandardCopyOption.COPY_ATTRIBUTES);
                                backupEntry = new FileEntry(backupEntry.preset(), backupEntry.name(), revision(primary), false, false);
                            }
                            if (backupEntry != null) {
                                entries.put(id, backupEntry);
                            }
                            return;
                        }
                        FileEntry entry = readPresetLocked(path, id, allowRecovery);
                        if (entry != null) {
                            entries.put(id, entry);
                        }
                    } catch (IllegalArgumentException | IOException ignored) {
                    }
                });
            }
            return entries;
        } catch (IOException exception) {
            throw new PresetException(PresetError.IO_FAILURE, "Could not scan presets", exception);
        }
    }

    private FileEntry readPresetLocked(Path path, UUID expectedId, boolean recover) {
        PresetDocument document;
        try (Reader reader = Files.newBufferedReader(path)) {
            document = gson.fromJson(reader, PresetDocument.class);
            if (document == null) {
                throw new PresetException(PresetError.INVALID_DATA, "Empty preset document");
            }
            if (document.schemaVersion > PresetDocument.CURRENT_SCHEMA_VERSION) {
                String reservedName;
                try {
                    reservedName = PresetNameValidator.normalize(document.name);
                } catch (RuntimeException ignored) {
                    reservedName = null;
                }
                return new FileEntry(null, reservedName, revision(path), true, true);
            }
            document.validateHeader();
            if (!expectedId.toString().equals(document.id)) {
                throw new PresetException(PresetError.INVALID_DATA, "Preset filename and UUID differ");
            }
        } catch (IOException | RuntimeException exception) {
            if (!recover || exception instanceof UnsupportedDataVersionException) {
                return null;
            }
            return recoverPresetLocked(path, expectedId);
        }

        try {
            InventoryPreset preset = document.toPreset(inventorySerializer, registries);
            boolean blocked = presetBackupUsesNewerSchema(path);
            return new FileEntry(preset, preset.name(), revision(path), blocked, blocked);
        } catch (RuntimeException exception) {
            return new FileEntry(null, document.name, revision(path), true, true);
        }
    }

    private FileEntry recoverPresetLocked(Path path, UUID id) {
        Path broken = uniqueArtifactPath(path.getParent(), path.getFileName() + ".broken", "");
        Path backup = path.resolveSibling(path.getFileName() + ".bak");
        try {
            move(path, broken);
            if (!Files.exists(backup)) {
                return new FileEntry(null, null, revision(broken), true, true);
            }
            FileEntry recovered = readPresetLocked(backup, id, false);
            if (recovered == null || recovered.preset() == null) {
                return recovered == null
                        ? new FileEntry(null, null, revision(broken), true, true)
                        : recovered;
            }
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.copy(backup, temporary, StandardCopyOption.REPLACE_EXISTING);
            move(temporary, path);
            return new FileEntry(recovered.preset(), recovered.name(), revision(path), recovered.unavailable(), recovered.writeBlocked());
        } catch (IOException exception) {
            return new FileEntry(null, null, safeRevision(path, broken), true, true);
        }
    }

    private String safeRevision(Path... candidates) {
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                try {
                    return revision(candidate);
                } catch (PresetException ignored) {
                }
            }
        }
        return "";
    }

    private String ensureNameAvailable(RepositoryState state, String requested, UUID allowedId) {
        String normalized = PresetNameValidator.normalize(requested);
        String key = PresetNameValidator.conflictKey(normalized);
        for (Map.Entry<UUID, FileEntry> entry : state.entries().entrySet()) {
            if (!entry.getKey().equals(allowedId) && entry.getValue().name() != null
                    && PresetNameValidator.conflictKey(entry.getValue().name()).equals(key)) {
                throw new PresetException(PresetError.NAME_CONFLICT, "Preset name already exists");
            }
        }
        return normalized;
    }

    private PresetRecord requireRecord(RepositoryState state, UUID id) {
        return state.records().stream().filter(record -> record.preset().id().equals(id)).findFirst()
                .orElseThrow(() -> new PresetException(PresetError.NOT_FOUND, "Preset not found"));
    }

    private void requireWritableEntry(RepositoryState state, UUID id) {
        FileEntry entry = state.entries().get(id);
        if (entry == null || entry.writeBlocked()) {
            throw new PresetException(PresetError.READ_ONLY, "Preset file or backup is read-only");
        }
    }

    private static void requireRevision(PresetRecord record, String expectedRevision) {
        if (expectedRevision != null && !record.revision().equals(expectedRevision)) {
            throw new PresetException(PresetError.STALE_DATA, "Preset changed externally");
        }
    }

    private UUID nextUnusedId(Set<UUID> existing) {
        for (int attempt = 0; attempt < 100; attempt++) {
            UUID id = uuidSupplier.get();
            if (!existing.contains(id) && !Files.exists(presetPath(id))) {
                return id;
            }
        }
        throw new PresetException(PresetError.IO_FAILURE, "Could not allocate preset UUID");
    }

    private void writeNewPresetLocked(InventoryPreset preset) {
        Path path = presetPath(preset.id());
        if (Files.exists(path) || Files.exists(path.resolveSibling(path.getFileName() + ".bak"))) {
            throw new PresetException(PresetError.STALE_DATA, "Preset file already exists");
        }
        writePresetLocked(preset);
    }

    private void writePresetLocked(InventoryPreset preset) {
        PresetDocument document = PresetDocument.fromPreset(preset, inventorySerializer, registries);
        AtomicJsonStore<PresetDocument> store = new AtomicJsonStore<>(
                gson,
                presetPath(preset.id()),
                PresetDocument.class,
                PresetDocument::new,
                value -> value.toPreset(inventorySerializer, registries)
        );
        if (!store.save(document)) {
            throw new PresetException(PresetError.IO_FAILURE, "Could not save preset");
        }
    }

    private void writeIndexLocked(PresetIndexDocument index) {
        if (indexBackupUsesNewerSchema()) {
            throw new PresetException(PresetError.READ_ONLY, "Preset index backup is newer");
        }
        if (!indexStore.save(index)) {
            throw new PresetException(PresetError.IO_FAILURE, "Could not save preset index");
        }
    }

    private Path presetPath(UUID id) {
        Path path = directory.resolve(id + ".json").normalize();
        if (!path.getParent().equals(directory)) {
            throw new PresetException(PresetError.INVALID_DATA, "Unsafe preset path");
        }
        return path;
    }

    private String revision(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new PresetException(PresetError.IO_FAILURE, "Could not fingerprint preset", exception);
        }
    }

    private PresetIndexDocument copyIndex(PresetIndexDocument source) {
        PresetIndexDocument copy = new PresetIndexDocument();
        copy.activePresetId = source.activePresetId;
        copy.order = new ArrayList<>(source.order);
        return copy;
    }

    private void rollbackCreatedPreset(UUID id, RuntimeException original) {
        try {
            Files.deleteIfExists(presetPath(id));
            Files.deleteIfExists(presetPath(id).resolveSibling(id + ".json.bak"));
        } catch (IOException rollbackException) {
            original.addSuppressed(rollbackException);
        }
        throw original;
    }

    private boolean indexBackupUsesNewerSchema() {
        Path backup = indexPath.resolveSibling("index.json.bak");
        if (!Files.exists(backup)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(backup)) {
            PresetIndexDocument document = gson.fromJson(reader, PresetIndexDocument.class);
            return document != null && document.schemaVersion > PresetIndexDocument.CURRENT_SCHEMA_VERSION;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private boolean presetBackupUsesNewerSchema(Path primary) {
        Path backup = primary.resolveSibling(primary.getFileName() + ".bak");
        if (!Files.exists(backup)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(backup)) {
            PresetDocument document = gson.fromJson(reader, PresetDocument.class);
            return document != null && (document.schemaVersion > PresetDocument.CURRENT_SCHEMA_VERSION
                    || document.inventory != null
                    && document.inventory.schemaVersion > com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventoryDocument.CURRENT_SCHEMA_VERSION);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static Path uniqueArtifactPath(Path directory, String base, String suffix) {
        Path candidate = directory.resolve(base + suffix);
        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(base + "." + counter++ + suffix);
        }
        return candidate;
    }

    private static Path uniquePresetArtifactPath(Path directory, String base) {
        Path candidate = directory.resolve(base + ".json");
        int counter = 1;
        while (Files.exists(candidate) || Files.exists(candidate.resolveSibling(candidate.getFileName() + ".bak"))) {
            candidate = directory.resolve(base + "." + counter++ + ".json");
        }
        return candidate;
    }

    private static void move(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private <T> T locked(IoSupplier<T> operation) {
        synchronized (PROCESS_LOCK) {
            try {
                Files.createDirectories(directory);
                try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock ignored = channel.lock()) {
                    return operation.get();
                }
            } catch (PresetException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new PresetException(PresetError.IO_FAILURE, "Preset repository operation failed", exception);
            }
        }
    }

    private record FileEntry(InventoryPreset preset, String name, String revision, boolean unavailable, boolean writeBlocked) {
    }

    private record RepositoryState(
            List<PresetRecord> records,
            Map<UUID, FileEntry> entries,
            PresetIndexDocument index,
            boolean indexWriteBlocked
    ) {
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws Exception;
    }
}
