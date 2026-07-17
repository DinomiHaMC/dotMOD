package com.dinomiha.dotmod.feature.inventorysearch.query;

import java.util.List;

public record AndNode(List<QueryNode> children) implements QueryNode {
    public AndNode {
        children = List.copyOf(children);
        if (children.size() > QueryTokenizer.MAX_CLAUSES) {
            throw new IllegalArgumentException("Too many query clauses");
        }
    }
}
