package com.dinomiha.dotmod.feature.invsee.catalog;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LocalItemCatalog {
    private final List<CatalogEntry> entries;

    private LocalItemCatalog(List<CatalogEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public static LocalItemCatalog create() {
        List<CatalogEntry> entries = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item != Items.AIR) {
                ItemStack stack = new ItemStack(item);
                entries.add(new CatalogEntry(Registries.ITEM.getId(item), stack));
            }
        }
        return new LocalItemCatalog(entries);
    }

    public List<CatalogEntry> search(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return entries;
        }
        String[] tokens = normalized.split("\\s+");
        return entries.stream().filter(entry -> entry.matches(tokens)).toList();
    }
}
