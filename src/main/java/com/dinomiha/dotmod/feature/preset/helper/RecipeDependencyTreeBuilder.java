package com.dinomiha.dotmod.feature.preset.helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Builds one bounded explanatory recipe plan without mutating real inventory. */
public final class RecipeDependencyTreeBuilder {
    private final int maxDepth;
    private final int maxNodes;

    public RecipeDependencyTreeBuilder(int maxDepth, int maxNodes) {
        if (maxDepth < 1 || maxNodes < 1) {
            throw new IllegalArgumentException("Recipe tree limits must be positive");
        }
        this.maxDepth = maxDepth;
        this.maxNodes = maxNodes;
    }

    public RecipeDependencyTree build(
            ExactItemKey item,
            long required,
            InventoryCounter available,
            RecipeCatalog catalog
    ) {
        return build(item, required, available, catalog, null);
    }

    public RecipeDependencyTree build(
            ExactItemKey item,
            long required,
            InventoryCounter available,
            RecipeCatalog catalog,
            RecipeOption rootRecipe
    ) {
        if (item == null || required <= 0L || available == null || catalog == null) {
            throw new IllegalArgumentException("Invalid recipe tree input");
        }
        return new RecipeDependencyTree(expand(
                item,
                required,
                new Budget(available),
                catalog,
                new RecipeCycleGuard(),
                0,
                new NodeBudget(maxNodes),
                rootRecipe
        ));
    }

    private RecipeDependencyTree.ItemNode expand(
            ExactItemKey item,
            long required,
            Budget inventory,
            RecipeCatalog catalog,
            RecipeCycleGuard guard,
            int depth,
            NodeBudget nodes,
            RecipeOption preferredRecipe
    ) {
        if (!nodes.claim()) {
            return leaf(item, required, RecipeDependencyTree.State.NODE_LIMIT);
        }
        long available = inventory.consumeExact(item, required);
        if (available >= required) {
            return leaf(item, required, RecipeDependencyTree.State.AVAILABLE);
        }
        if (depth >= maxDepth) {
            return leaf(item, required, RecipeDependencyTree.State.DEPTH_LIMIT);
        }
        if (!guard.enter(item)) {
            return leaf(item, required, RecipeDependencyTree.State.CYCLE);
        }
        try {
            List<RecipeOption> options = catalog.recipesFor(item);
            if (options.isEmpty()) {
                return leaf(item, required, RecipeDependencyTree.State.NO_RECIPE);
            }
            RecipeOption recipe = chooseRecipe(options, preferredRecipe);
            if (recipe == null) {
                return leaf(item, required, RecipeDependencyTree.State.UNKNOWN_REQUIREMENTS);
            }
            // A recipe node and at least one ingredient/limit node must fit.
            if (nodes.remaining() < 2) {
                return leaf(item, required, RecipeDependencyTree.State.NODE_LIMIT);
            }
            nodes.claim();
            long missing = required - available;
            long crafts = 1L + (missing - 1L) / recipe.outputCount();
            RecipeAvailabilityService.IngredientAllocation allocation =
                    RecipeAvailabilityService.allocate(recipe.ingredients(), crafts, inventory.entries());
            inventory.consume(allocation.assignments());

            List<RecipeDependencyTree.ItemNode> ingredients = new ArrayList<>();
            for (int index = 0; index < recipe.ingredients().size(); index++) {
                if (nodes.remaining() == 0) {
                    break;
                }
                RecipeIngredient ingredient = recipe.ingredients().get(index);
                Map<ExactItemKey, Long> assigned = allocation.assignments().get(index);
                long allocated = allocation.allocated(index);
                ExactItemKey display = assigned.keySet().stream().findFirst()
                        .orElseGet(() -> chooseAlternative(ingredient, inventory, catalog));
                boolean hasMore = index + 1 < recipe.ingredients().size();
                if (nodes.remaining() == 1 && hasMore) {
                    nodes.claim();
                    ingredients.add(leaf(display, crafts - allocated, RecipeDependencyTree.State.NODE_LIMIT));
                    break;
                }
                if (allocated >= crafts) {
                    if (!nodes.claim()) {
                        break;
                    }
                    ingredients.add(leaf(display, crafts, RecipeDependencyTree.State.AVAILABLE));
                } else {
                    Budget branch = inventory.copy();
                    RecipeDependencyTree.ItemNode child = expand(
                            chooseAlternative(ingredient, inventory, catalog),
                            crafts - allocated,
                            branch,
                            catalog,
                            guard,
                            depth + 1,
                            nodes,
                            null
                    );
                    if (isSatisfiable(child)) {
                        inventory.replaceWith(branch);
                    }
                    ingredients.add(child);
                }
            }
            RecipeDependencyTree.RecipeNode recipeNode =
                    new RecipeDependencyTree.RecipeNode(recipe, crafts, ingredients);
            if (ingredients.size() == recipe.ingredients().size()
                    && ingredients.stream().allMatch(RecipeDependencyTreeBuilder::isSatisfiable)) {
                long remainder = missing % recipe.outputCount();
                long surplus = remainder == 0L ? 0L : recipe.outputCount() - remainder;
                inventory.add(item, surplus);
            }
            return new RecipeDependencyTree.ItemNode(
                    item, required, RecipeDependencyTree.State.RECIPE, List.of(recipeNode)
            );
        } finally {
            guard.leave(item);
        }
    }

