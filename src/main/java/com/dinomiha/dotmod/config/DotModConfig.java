package com.dinomiha.dotmod.config;

import com.google.gson.annotations.SerializedName;

public final class DotModConfig {
    public static final int CURRENT_SCHEMA_VERSION = 8;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public GeneralConfig general = new GeneralConfig();
    public CommandsConfig commands = new CommandsConfig();
    public HudConfig hud = new HudConfig();
    public QuickCraftConfig quickCraft = new QuickCraftConfig();
    public InventoryPresetsConfig inventoryPresets = new InventoryPresetsConfig();
    public InventorySearchConfig inventorySearch = new InventorySearchConfig();
    public DurabilityConfig durability = new DurabilityConfig();
    public FeatureConfig screenshots = new FeatureConfig();
    public FeatureConfig deathHistory = new FeatureConfig();
    public ToggleWalkConfig toggleWalk = new ToggleWalkConfig();
    public FreelookConfig freelook = new FreelookConfig();
    public FeatureConfig fullBrightness = new FeatureConfig();
    public PlayerColorsConfig playerColors = new PlayerColorsConfig();
    public FeatureConfig commandAliases = new FeatureConfig();
    public KeybindsConfig keybinds = new KeybindsConfig();

    @SerializedName("interface")
    public InterfaceConfig interfaceConfig = new InterfaceConfig();

    public void validate() {
        ConfigValidator.validate(this);
    }

    public void replaceWith(DotModConfig source) {
        schemaVersion = source.schemaVersion;
        general = source.general;
        commands = source.commands;
        hud = source.hud;
        quickCraft = source.quickCraft;
        inventoryPresets = source.inventoryPresets;
        inventorySearch = source.inventorySearch;
        durability = source.durability;
        screenshots = source.screenshots;
        deathHistory = source.deathHistory;
        toggleWalk = source.toggleWalk;
        freelook = source.freelook;
        fullBrightness = source.fullBrightness;
        playerColors = source.playerColors;
        commandAliases = source.commandAliases;
        keybinds = source.keybinds;
        interfaceConfig = source.interfaceConfig;
    }

    public static final class HudOffset {
        public int dx;
        public int dy;
    }
}
