package com.dinomiha.dotmod.feature.inventorysearch.query;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryParserTest {
    private final QueryParser parser = new QueryParser();

    @Test
    void parsesDocumentedExamplesAndLogicalAnd() throws Exception {
        assertTrue(parser.parse("").children().isEmpty());
        FilterNode bare = filter(parser.parse("diamond sword"), 0);
        assertEquals(FilterType.ALL_TEXT, bare.type());
        assertEquals("diamond sword", bare.textValue());

        AndNode query = parser.parse("text:\"Diamond Sword\" & count>=2 & durability<=25%");
        assertEquals(3, query.children().size());
        assertEquals(FilterType.TEXT, filter(query, 0).type());
        assertEquals(0, filter(query, 0).start());
        assertEquals(20, filter(query, 0).end());
        assertEquals(ComparisonOperator.GREATER_OR_EQUAL, filter(query, 1).operator());
        assertEquals(2, filter(query, 1).numberValue());
        assertEquals(25, filter(query, 2).numberValue());
    }

    @Test
    void supportsIdsLoreEnchantmentsQuotesAndEscapes() throws Exception {
        assertEquals("minecraft:diamond_sword", filter(parser.parse("id=minecraft:diamond_sword"), 0).textValue());
        assertEquals("bound & blessed", filter(parser.parse("lore:'bound & blessed'"), 0).textValue());
        assertEquals("bound & blessed", filter(parser.parse("lore:bound \\& blessed"), 0).textValue());
        assertEquals("sharpness", filter(parser.parse("enchantment:sharpness"), 0).textValue());
        assertEquals("miner's pick", filter(parser.parse("Miner's Pick"), 0).textValue());
        assertEquals("minecraft:stone", filter(parser.parse("minecraft:stone"), 0).textValue());
    }

    @Test
    void parsesEveryComparisonOperator() throws Exception {
        List<ComparisonOperator> operators = List.of(
                filter(parser.parse("count:1"), 0).operator(),
                filter(parser.parse("count=1"), 0).operator(),
                filter(parser.parse("count!=1"), 0).operator(),
                filter(parser.parse("count<1"), 0).operator(),
                filter(parser.parse("count<=1"), 0).operator(),
                filter(parser.parse("count>1"), 0).operator(),
                filter(parser.parse("count>=1"), 0).operator()
        );
        assertEquals(List.of(
                ComparisonOperator.EQUALS,
                ComparisonOperator.EQUALS,
                ComparisonOperator.NOT_EQUALS,
                ComparisonOperator.LESS,
                ComparisonOperator.LESS_OR_EQUAL,
                ComparisonOperator.GREATER,
                ComparisonOperator.GREATER_OR_EQUAL
        ), operators);
    }

    @Test
    void reportsMalformedQueriesWithoutPartialAst() {
        assertError("& stone", QueryError.EMPTY_CLAUSE);
        assertError("stone &", QueryError.EMPTY_CLAUSE);
        assertError("stone && dirt", QueryError.EMPTY_CLAUSE);
        assertError("text:", QueryError.EMPTY_VALUE);
        assertError("unknown=thing", QueryError.UNKNOWN_FIELD);
        assertError("text<thing", QueryError.INVALID_OPERATOR);
        assertError("text==thing", QueryError.INVALID_OPERATOR);
        assertError("text!==thing", QueryError.INVALID_OPERATOR);
        assertError("count=many", QueryError.INVALID_NUMBER);
        assertError("count=+2", QueryError.INVALID_NUMBER);
        assertError("count=2%", QueryError.INVALID_NUMBER);
        assertError("durability=101", QueryError.NUMBER_OUT_OF_RANGE);
        assertError("text:\"broken", QueryError.UNTERMINATED_QUOTE);
        assertError("text:\"closed\" trailing", QueryError.TRAILING_INPUT);
        assertError("text:foo 'bar'", QueryError.TRAILING_INPUT);
        assertError("text:bad\\n", QueryError.INVALID_ESCAPE);
    }

    @Test
    void diagnosticSpansPointAtTheActualQuotedValue() {
        QueryParseException exception = assertThrows(
                QueryParseException.class, () -> parser.parse("lore:ok & text:\"broken")
        );
        assertEquals(QueryError.UNTERMINATED_QUOTE, exception.error());
        assertEquals(15, exception.start());
        assertEquals(22, exception.end());
    }

    @Test
    void enforcesLengthAndClauseLimits() {
        assertError("x".repeat(QueryTokenizer.MAX_LENGTH + 1), QueryError.QUERY_TOO_LONG);
        assertError(String.join("&", java.util.Collections.nCopies(17, "x")), QueryError.TOO_MANY_CLAUSES);
    }

    @Test
    void seededInputNeverEscapesTypedParseFailures() {
        Random random = new Random(0xD07D0DL);
        String alphabet = "abc &:\"'\\!=<>123_%";
        for (int sample = 0; sample < 5000; sample++) {
            int length = random.nextInt(80);
            StringBuilder value = new StringBuilder(length);
            for (int index = 0; index < length; index++) {
                value.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            assertDoesNotThrow(() -> {
                try {
                    parser.parse(value.toString());
                } catch (QueryParseException exception) {
                    assertTrue(exception.start() >= 0);
                    assertTrue(exception.end() >= exception.start());
                    assertTrue(exception.end() <= value.length());
                }
            });
        }
    }

    private void assertError(String query, QueryError expected) {
        QueryParseException exception = assertThrows(QueryParseException.class, () -> parser.parse(query));
        assertEquals(expected, exception.error());
        assertTrue(exception.start() >= 0);
        assertTrue(exception.end() >= exception.start());
    }

    private static FilterNode filter(AndNode node, int index) {
        return (FilterNode) node.children().get(index);
    }
}
