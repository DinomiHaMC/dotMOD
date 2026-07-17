package com.dinomiha.dotmod.feature.inventorysearch.query;

public enum ComparisonOperator {
    CONTAINS(":"),
    EQUALS("="),
    NOT_EQUALS("!="),
    LESS("<"),
    LESS_OR_EQUAL("<="),
    GREATER(">"),
    GREATER_OR_EQUAL(">=");

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static ComparisonOperator parse(String symbol) {
        for (ComparisonOperator operator : values()) {
            if (operator.symbol.equals(symbol)) {
                return operator;
            }
        }
        return null;
    }
}
