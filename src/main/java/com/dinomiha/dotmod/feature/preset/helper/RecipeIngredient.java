package com.dinomiha.dotmod.feature.preset.helper;

import java.util.List;

/** One consumed recipe position. Alternatives are OR choices, occurrences are not deduplicated. */
public record RecipeIngredient(List<ExactItemKey> alternatives, boolean exactComponents) {
    public RecipeIngredient(List<ExactItemKey> alternatives) {
        this(alternatives, true);
    }

    public RecipeIngredient {
        alternatives = List.copyOf(alternatives);
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("A recipe ingredient needs at least one known alternative");
        }
    }

    public boolean accepts(ExactItemKey key) {
        return alternatives.stream().anyMatch(alternative -> exactComponents
                ? alternative.equals(key)
                : alternative.exemplar().isOf(key.exemplar().getItem()));
    }
}
