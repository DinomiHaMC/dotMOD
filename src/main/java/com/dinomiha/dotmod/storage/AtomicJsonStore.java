package com.dinomiha.dotmod.storage;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AtomicJsonStore<T> {
    private static final Logger LOGGER = Logger.getLogger(AtomicJsonStore.class.getName());

    private final Gson gson;
    private final Path path;
    private final Class<T> type;
    private final Supplier<T> defaults;
    private final Consumer<T> validator;

    public AtomicJsonStore(Gson gson, Path path, Class<T> type, Supplier<T> defaults, Consumer<T> validator) {
        this.gson = Objects.requireNonNull(gson);
        this.path = Objects.requireNonNull(path);
        this.type = Objects.requireNonNull(type);
        this.defaults = Objects.requireNonNull(defaults);
        this.validator = Objects.requireNonNull(validator);
    }

    public LoadResult<T> load() {
        if (!Files.exists(path)) {
            Optional<T> backup = readBackup();
            if (backup.isPresent()) {
                boolean restored = write(backup.get(), false);
                return new LoadResult<>(backup.get(), true, false, !restored);
            }
            T value = createDefaults();
            boolean created = save(value);
            return new LoadResult<>(value, false, created, !created);
        }
        try {
            T value = read(path);
            return new LoadResult<>(value, false, false, false);
        } catch (UnsupportedDataVersionException exception) {
            LOGGER.log(Level.SEVERE, "Refusing to overwrite newer data in " + path, exception);
            return new LoadResult<>(createDefaults(), true, false, true);
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Failed to load " + path + "; attempting recovery", exception);
            if (!preserveBrokenFile()) {
                return new LoadResult<>(createDefaults(), true, false, true);
            }
            Optional<T> backup = readBackup();
            if (backup.isPresent()) {
                boolean restored = write(backup.get(), false);
                return new LoadResult<>(backup.get(), true, false, !restored);
            }
            T value = createDefaults();
            boolean restored = save(value);
            return new LoadResult<>(value, true, false, !restored);
        }
    }

    public boolean save(T value) {
        return write(value, true);
    }

    private boolean write(T value, boolean rotateBackup) {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            validator.accept(value);
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                gson.toJson(value, writer);
            }
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            if (rotateBackup && Files.exists(path)) {
                Path backup = path.resolveSibling(path.getFileName() + ".bak");
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                forceFile(backup);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            forceDirectory(path.getParent());
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Failed to save " + path, exception);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                LOGGER.log(Level.WARNING, "Failed to remove " + temporary, cleanupException);
            }
            return false;
        }
    }

    private T createDefaults() {
        T value = defaults.get();
        validator.accept(value);
        return value;
    }

    private T read(Path source) throws IOException {
        try (Reader reader = Files.newBufferedReader(source)) {
            T value = gson.fromJson(reader, type);
            if (value == null) {
                throw new IllegalStateException("JSON document is empty");
            }
            validator.accept(value);
            return value;
        }
    }

    private Optional<T> readBackup() {
        Path backup = path.resolveSibling(path.getFileName() + ".bak");
        if (!Files.exists(backup)) {
            return Optional.empty();
        }
        try {
            return Optional.of(read(backup));
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Failed to recover backup " + backup, exception);
            return Optional.empty();
        }
    }

    private boolean preserveBrokenFile() {
        Path broken = path.resolveSibling(path.getFileName() + ".broken");
        try {
            Files.move(path, broken, StandardCopyOption.REPLACE_EXISTING);
            forceDirectory(path.getParent());
            return true;
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to preserve broken file " + path, exception);
            return false;
        }
    }

    private static void forceFile(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private static void forceDirectory(Path directory) {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException exception) {
            LOGGER.log(Level.FINE, "Directory sync is not supported for " + directory, exception);
        }
    }

    public record LoadResult<T>(T value, boolean recovered, boolean created, boolean writeBlocked) {
    }
}
