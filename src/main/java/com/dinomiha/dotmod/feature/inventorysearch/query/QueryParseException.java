package com.dinomiha.dotmod.feature.inventorysearch.query;

public final class QueryParseException extends Exception {
    private final QueryError error;
    private final int start;
    private final int end;

    public QueryParseException(QueryError error, int start, int end) {
        super(error.name());
        this.error = error;
        this.start = start;
        this.end = end;
    }

    public QueryError error() {
        return error;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }
}
