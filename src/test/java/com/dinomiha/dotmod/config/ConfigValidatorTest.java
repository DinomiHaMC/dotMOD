package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertEquals(HudElement.values().length, config.hud.offsets.size());
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
}
