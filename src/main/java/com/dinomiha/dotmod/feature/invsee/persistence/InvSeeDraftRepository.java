package com.dinomiha.dotmod.feature.invsee.persistence;

import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import com.dinomiha.dotmod.storage.AtomicJsonStore;
import com.google.gson.Gson;
import net.minecraft.registry.RegistryWrapper;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.util.HexFormat;
import java.util.Optional;

public final class InvSeeDraftRepository {
    private static final Object PROCESS_LOCK = new Object();

    private final Path path;
    private final Path backupPath;
    private final Path lockPath;
    private final Gson gson;
    private final RegistryWrapper.WrapperLookup registries;
    private final VirtualInventorySerializer serializer = new VirtualInventorySerializer();
    private final AtomicJsonStore<VirtualInventoryDocument> store;
    private boolean writeBlocked;
    private boolean inspected;
    private FileState observedState = FileState.missing();

    public InvSeeDraftRepository(Path path, Gson gson, RegistryWrapper.WrapperLookup registries) {
        this.path = path;
        this.backupPath = path.resolveSibling(path.getFileName() + ".bak");
        this.lockPath = path.resolveSibling(path.getFileName() + ".lock");
        this.gson = gson;
        this.registries = registries;
        this.store = new AtomicJsonStore<>(
                gson,
                path,
                VirtualInventoryDocument.class,
                VirtualInventoryDocument::new,
                VirtualInventoryDocument::validateStructure
        );
    }

    public LoadOutcome load() {
        synchronized (PROCESS_LOCK) {
            try {
                Files.createDirectories(path.getParent());
                try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock ignored = channel.lock()) {
                    return loadLocked();
                }
            } catch (IOException | RuntimeException exception) {
                writeBlocked = true;
                inspected = true;
                return new LoadOutcome(Optional.empty(), false, true, false);
            }
        }
    }

    private LoadOutcome loadLocked() {
        inspected = true;
        if (!Files.exists(path) && !Files.exists(backupPath)) {
            writeBlocked = false;
            observedState = FileState.missing();
            return new LoadOutcome(Optional.empty(), false, false, false);
        }
        AtomicJsonStore.LoadResult<VirtualInventoryDocument> result = store.load();
        writeBlocked = result.writeBlocked() || backupUsesNewerSchema();
        observedState = FileState.capture(path, backupPath);
        try {
            VirtualInventorySnapshot snapshot = serializer.decode(result.value(), registries);
            return new LoadOutcome(Optional.of(snapshot), result.recovered(), writeBlocked, false);
        } catch (RuntimeException exception) {
            writeBlocked = true;
            return new LoadOutcome(Optional.empty(), result.recovered(), true, true);
        }
    }

    public boolean save(VirtualInventorySnapshot snapshot) {
        if (!inspected) {
            load();
        }
        if (writeBlocked) {
            return false;
        }
        synchronized (PROCESS_LOCK) {
            try {
                Files.createDirectories(path.getParent());
                try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock ignored = channel.lock()) {
                    FileState current = FileState.capture(path, backupPath);
                    if (!current.readable() || !current.equals(observedState) || backupUsesNewerSchema()) {
                        return false;
                    }

                    VirtualInventoryDocument document = serializer.encode(snapshot, registries);
                    FileState expected = current.afterSave(gson.toJson(document));
                    if (!store.save(document)) {
                        return false;
                    }
                    FileState actual = FileState.capture(path, backupPath);
                    if (!actual.equals(expected)) {
                        writeBlocked = true;
                        return false;
                    }
                    observedState = expected;
                    return true;
                }
            } catch (IOException | RuntimeException exception) {
                return false;
            }
        }
    }

    private boolean backupUsesNewerSchema() {
        if (!Files.exists(backupPath)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(backupPath)) {
            VirtualInventoryDocument backup = gson.fromJson(reader, VirtualInventoryDocument.class);
            if (backup == null) {
                return false;
            }
            backup.validateStructure();
            return false;
        } catch (com.dinomiha.dotmod.storage.UnsupportedDataVersionException exception) {
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    public record LoadOutcome(
            Optional<VirtualInventorySnapshot> snapshot,
            boolean recovered,
            boolean writeBlocked,
            boolean decodeFailed
    ) {
    }

    private record FileFingerprint(boolean exists, boolean readable, String sha256) {
        static FileFingerprint missing() {
            return new FileFingerprint(false, true, "");
        }

        static FileFingerprint capture(Path path) {
            if (!Files.exists(path)) {
                return missing();
            }
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return new FileFingerprint(true, true, HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path))));
            } catch (IOException exception) {
                return new FileFingerprint(true, false, "");
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 is unavailable", exception);
            }
        }

        static FileFingerprint of(String content) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                return new FileFingerprint(true, true, HexFormat.of().formatHex(digest.digest(bytes)));
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 is unavailable", exception);
            }
        }
    }

    private record FileState(FileFingerprint primary, FileFingerprint backup) {
        static FileState missing() {
            return new FileState(FileFingerprint.missing(), FileFingerprint.missing());
        }

        static FileState capture(Path primary, Path backup) {
            return new FileState(FileFingerprint.capture(primary), FileFingerprint.capture(backup));
        }

        boolean readable() {
            return primary.readable() && backup.readable();
        }

        FileState afterSave(String serializedDocument) {
            FileFingerprint nextBackup = primary.exists() ? primary : backup;
            return new FileState(FileFingerprint.of(serializedDocument), nextBackup);
        }
    }
}
