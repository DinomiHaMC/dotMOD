package com.dinomiha.dotmod.feature.invsee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeLayoutTest {
    @Test
    void compactLayoutFitsSmallScreen() {
        InvSeeLayout layout = InvSeeLayout.compute(320, 240, true);
        assertTrue(layout.compact());
        assertFalse(layout.wide());
        assertTrue(layout.inventoryPanel().inside(320, 240));
        assertTrue(layout.catalogPanel().inside(320, 240));
        assertTrue(layout.status().inside(320, 240));
        assertTrue(layout.footer().inside(320, 240));
        assertTrue(layout.inventoryPanel().y() + layout.inventoryPanel().height() < layout.status().y());
    }

    @Test
    void wideLayoutSeparatesInventoryAndCatalog() {
        InvSeeLayout layout = InvSeeLayout.compute(854, 480, true);
        assertTrue(layout.wide());
        assertFalse(layout.compact());
        assertTrue(layout.inventoryPanel().x() + layout.inventoryPanel().width() < layout.catalogPanel().x());
        assertTrue(layout.inventoryPanel().inside(854, 480));
        assertTrue(layout.catalogPanel().inside(854, 480));
    }
}
