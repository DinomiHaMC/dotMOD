package com.dinomiha.dotmod.feature.commandalias;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.feature.commandlist.CommandHistoryService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Set;

public final class CommandClientService {
    public static final int HISTORY_LIMIT = 100;
    private static CommandClientService instance;

    private final AliasRepository aliases;
    private final CommandHistoryService history;

    private CommandClientService(ConfigService config) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        aliases = new AliasRepository(config.paths().aliasesFile(), gson, Set.of());
        aliases.load();
        history = new CommandHistoryService(config.paths().commandHistoryFile(), gson, HISTORY_LIMIT);
    }

    public static void initialize() {
        instance = new CommandClientService(ConfigService.get());
    }

    public static CommandClientService get() {
        if (instance == null) {
            throw new IllegalStateException("CommandClientService has not been initialized");
        }
        return instance;
    }

    public AliasRepository aliases() {
        return aliases;
    }

    public CommandHistoryService history() {
        return history;
    }

    public boolean aliasesEnabled() {
        return ConfigService.get().config().commandAliases.enabled;
    }

    public boolean reload() {
        AliasRepository.LoadOutcome aliasOutcome = aliases.reload();
        CommandHistoryService.LoadOutcome historyOutcome = history.reload();
        return !aliasOutcome.recovered() && !aliasOutcome.writeBlocked()
                && !historyOutcome.recovered() && !historyOutcome.writeBlocked();
    }
}
