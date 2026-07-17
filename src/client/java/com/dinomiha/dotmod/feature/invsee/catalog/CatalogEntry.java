package com.dinomiha.dotmod.feature.invsee.catalog;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class CatalogEntry {
    private final Identifier id;
    private final ItemStack stack;
    private final String searchText;

    public CatalogEntry(Identifier id, ItemStack stack) {
        this.id = id;
        this.stack = stack.copy();
        this.searchText = (id + " " + stack.getName().getString()).toLowerCase(Locale.ROOT);
    }

    public Identifier id() {
        return id;
    }

    public ItemStack stack() {
        return stack.copy();
    }

    boolean matches(String[] tokens) {
        for (String token : tokens) {
            if (!searchText.contains(token)) {
                return false;
            }
        }
        return true;
    }
}
