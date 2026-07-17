package com.dinomiha.dotmod.feature.inventorysearch.query;

import java.util.Locale;

public enum FilterType {
    TEXT(false),
    ID(false),
    LORE(false),
    ENCHANTMENT(false),
    DURABILITY(true),
    COUNT(true),
    ALL_TEXT(false);

    private final boolean numeric;

    FilterType(boolean numeric) {
        this.numeric = numeric;
    }

    public boolean numeric() {
        return numeric;
    }

    public static FilterType parse(String value) {
        String key = value.toLowerCase(Locale.ROOT).replace('-', '_');
        try {
            return valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
