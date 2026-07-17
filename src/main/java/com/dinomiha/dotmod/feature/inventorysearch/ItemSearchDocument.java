package com.dinomiha.dotmod.feature.inventorysearch;

import com.dinomiha.dotmod.feature.inventorysearch.query.QueryParser;

import java.util.ArrayList;
import java.util.List;

public record ItemSearchDocument(
        String displayName,
        String itemId,
        List<String> lore,
        List<String> enchantments,
        List<String> allText,
        int count,
        Integer durabilityPercent
) {
    public ItemSearchDocument {
        displayName = bounded(displayName, 256);
        itemId = bounded(itemId, 128);
        lore = normalize(lore, 32, 4096);
        enchantments = normalize(enchantments, 128, 4096);
        allText = normalize(allText, 256, 8192);
        if (count < 0 || durabilityPercent != null && (durabilityPercent < 0 || durabilityPercent > 100)) {
            throw new IllegalArgumentException("Invalid item search document");
        }
    }

    public static ItemSearchDocument of(
            String displayName,
            String itemId,
            List<String> lore,
            List<String> enchantments,
            List<String> tooltip,
            int count,
            Integer durabilityPercent
    ) {
        List<String> all = new ArrayList<>();
        all.add(displayName);
        all.add(itemId);
        all.addAll(lore);
        all.addAll(enchantments);
        all.addAll(tooltip);
        return new ItemSearchDocument(displayName, itemId, lore, enchantments, all, count, durabilityPercent);
    }

    private static List<String> normalize(List<String> input, int maxEntries, int totalCodePoints) {
        if (input == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        int remaining = totalCodePoints;
        for (String raw : input) {
            if (raw == null || result.size() >= maxEntries || remaining <= 0) {
                continue;
            }
            String value = bounded(raw, Math.min(256, remaining));
            if (!value.isEmpty()) {
                result.add(value);
                remaining -= value.codePointCount(0, value.length());
            }
        }
        return List.copyOf(result);
    }

    private static String bounded(String input, int maxCodePoints) {
        String normalized = QueryParser.normalize(input);
        int count = normalized.codePointCount(0, normalized.length());
        if (count <= maxCodePoints) {
            return normalized;
        }
        return normalized.substring(0, normalized.offsetByCodePoints(0, maxCodePoints));
    }
}
