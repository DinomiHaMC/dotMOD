package com.dinomiha.dotmod.feature.commandalias;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandAliasDocumentTest {
    @Test
    void normalizesNamesAndRejectsDuplicateNormalizedNames() {
        assertEquals("quick-home", new CommandAlias(" Quick-Home ", "home", true).name());

        AliasException exception = assertThrows(AliasException.class, () -> new CommandAliasDocument(List.of(
                new CommandAlias("HOME", "home", true),
                new CommandAlias("home", "spawn", true)
        )));
        assertEquals(AliasError.DUPLICATE_NAME, exception.error());
    }

    @Test
    void enforcesNameTemplateAndControlCharacterBounds() {
        assertError(AliasError.INVALID_NAME, () -> new CommandAlias("bad name", "say hi", true));
        assertError(AliasError.INVALID_NAME, () -> new CommandAlias("a".repeat(33), "say hi", true));
        assertError(AliasError.INVALID_TEMPLATE, () -> new CommandAlias("ok", "", true));
        assertError(AliasError.INVALID_TEMPLATE, () -> new CommandAlias("ok", "x".repeat(257), true));
        assertError(AliasError.INVALID_TEMPLATE, () -> new CommandAlias("ok", "say\u0000bad", true));
        assertError(AliasError.INVALID_TEMPLATE, () -> new CommandAlias("ok", "say \"open", true));
    }

    @Test
    void enforcesDocumentSchemaAndAliasCount() {
        List<CommandAlias> aliases = new ArrayList<>();
        for (int index = 0; index < 129; index++) {
            aliases.add(new CommandAlias("a" + index, "say " + index, true));
        }
        assertError(AliasError.TOO_MANY_ALIASES, () -> new CommandAliasDocument(aliases));

        CommandAliasDocument document = new CommandAliasDocument();
        document.schemaVersion = 0;
        assertError(AliasError.INVALID_DATA, document::validate);
    }

    private static void assertError(AliasError expected, Runnable operation) {
        AliasException exception = assertThrows(AliasException.class, operation::run);
        assertEquals(expected, exception.error());
    }
}
