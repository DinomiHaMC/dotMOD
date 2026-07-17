package com.dinomiha.dotmod.feature.preset.helper;

import com.dinomiha.dotmod.feature.preset.PresetRecord;

import java.util.List;

public final class PresetHelperModel {
    private final PresetRecord preset;
    private final PresetProgress progress;
    private final InventoryCounter available;
    private final RecipeCatalog recipes;
    private final boolean includesContainer;
    private final RecipeAvailabilityService availabilityService = new RecipeAvailabilityService();

    public PresetHelperModel(
            PresetRecord preset,
            PresetProgress progress,
            InventoryCounter available,
            RecipeCatalog recipes,
            boolean includesContainer
    ) {
        this.preset = preset;
        this.progress = progress;
        this.available = available;
        this.recipes = recipes;
        this.includesContainer = includesContainer;
    }

    public PresetRecord preset() {
        return preset;
    }

    public PresetProgress progress() {
        return progress;
    }

    public InventoryCounter available() {
        return available;
    }

    public boolean includesContainer() {
        return includesContainer;
    }

    public List<RecipeOption> recipes(PresetRequirement requirement) {
        return recipes.recipesFor(requirement.key());
    }

    public RecipeAvailability availability(PresetRequirement requirement, RecipeOption recipe) {
        InventoryCounter ingredients = available.without(requirement.key(), requirement.satisfied());
        return availabilityService.analyze(recipe, requirement.missing(), ingredients);
    }

    public boolean craftable(PresetRequirement requirement) {
        return requirement.missing() > 0L && recipes(requirement).stream()
                .map(recipe -> availability(requirement, recipe))
                .anyMatch(RecipeAvailability::craftableNow);
    }

    public RecipeDependencyTree dependencyTree(PresetRequirement requirement, RecipeOption recipe) {
        return new RecipeDependencyTreeBuilder(12, 512).build(
                requirement.key(), requirement.required(), available, recipes, recipe
        );
    }
}
