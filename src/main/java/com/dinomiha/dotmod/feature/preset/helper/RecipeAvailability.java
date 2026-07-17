package com.dinomiha.dotmod.feature.preset.helper;

import java.util.List;

public record RecipeAvailability(
        RecipeOption recipe,
        long craftsNeeded,
        long craftsPossibleNow,
        List<IngredientStatus> ingredients
) {
    public RecipeAvailability {
        ingredients = List.copyOf(ingredients);
    }

    public boolean craftableNow() {
        return craftsPossibleNow > 0L;
    }

    public record IngredientStatus(RecipeIngredient ingredient, long required, long available, long missing) {
    }
}
