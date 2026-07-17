package com.dinomiha.dotmod.feature.commandlist;

import com.dinomiha.dotmod.storage.AtomicJsonStore;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommandHistoryService {
    public static final int MAX_HISTORY_LIMIT = 500;
    public static final int MAX_PINNED = 100;

    private final AtomicJsonStore<CommandHistoryDocument> store;
    private final Path path;
    private final Path backupPath;
    private final Gson gson;
    private final int historyLimit;
    private List<CommandEntry> recent = List.of();
    private List<CommandEntry> pinned = List.of();
    private boolean writeBlocked;

    public CommandHistoryService(Path path, Gson gson, int historyLimit) {
        Objects.requireNonNull(path, "path");
        this.path = path;
        this.gson = Objects.requireNonNull(gson, "gson");
        this.historyLimit = Math.clamp(historyLimit, 0, MAX_HISTORY_LIMIT);
        this.backupPath = path.resolveSibling(path.getFileName() + ".bak");
        this.store = new AtomicJsonStore<>(
                gson,
                path,
                CommandHistoryDocument.class,
                CommandHistoryDocument::new,
                CommandHistoryDocument::validate
        );
        reload();
    }

    public synchronized LoadOutcome reload() {
        AtomicJsonStore.LoadResult<CommandHistoryDocument> result = store.load();
        CommandHistoryDocument document = result.value();
        List<CommandEntry> loadedRecent = limitedCopy(document.recent);
        List<CommandEntry> loadedPinned = List.copyOf(document.pinned.subList(
                0, Math.min(MAX_PINNED, document.pinned.size())
        ));
        boolean backupBlocked = backupUsesNewerSchema();
        writeBlocked = result.writeBlocked() || backupBlocked;

        boolean trimmed = loadedRecent.size() != document.recent.size()
                || loadedPinned.size() != document.pinned.size();
        if (trimmed && !writeBlocked) {
            CommandHistoryDocument normalized = document(loadedRecent, loadedPinned);
            if (!store.save(normalized)) {
                writeBlocked = true;
            }
        }
        recent = loadedRecent;
        pinned = loadedPinned;
        return new LoadOutcome(recent, pinned, result.recovered(), result.created(), writeBlocked);
    }

    public synchronized List<CommandEntry> recent() {
        return recent;
    }

    public synchronized List<CommandEntry> pinned() {
        return pinned;
    }

    public synchronized boolean writeBlocked() {
        return writeBlocked;
    }

    public int historyLimit() {
        return historyLimit;
    }

    public synchronized boolean record(String command) {
        if (SensitiveCommandFilter.isSensitive(command)) {
            return false;
        }
        CommandEntry entry = new CommandEntry(command);
        if (writeBlocked) {
            return false;
        }

        ArrayList<CommandEntry> nextPinned = new ArrayList<>(pinned);
        if (nextPinned.remove(entry)) {
            nextPinned.addFirst(entry);
            return save(recent, nextPinned);
        }
        ArrayList<CommandEntry> nextRecent = new ArrayList<>(recent);
        nextRecent.remove(entry);
        nextRecent.addFirst(entry);
        trim(nextRecent);
        return save(nextRecent, pinned);
    }

    public synchronized boolean pin(String command) {
        if (SensitiveCommandFilter.isSensitive(command)) {
            return false;
        }
        CommandEntry entry = new CommandEntry(command);
        if (writeBlocked) {
            return false;
        }

        ArrayList<CommandEntry> nextRecent = new ArrayList<>(recent);
        nextRecent.remove(entry);
        ArrayList<CommandEntry> nextPinned = new ArrayList<>(pinned);
        nextPinned.remove(entry);
        nextPinned.addFirst(entry);
        if (nextPinned.size() > MAX_PINNED) {
            nextPinned.subList(MAX_PINNED, nextPinned.size()).clear();
        }
        return save(nextRecent, nextPinned);
    }

    public synchronized boolean unpin(String command) {
        CommandEntry entry = new CommandEntry(command);
        if (writeBlocked) {
            return false;
        }
        ArrayList<CommandEntry> nextPinned = new ArrayList<>(pinned);
        if (!nextPinned.remove(entry)) {
            return true;
        }
        return save(recent, nextPinned);
    }

    public synchronized boolean clearRecent() {
        if (writeBlocked) {
            return false;
        }
        if (recent.isEmpty()) {
            return true;
        }
        return save(List.of(), pinned);
    }

    private boolean save(List<CommandEntry> nextRecent, List<CommandEntry> nextPinned) {
        if (usesNewerSchema(path) || backupUsesNewerSchema()) {
            writeBlocked = true;
            return false;
        }
        CommandHistoryDocument document = document(nextRecent, nextPinned);
        if (!store.save(document)) {
            return false;
        }
        recent = List.copyOf(nextRecent);
        pinned = List.copyOf(nextPinned);
        return true;
    }

    private CommandHistoryDocument document(List<CommandEntry> recent, List<CommandEntry> pinned) {
        CommandHistoryDocument document = new CommandHistoryDocument();
        document.recent = new ArrayList<>(recent);
        document.pinned = new ArrayList<>(pinned);
        return document;
    }

    private List<CommandEntry> limitedCopy(List<CommandEntry> entries) {
        return List.copyOf(entries.subList(0, Math.min(historyLimit, entries.size())));
    }

    private void trim(ArrayList<CommandEntry> entries) {
        if (entries.size() > historyLimit) {
            entries.subList(historyLimit, entries.size()).clear();
        }
    }

    private boolean backupUsesNewerSchema() {
        return usesNewerSchema(backupPath);
    }

    private boolean usesNewerSchema(Path candidate) {
        if (!Files.exists(candidate)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(candidate)) {
            CommandHistoryDocument backup = gson.fromJson(reader, CommandHistoryDocument.class);
            if (backup != null) {
                backup.validate();
            }
            return false;
        } catch (UnsupportedDataVersionException exception) {
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    public record LoadOutcome(
            List<CommandEntry> recent,
            List<CommandEntry> pinned,
            boolean recovered,
            boolean created,
            boolean writeBlocked
    ) {
    }
}
