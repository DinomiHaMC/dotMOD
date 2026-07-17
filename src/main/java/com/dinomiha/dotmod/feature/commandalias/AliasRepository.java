package com.dinomiha.dotmod.feature.commandalias;

import com.dinomiha.dotmod.storage.AtomicJsonStore;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AliasRepository {
    private final AtomicJsonStore<CommandAliasDocument> store;
    private final Path backupPath;
    private final Gson gson;
    private final Set<String> conflicts;
    private CommandAliasDocument document = new CommandAliasDocument();
    private boolean loaded;
    private boolean writeBlocked;

    public AliasRepository(Path path, Gson gson, Set<String> conflictRoots) {
        this.backupPath = path.resolveSibling(path.getFileName() + ".bak");
        this.gson = gson;
        this.store = new AtomicJsonStore<>(
                gson,
                path,
                CommandAliasDocument.class,
                CommandAliasDocument::new,
                CommandAliasDocument::validate
        );
        this.conflicts = new HashSet<>();
        this.conflicts.add("dot");
        this.conflicts.add("dotmod");
        for (String root : conflictRoots) {
            if (root != null) {
                this.conflicts.add(root.toLowerCase(Locale.ROOT));
            }
        }
    }

    public LoadOutcome load() {
        AtomicJsonStore.LoadResult<CommandAliasDocument> result = store.load();
        document = result.value();
        loaded = true;
        writeBlocked = result.writeBlocked() || backupUsesNewerSchema();
        return new LoadOutcome(list(), result.recovered(), result.created(), writeBlocked);
    }

    public LoadOutcome reload() {
        return load();
    }

    public List<CommandAlias> list() {
        ensureLoaded();
        return List.copyOf(document.aliases);
    }

    public CommandAlias upsert(CommandAlias alias) {
        ensureWritable();
        if (conflicts.contains(alias.name())) {
            throw new AliasException(AliasError.COMMAND_CONFLICT, "Alias conflicts with command " + alias.name());
        }
        List<CommandAlias> next = new ArrayList<>(document.aliases);
        int existing = -1;
        for (int index = 0; index < next.size(); index++) {
            if (next.get(index).name().equals(alias.name())) {
                existing = index;
                break;
            }
        }
        if (existing >= 0) {
            next.set(existing, alias);
        } else {
            next.add(alias);
        }
        CommandAliasDocument candidate = new CommandAliasDocument(next);
        if (new AliasCycleDetector().hasCycle(candidate.aliases)) {
            throw new AliasException(AliasError.CYCLE, "Alias introduces a cycle");
        }
        save(candidate);
        return alias;
    }

    public boolean remove(String name) {
        ensureWritable();
        String normalized = CommandAlias.normalizeName(name);
        List<CommandAlias> next = document.aliases.stream()
                .filter(alias -> !alias.name().equals(normalized))
                .toList();
        if (next.size() == document.aliases.size()) {
            return false;
        }
        save(new CommandAliasDocument(next));
        return true;
    }

    public void save() {
        ensureWritable();
        save(new CommandAliasDocument(document.aliases));
    }

    public boolean writeBlocked() {
        ensureLoaded();
        return writeBlocked;
    }

    private void save(CommandAliasDocument candidate) {
        if (!store.save(candidate)) {
            throw new AliasException(AliasError.IO_FAILURE, "Failed to save command aliases");
        }
        document = candidate;
    }

    private void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private void ensureWritable() {
        ensureLoaded();
        if (writeBlocked) {
            throw new AliasException(AliasError.READ_ONLY, "Command aliases are read-only");
        }
    }

    private boolean backupUsesNewerSchema() {
        if (!Files.exists(backupPath)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(backupPath)) {
            CommandAliasDocument backup = gson.fromJson(reader, CommandAliasDocument.class);
            if (backup != null) {
                backup.validate();
            }
            return false;
        } catch (com.dinomiha.dotmod.storage.UnsupportedDataVersionException exception) {
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    public record LoadOutcome(List<CommandAlias> aliases, boolean recovered, boolean created, boolean writeBlocked) {
    }
}
