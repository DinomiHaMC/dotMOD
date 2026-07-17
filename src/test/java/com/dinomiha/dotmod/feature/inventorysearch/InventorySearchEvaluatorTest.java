package com.dinomiha.dotmod.feature.inventorysearch;

import com.dinomiha.dotmod.feature.inventorysearch.query.QueryParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySearchEvaluatorTest {
    private final QueryParser parser = new QueryParser();
    private final InventorySearchEvaluator evaluator = new InventorySearchEvaluator();
    private final ItemSearchDocument sword = ItemSearchDocument.of(
            "Алмазный меч",
            "minecraft:diamond_sword",
            List.of("Bound & blessed"),
            List.of("minecraft:sharpness", "Sharpness V"),
            List.of("When in main hand"),
            2,
            24
    );

    @Test
    void matchesLocalizedNameIdLoreEnchantmentsAndAllText() throws Exception {
        assertMatches("алмазный");
        assertMatches("id=minecraft:diamond_sword");
        assertMatches("id:diamond_sword");
        assertMatches("lore:blessed");
        assertMatches("enchantment:sharpness");
        assertMatches("all-text:\"main hand\"");
        assertFalse(matches("text:iron"));
    }

    @Test
    void evaluatesAllNumericOperatorsAndAnd() throws Exception {
        assertMatches("count=2");
        assertMatches("count!=1");
        assertMatches("count>1");
        assertMatches("count>=2");
        assertMatches("count<3");
        assertMatches("count<=2");
        assertMatches("durability<25 & count>=2");
        assertFalse(matches("durability>=25"));
        assertFalse(matches("count=2 & lore:missing"));
    }

    @Test
    void nonDamageableDocumentsNeverMatchDurability() throws Exception {
        ItemSearchDocument block = ItemSearchDocument.of(
                "Stone", "minecraft:stone", List.of(), List.of(), List.of(), 64, null
        );
        assertFalse(evaluator.matches(parser.parse("durability!=50"), block));
    }

    @Test
    void emptyQueryMatchesEverythingAndNormalizationUsesNfkc() throws Exception {
        assertTrue(evaluator.matches(parser.parse(""), sword));
        assertMatches("ＡＬＬ-ＴＥＸＴ:diamond_sword");
    }

    @Test
    void serverTextIsBoundedByCodePointsAndAggregateBudget() {
        ItemSearchDocument bounded = ItemSearchDocument.of(
                "x".repeat(1000),
                "minecraft:test",
                java.util.Collections.nCopies(100, "л".repeat(1000)),
                java.util.Collections.nCopies(200, "e".repeat(1000)),
                java.util.Collections.nCopies(200, "t".repeat(1000)),
                1,
                null
        );

        assertTrue(bounded.displayName().codePointCount(0, bounded.displayName().length()) <= 256);
        assertTrue(bounded.allText().stream().mapToInt(value -> value.codePointCount(0, value.length())).sum() <= 8192);
        assertTrue(bounded.lore().size() <= 32);
    }

    private void assertMatches(String query) throws Exception {
        assertTrue(matches(query), query);
    }

    private boolean matches(String query) throws Exception {
        return evaluator.matches(parser.parse(query), sword);
    }
}
