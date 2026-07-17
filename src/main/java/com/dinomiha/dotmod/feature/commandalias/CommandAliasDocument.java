package com.dinomiha.dotmod.feature.commandalias;

import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CommandAliasDocument {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_ALIASES = 128;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public List<CommandAlias> aliases = new ArrayList<>();

    public CommandAliasDocument() {
    }

    public CommandAliasDocument(List<CommandAlias> aliases) {
        this.aliases = new ArrayList<>(aliases);
        validate();
    }

    public void validate() {
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedDataVersionException("Unsupported command alias schema " + schemaVersion);
        }
        if (schemaVersion < 1 || aliases == null) {
            throw new AliasException(AliasError.INVALID_DATA, "Invalid command alias document");
        }
        if (aliases.size() > MAX_ALIASES) {
            throw new AliasException(AliasError.TOO_MANY_ALIASES, "A maximum of 128 aliases is allowed");
        }
        Set<String> names = new HashSet<>();
        List<CommandAlias> normalized = new ArrayList<>(aliases.size());
        for (CommandAlias alias : aliases) {
            if (alias == null) {
                throw new AliasException(AliasError.INVALID_DATA, "Alias entry is missing");
            }
            CommandAlias checked = new CommandAlias(alias.name(), alias.template(), alias.enabled());
            if (!names.add(checked.name())) {
                throw new AliasException(AliasError.DUPLICATE_NAME, "Duplicate alias " + checked.name());
            }
            normalized.add(checked);
        }
        aliases = normalized;
        schemaVersion = CURRENT_SCHEMA_VERSION;
    }
}
