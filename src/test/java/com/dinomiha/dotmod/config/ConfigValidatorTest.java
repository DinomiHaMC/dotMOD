package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.hud.widget.HudWidgetDefaults;
import com.dinomiha.dotmod.hud.widget.HudWidgetSettings;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValidatorTest {
    @Test
    void restoresMissingCategoriesAndClampsInvalidValues() {
        DotModConfig config = new DotModConfig();
        config.general = null;
        config.interfaceConfig = null;
        config.quickCraft.slots2x2 = List.of(-1, 9, 9, 36);
        config.hud.gridSize = 0;
        config.hud.magneticSnapDistance = 50;
        config.hud.uniformNameTags.size = Float.NaN;
        config.playerColors.greenColor = "invalid";

        config.validate();

        assertNotNull(config.general);
        assertNotNull(config.interfaceConfig);
        assertEquals(List.of(9), config.quickCraft.slots2x2);
        assertEquals(1, config.hud.gridSize);
        assertEquals(16, config.hud.magneticSnapDistance);
        assertEquals(1.0F, config.hud.uniformNameTags.size);
        assertEquals("#55FF55", config.playerColors.greenColor);
        assertEquals(HudWidgetDefaults.definitions().size(), config.hud.widgets.size());
        assertEquals(DotModConfig.CURRENT_SCHEMA_VERSION, config.schemaVersion);
    }

    @Test
    void rejectsNewerSchemaWithoutDowngradingIt() {
        DotModConfig config = new DotModConfig();
        config.schemaVersion = DotModConfig.CURRENT_SCHEMA_VERSION + 1;

        assertThrows(UnsupportedDataVersionException.class, config::validate);
        assertEquals(DotModConfig.CURRENT_SCHEMA_VERSION + 1, config.schemaVersion);
    }

    @Test
    void reloadCanReplaceValuesWithoutInvalidatingRootReference() {
        DotModConfig current = new DotModConfig();
        DotModConfig loaded = new DotModConfig();
        loaded.general.enabled = false;

        current.replaceWith(loaded);

        assertFalse(current.general.enabled);
    }

    @Test
    void schemaMigrationPreservesExplicitPresetFeatureChoice() {
        DotModConfig migrated = new DotModConfig();
        migrated.schemaVersion = 2;
        migrated.inventoryPresets.enabled = false;
        migrated.validate();
        assertFalse(migrated.inventoryPresets.enabled);

        DotModConfig current = new DotModConfig();
        current.schemaVersion = 3;
        current.inventoryPresets.enabled = false;
        current.validate();
        assertFalse(current.inventoryPresets.enabled);
    }

    @Test
    void schemaFourMigrationPreservesLegacyHudPositionAndDurabilityChoice() {
        DotModConfig migrated = new DotModConfig();
        migrated.schemaVersion = 3;
        migrated.durability.enabled = true;
        migrated.hud.widgets.put("addon.legacy", new HudWidgetSettings());
        migrated.hud.offset(HudElement.HEARTS).dx = 7;
        migrated.hud.offset(HudElement.HEARTS).dy = -3;

        migrated.validate();

        var hearts = migrated.hud.widget(HudWidgetDefaults.HEARTS);
        assertEquals(-84, hearts.offsetX);
        assertEquals(-42, hearts.offsetY);
        assertTrue(migrated.durability.enabled);
        assertTrue(migrated.hud.widgets.containsKey("addon.legacy"));
        assertEquals(5, migrated.schemaVersion);
    }

    @Test
    void validatesWidgetAndDurabilitySettingsWithoutDroppingUnknownIds() {
        DotModConfig config = new DotModConfig();
        HudWidgetSettings unknown = new HudWidgetSettings();
        unknown.scale = Float.NaN;
        unknown.alpha = 5.0F;
        unknown.anchor = null;
        config.hud.widgets.put("addon.widget", unknown);
        config.durability.warningThreshold = Float.POSITIVE_INFINITY;
        config.durability.warningCooldownSeconds = -10;
        config.durability.lowColor = "bad";

        config.validate();

        assertTrue(config.hud.widgets.containsKey("addon.widget"));
        assertEquals(1.0F, config.hud.widgets.get("addon.widget").scale);
        assertEquals(1.0F, config.hud.widgets.get("addon.widget").alpha);
        assertNotNull(config.hud.widgets.get("addon.widget").anchor);
        assertEquals(0.15F, config.durability.warningThreshold);
        assertEquals(0, config.durability.warningCooldownSeconds);
        assertEquals("#FF5555", config.durability.lowColor);

        config.hud.resetOffsets();
        assertTrue(config.hud.widgets.containsKey("addon.widget"));
    }

    @Test
    void schemaFiveMigrationPreservesExplicitSearchChoiceAndOldMissingDefault() {
        Gson gson = new Gson();
        DotModConfig enabled = gson.fromJson("{\"schemaVersion\":4,\"inventorySearch\":{\"enabled\":true}}", DotModConfig.class);
        DotModConfig disabled = gson.fromJson("{\"schemaVersion\":4,\"inventorySearch\":{\"enabled\":false}}", DotModConfig.class);
        DotModConfig missing = gson.fromJson("{\"schemaVersion\":4}", DotModConfig.class);
        DotModConfig explicitNull = gson.fromJson("{\"schemaVersion\":4,\"inventorySearch\":null}", DotModConfig.class);

        enabled.validate();
        disabled.validate();
        missing.validate();
        explicitNull.validate();

        assertTrue(enabled.inventorySearch.enabled);
        assertFalse(disabled.inventorySearch.enabled);
        assertFalse(missing.inventorySearch.enabled);
        assertFalse(explicitNull.inventorySearch.enabled);
    }
}
