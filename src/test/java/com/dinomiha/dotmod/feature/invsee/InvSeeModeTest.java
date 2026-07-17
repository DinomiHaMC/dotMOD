package com.dinomiha.dotmod.feature.invsee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeModeTest {
    @Test
    void capabilityMatrixKeepsViewReadOnlyAndCatalogCreativeOnly() {
        assertTrue(InvSeeMode.VIEW.allows(InvSeeCapability.TOOLTIP));
        assertTrue(InvSeeMode.VIEW.allows(InvSeeCapability.COPY_INFO));
        assertFalse(InvSeeMode.VIEW.allows(InvSeeCapability.MUTATE));
        assertFalse(InvSeeMode.VIEW.allows(InvSeeCapability.SAVE));
        assertTrue(InvSeeMode.EDIT.allows(InvSeeCapability.MUTATE));
        assertTrue(InvSeeMode.EDIT.allows(InvSeeCapability.SAVE));
        assertFalse(InvSeeMode.EDIT.allows(InvSeeCapability.CATALOG));
        assertTrue(InvSeeMode.CREATIVE.allows(InvSeeCapability.CATALOG));
    }
}
