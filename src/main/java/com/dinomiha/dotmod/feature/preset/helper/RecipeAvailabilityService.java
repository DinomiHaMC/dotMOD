package com.dinomiha.dotmod.feature.preset.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Computes immediate recipe feasibility without moving items or requesting a craft. */
public final class RecipeAvailabilityService {
    public RecipeAvailability analyze(RecipeOption recipe, long missingOutput, InventoryCounter available) {
        if (recipe == null || available == null || missingOutput < 0L) {
            throw new IllegalArgumentException("Invalid recipe analysis input");
        }
        long craftsNeeded = divideCeil(missingOutput, recipe.outputCount());
        if (!recipe.requirementsKnown() || craftsNeeded == 0L) {
            return new RecipeAvailability(recipe, craftsNeeded, 0L, List.of());
        }

        IngredientAllocation needed = allocate(recipe.ingredients(), craftsNeeded, available.entries());
        long low = 0L;
        long relevantItems = saturatingSum(available.entries().stream()
                .filter(entry -> recipe.ingredients().stream().anyMatch(ingredient -> ingredient.accepts(entry.key())))
                .mapToLong(InventoryCounter.Entry::count)
                .toArray());
        long high = relevantItems / recipe.ingredients().size();
        while (low < high) {
            long distance = high - low;
            long candidate = low + distance / 2L + distance % 2L;
            if (allocate(recipe.ingredients(), candidate, available.entries()).complete()) {
                low = candidate;
            } else {
                high = candidate - 1L;
            }
        }

        List<RecipeAvailability.IngredientStatus> statuses = new ArrayList<>();
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            long allocated = needed.allocated(index);
            statuses.add(new RecipeAvailability.IngredientStatus(
                    recipe.ingredients().get(index), craftsNeeded, allocated, craftsNeeded - allocated
            ));
        }
        return new RecipeAvailability(recipe, craftsNeeded, low, statuses);
    }

    static IngredientAllocation allocate(
            List<RecipeIngredient> ingredients,
            long crafts,
            List<InventoryCounter.Entry> available
    ) {
        if (crafts == 0L) {
            return new IngredientAllocation(
                    true, Collections.nCopies(ingredients.size(), Map.of())
            );
        }
        List<InventoryCounter.Entry> variants = available.stream()
                .filter(entry -> ingredients.stream().anyMatch(ingredient -> ingredient.accepts(entry.key())))
                .toList();
        int source = 0;
        int variantStart = 1;
        int ingredientStart = variantStart + variants.size();
        int sink = ingredientStart + ingredients.size();
        FlowNetwork network = new FlowNetwork(sink + 1);
        FlowNetwork.Edge[][] allocations = new FlowNetwork.Edge[variants.size()][ingredients.size()];

        for (int variant = 0; variant < variants.size(); variant++) {
            network.addEdge(source, variantStart + variant, variants.get(variant).count());
            List<Integer> ingredientOrder = IntStream.range(0, ingredients.size()).boxed()
                    .sorted(java.util.Comparator
                            .comparingInt((Integer index) -> ingredients.get(index).alternatives().size())
                            .thenComparingInt(Integer::intValue))
                    .toList();
            for (int ingredient : ingredientOrder) {
                if (ingredients.get(ingredient).accepts(variants.get(variant).key())) {
                    allocations[variant][ingredient] = network.addEdge(
                            variantStart + variant,
                            ingredientStart + ingredient,
                            variants.get(variant).count()
                    );
                }
            }
        }
        List<FlowNetwork.Edge> demandEdges = new ArrayList<>();
        for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
            demandEdges.add(network.addEdge(ingredientStart + ingredient, sink, crafts));
        }
        long flowLimit = saturatingSum(variants.stream().mapToLong(InventoryCounter.Entry::count).toArray());
        network.maxFlow(source, sink, flowLimit);
        List<Map<ExactItemKey, Long>> assignments = new ArrayList<>();
        for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
            LinkedHashMap<ExactItemKey, Long> assigned = new LinkedHashMap<>();
            for (int variant = 0; variant < variants.size(); variant++) {
                FlowNetwork.Edge edge = allocations[variant][ingredient];
                if (edge != null && edge.reverse.capacity > 0L) {
                    assigned.put(variants.get(variant).key(), edge.reverse.capacity);
                }
            }
            assignments.add(Collections.unmodifiableMap(assigned));
        }
        return new IngredientAllocation(
                demandEdges.stream().allMatch(edge -> edge.capacity == 0L), assignments
        );
    }

    private static long divideCeil(long value, long divisor) {
        return value == 0L ? 0L : 1L + (value - 1L) / divisor;
    }

    private static long saturatingSum(long[] values) {
        long total = 0L;
        for (long value : values) {
            if (Long.MAX_VALUE - total < value) {
                return Long.MAX_VALUE;
            }
            total += value;
        }
        return total;
    }

    record IngredientAllocation(boolean complete, List<Map<ExactItemKey, Long>> assignments) {
        IngredientAllocation {
            assignments = List.copyOf(assignments);
        }

        long allocated(int ingredient) {
            return saturatingSum(assignments.get(ingredient).values().stream().mapToLong(Long::longValue).toArray());
        }
    }

    private static final class FlowNetwork {
        private final List<List<Edge>> edges;

        private FlowNetwork(int vertices) {
            edges = new ArrayList<>(vertices);
            for (int index = 0; index < vertices; index++) {
                edges.add(new ArrayList<>());
            }
        }

        private Edge addEdge(int from, int to, long capacity) {
            Edge forward = new Edge(to, capacity);
            Edge reverse = new Edge(from, 0L);
            forward.reverse = reverse;
            reverse.reverse = forward;
            edges.get(from).add(forward);
            edges.get(to).add(reverse);
            return forward;
        }

        private long maxFlow(int source, int sink, long limit) {
            long total = 0L;
            while (total < limit) {
                boolean[] visited = new boolean[edges.size()];
                long added = augment(source, sink, limit - total, visited);
                if (added == 0L) {
                    break;
                }
                total += added;
            }
            return total;
        }

        private long augment(int vertex, int sink, long amount, boolean[] visited) {
            if (vertex == sink) {
                return amount;
            }
            visited[vertex] = true;
            for (Edge edge : edges.get(vertex)) {
                if (edge.capacity == 0L || visited[edge.to]) {
                    continue;
                }
                long pushed = augment(edge.to, sink, Math.min(amount, edge.capacity), visited);
                if (pushed > 0L) {
                    edge.capacity -= pushed;
                    edge.reverse.capacity += pushed;
                    return pushed;
                }
            }
            return 0L;
        }

        private static final class Edge {
            private final int to;
            private long capacity;
            private Edge reverse;

            private Edge(int to, long capacity) {
                this.to = to;
                this.capacity = capacity;
            }
        }
    }
}
