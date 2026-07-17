package com.dinomiha.dotmod.feature.invsee;

import com.dinomiha.dotmod.feature.invsee.catalog.LocalItemCatalog;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalItemCatalogTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void searchesLocalCatalogByIdentifierTokens() {
        LocalItemCatalog catalog = LocalItemCatalog.create();

        assertFalse(catalog.search("").isEmpty());
        assertTrue(catalog.search("minecraft:diamond").stream()
                .anyMatch(entry -> entry.stack().isOf(Items.DIAMOND)));
        assertTrue(catalog.search("minecraft diamond").stream()
                .anyMatch(entry -> entry.stack().isOf(Items.DIAMOND)));
    }
}
