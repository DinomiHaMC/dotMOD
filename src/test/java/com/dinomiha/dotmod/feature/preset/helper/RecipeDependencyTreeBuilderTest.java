package com.dinomiha.dotmod.feature.preset.helper;

import com.dinomiha.dotmod.feature.invsee.MinecraftTestBootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeDependencyTreeBuilderTest {
    private static ExactItemKey stone;
    private static ExactItemKey dirt;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
        stone = new ExactItemKey(new ItemStack(Items.STONE));
        dirt = new ExactItemKey(new ItemStack(Items.DIRT));
    }

    @Test
    void terminatesTwoItemCycle() {
        RecipeOption stoneRecipe = recipe("stone", stone, dirt);
        RecipeOption dirtRecipe = recipe("dirt", dirt, stone);
        Map<ExactItemKey, List<RecipeOption>> recipes = Map.of(
                stone, List.of(stoneRecipe), dirt, List.of(dirtRecipe)
        );

        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(12, 64).build(
                stone, 1, new InventoryCounter(List.of()), key -> recipes.getOrDefault(key, List.of())
        );

        RecipeDependencyTree.ItemNode cycle = tree.root().recipes().getFirst()
                .ingredients().getFirst().recipes().getFirst().ingredients().getFirst();
        assertEquals(RecipeDependencyTree.State.CYCLE, cycle.state());
    }

    @Test
    void enforcesDepthLimit() {
        RecipeOption stoneRecipe = recipe("stone", stone, dirt);
        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(1, 64).build(
                stone, 1, new InventoryCounter(List.of()),
                key -> key.equals(stone) ? List.of(stoneRecipe) : List.of()
        );

        assertEquals(
                RecipeDependencyTree.State.DEPTH_LIMIT,
                tree.root().recipes().getFirst().ingredients().getFirst().state()
        );
    }

    @Test
    void partialOutputStillExpandsItsMissingAmount() {
        RecipeOption recipe = recipe("stone", stone, dirt);
        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(12, 64).build(
                stone, 10, new InventoryCounter(List.of(new ItemStack(Items.STONE, 6))),
                key -> key.equals(stone) ? List.of(recipe) : List.of()
        );

        assertEquals(RecipeDependencyTree.State.RECIPE, tree.root().state());
        assertEquals(4, tree.root().recipes().getFirst().crafts());
    }

    @Test
    void inventoryBudgetIsNotReusedByRepeatedIngredients() {
        RecipeOption recipe = new RecipeOption("bricks", stone, 1, List.of(
                new RecipeIngredient(List.of(dirt)), new RecipeIngredient(List.of(dirt))
        ), true);
        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(12, 64).build(
                stone, 1, new InventoryCounter(List.of(new ItemStack(Items.DIRT))),
                key -> key.equals(stone) ? List.of(recipe) : List.of()
        );

        assertEquals(RecipeDependencyTree.State.AVAILABLE, tree.root().recipes().getFirst().ingredients().get(0).state());
        assertEquals(RecipeDependencyTree.State.NO_RECIPE, tree.root().recipes().getFirst().ingredients().get(1).state());
    }

    @Test
    void componentInsensitiveIngredientAcceptsComponentBearingStack() {
        ItemStack namedDirt = new ItemStack(Items.DIRT);
        namedDirt.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Named"));
        RecipeOption recipe = new RecipeOption("stone", stone, 1, List.of(
                new RecipeIngredient(List.of(dirt), false)
        ), true);
        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(12, 64).build(
                stone, 1, new InventoryCounter(List.of(namedDirt)),
                key -> key.equals(stone) ? List.of(recipe) : List.of()
        );

        assertEquals(RecipeDependencyTree.State.AVAILABLE, tree.root().recipes().getFirst().ingredients().getFirst().state());
    }

    @Test
    void nestedSiblingRecipesShareOneInventoryBudget() {
        ExactItemKey sand = new ExactItemKey(new ItemStack(Items.SAND));
        RecipeOption root = new RecipeOption("stone", stone, 1, List.of(
                new RecipeIngredient(List.of(dirt)), new RecipeIngredient(List.of(dirt))
        ), true);
        RecipeOption nested = recipe("dirt", dirt, sand);
        Map<ExactItemKey, List<RecipeOption>> recipes = Map.of(
                stone, List.of(root), dirt, List.of(nested)
        );

        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(12, 64).build(
                stone, 1, new InventoryCounter(List.of(new ItemStack(Items.SAND))),
                key -> recipes.getOrDefault(key, List.of())
        );

        List<RecipeDependencyTree.ItemNode> children = tree.root().recipes().getFirst().ingredients();
        assertEquals(
                RecipeDependencyTree.State.AVAILABLE,
                children.get(0).recipes().getFirst().ingredients().getFirst().state()
        );
        assertEquals(
                RecipeDependencyTree.State.NO_RECIPE,
                children.get(1).recipes().getFirst().ingredients().getFirst().state()
        );
    }

    @Test
    void returnedTreeNeverExceedsNodeLimit() {
        RecipeOption root = new RecipeOption("stone", stone, 1, List.of(
                new RecipeIngredient(List.of(dirt)),
                new RecipeIngredient(List.of(dirt)),
                new RecipeIngredient(List.of(dirt))
        ), true);
        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(12, 3).build(
                stone, 1, new InventoryCounter(List.of()),
                key -> key.equals(stone) ? List.of(root) : List.of()
        );

        assertEquals(3, countNodes(tree.root()));
    }

    @Test
    void constrainedIngredientKeepsScarceItemDuringRecursivePlanning() {
        ExactItemKey bricks = new ExactItemKey(new ItemStack(Items.STONE_BRICKS));
        ExactItemKey sand = new ExactItemKey(new ItemStack(Items.SAND));
        RecipeOption root = new RecipeOption("bricks", bricks, 1, List.of(
                new RecipeIngredient(List.of(stone, dirt)),
                new RecipeIngredient(List.of(stone))
        ), true);
        RecipeOption nested = recipe("dirt", dirt, sand);
        Map<ExactItemKey, List<RecipeOption>> recipes = Map.of(
                bricks, List.of(root), dirt, List.of(nested)
        );

        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(12, 64).build(
                bricks, 1, new InventoryCounter(List.of(
                        new ItemStack(Items.STONE), new ItemStack(Items.SAND)
                )), key -> recipes.getOrDefault(key, List.of())
        );

        List<RecipeDependencyTree.ItemNode> ingredients = tree.root().recipes().getFirst().ingredients();
        assertEquals(RecipeDependencyTree.State.RECIPE, ingredients.get(0).state());
        assertEquals(RecipeDependencyTree.State.AVAILABLE, ingredients.get(1).state());
    }

    @Test
    void nestedExpansionCannotExceedNodeLimit() {
        ExactItemKey sand = new ExactItemKey(new ItemStack(Items.SAND));
        RecipeOption root = new RecipeOption("stone", stone, 1, List.of(
                new RecipeIngredient(List.of(dirt)), new RecipeIngredient(List.of(dirt))
        ), true);
        RecipeOption nested = recipe("dirt", dirt, sand);
        Map<ExactItemKey, List<RecipeOption>> recipes = Map.of(
                stone, List.of(root), dirt, List.of(nested)
        );

        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(12, 5).build(
                stone, 1, new InventoryCounter(List.of()),
                key -> recipes.getOrDefault(key, List.of())
        );

        org.junit.jupiter.api.Assertions.assertTrue(countNodes(tree.root()) <= 5);
    }

    @Test
    void nestedRecipeSurplusSatisfiesRepeatedSiblings() {
        ExactItemKey bricks = new ExactItemKey(new ItemStack(Items.STONE_BRICKS));
        ExactItemKey planks = new ExactItemKey(new ItemStack(Items.OAK_PLANKS));
        ExactItemKey log = new ExactItemKey(new ItemStack(Items.OAK_LOG));
        RecipeIngredient plank = new RecipeIngredient(List.of(planks));
        RecipeOption root = new RecipeOption(
                "bricks", bricks, 1, List.of(plank, plank, plank, plank), true
        );
        RecipeOption nested = new RecipeOption(
                "planks", planks, 4, List.of(new RecipeIngredient(List.of(log))), true
        );
        Map<ExactItemKey, List<RecipeOption>> recipes = Map.of(
                bricks, List.of(root), planks, List.of(nested)
        );

        RecipeDependencyTree tree = new RecipeDependencyTreeBuilder(12, 64).build(
                bricks, 1, new InventoryCounter(List.of(new ItemStack(Items.OAK_LOG))),
                key -> recipes.getOrDefault(key, List.of())
        );

        List<RecipeDependencyTree.ItemNode> ingredients = tree.root().recipes().getFirst().ingredients();
        assertEquals(RecipeDependencyTree.State.RECIPE, ingredients.getFirst().state());
        assertEquals(3, ingredients.stream()
                .filter(node -> node.state() == RecipeDependencyTree.State.AVAILABLE)
                .count());
    }

    private static int countNodes(RecipeDependencyTree.ItemNode node) {
        int count = 1;
        for (RecipeDependencyTree.RecipeNode recipe : node.recipes()) {
            count++;
            for (RecipeDependencyTree.ItemNode ingredient : recipe.ingredients()) {
                count += countNodes(ingredient);
            }
        }
        return count;
    }

    private static RecipeOption recipe(String id, ExactItemKey output, ExactItemKey ingredient) {
        return new RecipeOption(id, output, 1, List.of(new RecipeIngredient(List.of(ingredient))), true);
    }
}
