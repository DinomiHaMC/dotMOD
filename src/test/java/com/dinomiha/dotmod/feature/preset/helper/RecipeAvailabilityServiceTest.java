package com.dinomiha.dotmod.feature.preset.helper;

import com.dinomiha.dotmod.feature.invsee.MinecraftTestBootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeAvailabilityServiceTest {
    private static ExactItemKey stone;
    private static ExactItemKey dirt;
    private static ExactItemKey output;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
        stone = new ExactItemKey(new ItemStack(Items.STONE));
        dirt = new ExactItemKey(new ItemStack(Items.DIRT));
        output = new ExactItemKey(new ItemStack(Items.STONE_BRICKS));
    }

    @Test
    void allocatesCompetingAlternativesInsteadOfUsingGreedyCounts() {
        RecipeOption recipe = new RecipeOption("test", output, 4, List.of(
                new RecipeIngredient(List.of(stone, dirt)),
                new RecipeIngredient(List.of(stone))
        ), true);

        RecipeAvailability result = new RecipeAvailabilityService().analyze(
                recipe, 4, new InventoryCounter(List.of(
                        new ItemStack(Items.STONE), new ItemStack(Items.DIRT)
                ))
        );

        assertEquals(1, result.craftsNeeded());
        assertEquals(1, result.craftsPossibleNow());
        assertTrue(result.craftableNow());
    }

    @Test
    void resolvesThreeWayAlternativeCompetition() {
        ExactItemKey cobblestone = new ExactItemKey(new ItemStack(Items.COBBLESTONE));
        RecipeOption recipe = new RecipeOption("competition", output, 1, List.of(
                new RecipeIngredient(List.of(stone, dirt)),
                new RecipeIngredient(List.of(stone)),
                new RecipeIngredient(List.of(dirt, cobblestone))
        ), true);

        RecipeAvailability result = new RecipeAvailabilityService().analyze(
                recipe, 1, new InventoryCounter(List.of(
                        new ItemStack(Items.STONE),
                        new ItemStack(Items.DIRT),
                        new ItemStack(Items.COBBLESTONE)
                ))
        );

        assertEquals(1, result.craftsPossibleNow());
    }

    @Test
    void repeatedIngredientsRemainSeparateAndMissingIsReported() {
        RecipeIngredient ingredient = new RecipeIngredient(List.of(stone));
        RecipeOption recipe = new RecipeOption("test", output, 1, List.of(ingredient, ingredient), true);

        RecipeAvailability result = new RecipeAvailabilityService().analyze(
                recipe, 1, new InventoryCounter(List.of(new ItemStack(Items.STONE)))
        );

        assertEquals(0, result.craftsPossibleNow());
        assertEquals(1, result.ingredients().stream().mapToLong(RecipeAvailability.IngredientStatus::missing).sum());
        assertFalse(result.craftableNow());
    }

    @Test
    void unknownRequirementsNeverClaimCraftability() {
        RecipeOption recipe = new RecipeOption("special", output, 1, List.of(), false);
        RecipeAvailability result = new RecipeAvailabilityService().analyze(
                recipe, 1, new InventoryCounter(List.of(new ItemStack(Items.STONE, 64)))
        );

        assertEquals(0, result.craftsPossibleNow());
        assertTrue(result.ingredients().isEmpty());
    }

    @Test
    void hugeRequestedCountDoesNotOverflowTotalDemand() {
        RecipeIngredient ingredient = new RecipeIngredient(List.of(stone));
        RecipeOption recipe = new RecipeOption("huge", output, 1, List.of(ingredient, ingredient), true);

        RecipeAvailability result = new RecipeAvailabilityService().analyze(
                recipe, Long.MAX_VALUE, new InventoryCounter(List.of())
        );

        assertEquals(Long.MAX_VALUE, result.craftsNeeded());
        assertEquals(0, result.craftsPossibleNow());
    }
}
