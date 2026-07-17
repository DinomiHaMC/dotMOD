package com.dinomiha.dotmod.feature.inventorysearch.query;

public enum QueryError {
    QUERY_TOO_LONG,
    TOO_MANY_CLAUSES,
    EMPTY_CLAUSE,
    EMPTY_VALUE,
    UNKNOWN_FIELD,
    INVALID_OPERATOR,
    UNTERMINATED_QUOTE,
    INVALID_ESCAPE,
    INVALID_NUMBER,
    NUMBER_OUT_OF_RANGE,
    TRAILING_INPUT
}
