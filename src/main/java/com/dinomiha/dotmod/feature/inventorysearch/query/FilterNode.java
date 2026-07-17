package com.dinomiha.dotmod.feature.inventorysearch.query;

public record FilterNode(
        FilterType type,
        ComparisonOperator operator,
        String textValue,
        Integer numberValue,
        int start,
        int end
) implements QueryNode {
    public FilterNode {
        if (type == null || operator == null || start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid query filter");
        }
        if (type.numeric() != (numberValue != null) || type.numeric() == (textValue != null)) {
            throw new IllegalArgumentException("Filter value does not match its field");
        }
    }
}
