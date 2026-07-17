package com.dinomiha.dotmod.feature.commandalias;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AliasExpanderTest {
    @Test
    void tokenizesQuotedArgumentsAndEscapesEachInsertedValue() {
        AliasExpander expander = expander(alias("tell", "msg $1 $2"));

        assertEquals("msg \"two words\" \"a\\\"b\"", expander.expand("tell \"two words\" 'a\"b'"));
    }

    @Test
    void expandsPositionalRestAndLiteralDollarPlaceholders() {
        AliasExpander expander = expander(alias("route", "execute $2 $$ $1 $*"));

        assertEquals("execute second $ first first second third", expander.expand("route first second third"));
        assertEquals("say $", expander(alias("cash", "say $$")).expand("cash ignored"));
    }

    @Test
    void missingPositionIsEmptyAndArgumentsAreNotImplicitlyAppended() {
        AliasExpander expander = expander(alias("one", "say $1 $9"));

        assertEquals("say value", expander.expand("one value extra"));
    }

    @Test
    void appendsAllArgumentsWhenTemplateHasNoArgumentPlaceholder() {
        AliasExpander expander = expander(alias("gm", "gamemode creative"));

        assertEquals("gamemode creative \"Player One\"", expander.expand("gm \"Player One\""));
    }

    @Test
    void recursivelyExpandsInMemoryAndPreservesLeadingSlash() {
        AliasExpander expander = expander(
                alias("a", "b $*"),
                alias("b", "say $*")
        );

        assertEquals("/say hello", expander.expand("/a hello"));
    }

    @Test
    void normalizesSlashPrefixedTemplatesForFabricCommandEvents() {
        AliasExpander expander = expander(alias("home", " /warp home "));

        assertEquals("warp home", expander.expand("home"));
    }

    @Test
    void matchesAliasRootsCaseInsensitively() {
        assertEquals("say 1", expander(alias("s", "say 1")).expand("S"));
    }

    @Test
    void ignoresDisabledAliasesIncludingDuringRecursion() {
        AliasExpander expander = expander(
                alias("a", "b $*"),
                new CommandAlias("b", "say changed", false)
        );

        assertEquals("b value", expander.expand("a value"));
        assertEquals("b value", expander.expand("b value"));
    }

    @Test
    void reportsCyclesDepthOutputAndMalformedInputWithTypedFailures() {
        assertError(AliasError.CYCLE, () -> expander(alias("a", "b"), alias("b", "a")).expand("a"));

        CommandAlias[] deep = new CommandAlias[17];
        for (int index = 0; index < deep.length; index++) {
            deep[index] = alias("a" + index, index == 16 ? "say done" : "a" + (index + 1));
        }
        assertError(AliasError.MAX_DEPTH, () -> expander(deep).expand("a0"));
        assertError(AliasError.OUTPUT_TOO_LONG,
                () -> expander(alias("long", "say $1")).expand("long " + "x".repeat(253)));
        assertError(AliasError.INVALID_INPUT, () -> expander(alias("a", "say $1")).expand("a \"open"));
        assertError(AliasError.INVALID_INPUT, () -> expander(alias("a", "say $1")).expand("a bad\nvalue"));
        assertEquals("disabled " + "x".repeat(300),
                expander(new CommandAlias("disabled", "say nope", false)).expand("disabled " + "x".repeat(300)));
    }

    private static CommandAlias alias(String name, String template) {
        return new CommandAlias(name, template, true);
    }

    private static AliasExpander expander(CommandAlias... aliases) {
        return new AliasExpander(List.of(aliases));
    }

    private static void assertError(AliasError expected, Runnable operation) {
        AliasException exception = assertThrows(AliasException.class, operation::run);
        assertEquals(expected, exception.error());
    }
}
