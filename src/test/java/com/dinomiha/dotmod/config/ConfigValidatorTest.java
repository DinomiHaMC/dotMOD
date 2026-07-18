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
        assertEquals(9, migrated.schemaVersion);
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
    void schemaSixMigrationEnablesPreviouslyHiddenSearchAndDurability() {
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
        assertTrue(disabled.inventorySearch.enabled);
        assertTrue(missing.inventorySearch.enabled);
        assertTrue(explicitNull.inventorySearch.enabled);
        assertTrue(disabled.durability.enabled);
        assertTrue(disabled.commandAliases.enabled);
        assertTrue(disabled.deathHistory.enabled);
        assertTrue(disabled.screenshots.enabled);
    }

    @Test
    void currentSchemaPreservesExplicitFeatureChoices() {
        Gson gson = new Gson();
        DotModConfig current = gson.fromJson(
                "{\"schemaVersion\":6,\"inventorySearch\":{\"enabled\":false},\"durability\":{\"enabled\":false},\"commandAliases\":{\"enabled\":false},\"deathHistory\":{\"enabled\":false},\"screenshots\":{\"enabled\":false}}",
                DotModConfig.class
        );

        current.validate();

        assertFalse(current.inventorySearch.enabled);
        assertFalse(current.durability.enabled);
        assertFalse(current.commandAliases.enabled);
        assertFalse(current.deathHistory.enabled);
        assertFalse(current.screenshots.enabled);
    }

    @Test
    void schemaSevenEnablesNewFeaturesButPreservesCurrentExplicitFalse() {
        Gson gson = new Gson();
        DotModConfig migrated = gson.fromJson("{\"schemaVersion\":6,\"toggleWalk\":{\"enabled\":false},\"freelook\":{\"enabled\":false}}", DotModConfig.class);
        DotModConfig current = gson.fromJson("{\"schemaVersion\":7,\"toggleWalk\":{\"enabled\":false},\"freelook\":{\"enabled\":false}}", DotModConfig.class);

        migrated.validate();
        current.validate();

        assertTrue(migrated.toggleWalk.enabled);
        assertTrue(migrated.freelook.enabled);
        assertFalse(current.toggleWalk.enabled);
        assertFalse(current.freelook.enabled);
    }

    @Test
    void schemaEightEnablesFullBrightnessAndEnforcesFreelookPerspectiveMigration() {
        Gson gson = new Gson();
        DotModConfig migrated = gson.fromJson(
                "{\"schemaVersion\":7,\"fullBrightness\":{\"enabled\":false},\"freelook\":{\"perspective\":\"PRESERVE\"}}",
                DotModConfig.class
        );
        DotModConfig current = gson.fromJson(
                "{\"schemaVersion\":8,\"fullBrightness\":{\"enabled\":false}}",
                DotModConfig.class
        );

        migrated.validate();
        current.validate();

        assertTrue(migrated.fullBrightness.enabled);
        assertEquals(FreelookPerspective.SWITCH_TO_THIRD_PERSON_BACK, migrated.freelook.perspective);
        assertFalse(current.fullBrightness.enabled);
    }

    @Test
    void schemaEightPreservesMovementWidgetCenterAfterWidthIncrease() {
        Gson gson = new Gson();
        DotModConfig migrated = gson.fromJson(
                "{\"schemaVersion\":7,\"hud\":{\"widgets\":{\"dotmod.movement\":{"
                        + "\"visible\":true,\"anchor\":\"BOTTOM_CENTER\",\"offsetX\":10,\"offsetY\":-62,"
                        + "\"scale\":1.0,\"alpha\":1.0}}}}",
                DotModConfig.class
        );

        migrated.validate();

        assertEquals(-50, migrated.hud.widgets.get("dotmod.movement").offsetX);
    }

    @Test
    void schemaNineEnablesToggleSprintButPreservesCurrentExplicitFalse() {
        Gson gson = new Gson();
        DotModConfig migrated = gson.fromJson(
                "{\"schemaVersion\":8,\"toggleWalk\":{\"toggleSprint\":{\"enabled\":false}}}",
                DotModConfig.class
        );
        DotModConfig current = gson.fromJson(
                "{\"schemaVersion\":9,\"toggleWalk\":{\"toggleSprint\":{\"enabled\":false}}}",
                DotModConfig.class
        );

        migrated.validate();
        current.validate();

        assertTrue(migrated.toggleWalk.toggleSprint.enabled);
        assertFalse(current.toggleWalk.toggleSprint.enabled);
    }
}