    private static RecipeOption chooseRecipe(List<RecipeOption> options, RecipeOption preferred) {
        if (preferred != null && preferred.requirementsKnown() && options.contains(preferred)) {
            return preferred;
        }
        return options.stream().filter(RecipeOption::requirementsKnown).findFirst().orElse(null);
    }

    private static ExactItemKey chooseAlternative(
            RecipeIngredient ingredient,
            Budget inventory,
            RecipeCatalog catalog
    ) {
        return ingredient.alternatives().stream()
                .max(Comparator
                        .comparingInt((ExactItemKey key) -> catalog.recipesFor(key).stream()
                                .anyMatch(RecipeOption::requirementsKnown) ? 1 : 0)
                        .thenComparingLong(inventory::count))
                .orElseThrow();
    }

    private static RecipeDependencyTree.ItemNode leaf(
            ExactItemKey item,
            long required,
            RecipeDependencyTree.State state
    ) {
        return new RecipeDependencyTree.ItemNode(item, required, state, List.of());
    }

    private static boolean isSatisfiable(RecipeDependencyTree.ItemNode node) {
        if (node.state() == RecipeDependencyTree.State.AVAILABLE) {
            return true;
        }
        if (node.state() != RecipeDependencyTree.State.RECIPE || node.recipes().size() != 1) {
            return false;
        }
        RecipeDependencyTree.RecipeNode recipe = node.recipes().getFirst();
        return recipe.ingredients().size() == recipe.recipe().ingredients().size()
                && recipe.ingredients().stream().allMatch(RecipeDependencyTreeBuilder::isSatisfiable);
    }

    private static final class NodeBudget {
        private final int limit;
        private int used;

        private NodeBudget(int limit) {
            this.limit = limit;
        }

        private boolean claim() {
            if (used >= limit) {
                return false;
            }
            used++;
            return true;
        }

        private int remaining() {
            return limit - used;
        }
    }

    private static final class Budget {
        private final ArrayList<MutableCount> counts;

        private Budget(InventoryCounter source) {
            counts = source.entries().stream()
                    .map(entry -> new MutableCount(entry.key(), entry.count()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        private Budget(Budget source) {
            counts = source.counts.stream()
                    .map(MutableCount::copy)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        private Budget copy() {
            return new Budget(this);
        }

        private void replaceWith(Budget source) {
            counts.clear();
            source.counts.stream().map(MutableCount::copy).forEach(counts::add);
        }

        private void add(ExactItemKey key, long amount) {
            if (amount <= 0L) {
                return;
            }
            MutableCount entry = counts.stream()
                    .filter(candidate -> candidate.key.equals(key))
                    .findFirst()
                    .orElse(null);
            if (entry == null) {
                counts.add(new MutableCount(key, amount));
            } else {
                entry.count = Long.MAX_VALUE - entry.count < amount
                        ? Long.MAX_VALUE
                        : entry.count + amount;
            }
        }

        private List<InventoryCounter.Entry> entries() {
            return counts.stream()
                    .filter(entry -> entry.count > 0L)
                    .map(entry -> new InventoryCounter.Entry(entry.key, entry.count))
                    .toList();
        }

        private long count(ExactItemKey key) {
            return counts.stream()
                    .filter(entry -> entry.key.equals(key))
                    .mapToLong(entry -> entry.count)
                    .sum();
        }

        private long consumeExact(ExactItemKey key, long requested) {
            MutableCount entry = counts.stream()
                    .filter(candidate -> candidate.key.equals(key))
                    .findFirst()
                    .orElse(null);
            if (entry == null) {
                return 0L;
            }
            long consumed = Math.min(requested, entry.count);
            entry.count -= consumed;
            return consumed;
        }

        private void consume(List<Map<ExactItemKey, Long>> assignments) {
            for (Map<ExactItemKey, Long> assignment : assignments) {
                for (Map.Entry<ExactItemKey, Long> entry : assignment.entrySet()) {
                    consumeExact(entry.getKey(), entry.getValue());
                }
            }
        }

        private static final class MutableCount {
            private final ExactItemKey key;
            private long count;

            private MutableCount(ExactItemKey key, long count) {
                this.key = key;
                this.count = count;
            }

            private MutableCount copy() {
                return new MutableCount(key, count);
            }
        }
    }
}
