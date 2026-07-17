package com.dinomiha.dotmod.feature.preset.helper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.util.context.ContextParameterMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Recipes exposed by the current connection's vanilla client recipe book. */
public final class ClientRecipeCatalog implements RecipeCatalog {
    private final List<RecipeOption> recipes;

    private ClientRecipeCatalog(List<RecipeOption> recipes) {
        this.recipes = List.copyOf(recipes);
    }

    public static ClientRecipeCatalog capture(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return new ClientRecipeCatalog(List.of());
        }
        Map<NetworkRecipeId, RecipeDisplayEntry> displays = new LinkedHashMap<>();
        for (RecipeResultCollection collection : client.player.getRecipeBook().getOrderedResults()) {
            for (RecipeDisplayEntry entry : collection.getAllRecipes()) {
                displays.putIfAbsent(entry.id(), entry);
            }
        }
        ContextParameterMap context = SlotDisplayContexts.createParameters(client.world);
        List<RecipeOption> recipes = new ArrayList<>();
        for (RecipeDisplayEntry entry : displays.values()) {
            IngredientCapture capture = ingredients(entry);
            int variant = 0;
            for (ItemStack output : entry.getStacks(context)) {
                if (output.isEmpty()) {
                    continue;
                }
                recipes.add(new RecipeOption(
                        "client:" + entry.id().index() + ":" + variant++,
                        new ExactItemKey(output),
                        output.getCount(),
                        capture.ingredients(),
                        capture.known()
                ));
            }
        }
        return new ClientRecipeCatalog(recipes);
    }

    private static IngredientCapture ingredients(RecipeDisplayEntry entry) {
        if (entry.craftingRequirements().isEmpty()) {
            return new IngredientCapture(List.of(), false);
        }
        List<RecipeIngredient> result = new ArrayList<>();
        for (Ingredient ingredient : entry.craftingRequirements().orElseThrow()) {
            if (ingredient.requiresTesting()) {
                return new IngredientCapture(List.of(), false);
            }
            List<ExactItemKey> alternatives = ingredient.getMatchingItems()
                    .map(item -> new ExactItemKey(new ItemStack(item.value())))
                    .distinct()
                    .toList();
            if (alternatives.isEmpty()) {
                return new IngredientCapture(List.of(), false);
            }
            result.add(new RecipeIngredient(alternatives, false));
        }
        return new IngredientCapture(List.copyOf(result), !result.isEmpty());
    }

    @Override
    public List<RecipeOption> recipesFor(ExactItemKey output) {
        return recipes.stream().filter(recipe -> recipe.output().equals(output)).toList();
    }

    private record IngredientCapture(List<RecipeIngredient> ingredients, boolean known) {
    }
}
