package com.dinomiha.dotmod.feature.preset.helper;

import java.util.List;

public record RecipeOption(
        String id,
        ExactItemKey output,
        int outputCount,
        List<RecipeIngredient> ingredients,
        boolean requirementsKnown
) {
    public RecipeOption {
        if (id == null || id.isBlank() || output == null || outputCount <= 0) {
            throw new IllegalArgumentException("Invalid recipe option");
        }
        ingredients = List.copyOf(ingredients);
        if (requirementsKnown && ingredients.isEmpty()) {
            throw new IllegalArgumentException("Known recipe requirements cannot be empty");
        }
    }
}
