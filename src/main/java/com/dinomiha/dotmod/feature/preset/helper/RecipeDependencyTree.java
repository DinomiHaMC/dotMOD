package com.dinomiha.dotmod.feature.preset.helper;

import java.util.List;

public record RecipeDependencyTree(ItemNode root) {
    public enum State {
        AVAILABLE,
        RECIPE,
        NO_RECIPE,
        UNKNOWN_REQUIREMENTS,
        CYCLE,
        DEPTH_LIMIT,
        NODE_LIMIT
    }

    public record ItemNode(ExactItemKey item, long required, State state, List<RecipeNode> recipes) {
        public ItemNode {
            recipes = List.copyOf(recipes);
        }
    }

    public record RecipeNode(RecipeOption recipe, long crafts, List<ItemNode> ingredients) {
        public RecipeNode {
            ingredients = List.copyOf(ingredients);
        }
    }
}
