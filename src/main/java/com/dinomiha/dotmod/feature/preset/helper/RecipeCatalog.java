package com.dinomiha.dotmod.feature.preset.helper;

import java.util.List;

@FunctionalInterface
public interface RecipeCatalog {
    List<RecipeOption> recipesFor(ExactItemKey output);
}
