package com.dinomiha.dotmod.feature.inventorysearch;

import com.dinomiha.dotmod.feature.inventorysearch.query.AndNode;
import com.dinomiha.dotmod.feature.inventorysearch.query.ComparisonOperator;
import com.dinomiha.dotmod.feature.inventorysearch.query.FilterNode;
import com.dinomiha.dotmod.feature.inventorysearch.query.QueryNode;

import java.util.List;

public final class InventorySearchEvaluator {
    public boolean matches(QueryNode query, ItemSearchDocument document) {
        if (query instanceof AndNode and) {
            return and.children().stream().allMatch(child -> matches(child, document));
        }
        FilterNode filter = (FilterNode) query;
        if (filter.type().numeric()) {
            Integer actual = filter.type() == com.dinomiha.dotmod.feature.inventorysearch.query.FilterType.COUNT
                    ? Integer.valueOf(document.count())
                    : document.durabilityPercent();
            return actual != null && compare(actual, filter.numberValue(), filter.operator());
        }
        List<String> candidates = switch (filter.type()) {
            case TEXT -> List.of(document.displayName());
            case ID -> List.of(document.itemId());
            case LORE -> document.lore();
            case ENCHANTMENT -> document.enchantments();
            case ALL_TEXT -> document.allText();
            default -> List.of();
        };
        if (candidates.isEmpty()) {
            return false;
        }
        return switch (filter.operator()) {
            case CONTAINS -> candidates.stream().anyMatch(value -> value.contains(filter.textValue()));
            case EQUALS -> candidates.stream().anyMatch(value -> value.equals(filter.textValue()));
            case NOT_EQUALS -> candidates.stream().noneMatch(value -> value.equals(filter.textValue()));
            default -> false;
        };
    }

    private static boolean compare(int actual, int expected, ComparisonOperator operator) {
        return switch (operator) {
            case EQUALS, CONTAINS -> actual == expected;
            case NOT_EQUALS -> actual != expected;
            case LESS -> actual < expected;
            case LESS_OR_EQUAL -> actual <= expected;
            case GREATER -> actual > expected;
            case GREATER_OR_EQUAL -> actual >= expected;
        };
    }
}
