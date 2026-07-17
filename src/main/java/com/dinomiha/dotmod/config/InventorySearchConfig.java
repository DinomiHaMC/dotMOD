package com.dinomiha.dotmod.config;

public final class InventorySearchConfig {
    /** Nullable only before schema-aware validation distinguishes old missing fields from new defaults. */
    public Boolean enabled;
    public InventorySearchDisplayMode displayMode = InventorySearchDisplayMode.DIM;
}
