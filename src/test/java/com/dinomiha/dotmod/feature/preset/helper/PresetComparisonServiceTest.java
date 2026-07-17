package com.dinomiha.dotmod.feature.preset.helper;

import com.dinomiha.dotmod.feature.invsee.MinecraftTestBootstrap;
import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetComparisonServiceTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void combinesPlayerAndContainerWithoutCountingExcess() {
        PresetProgress progress = new PresetComparisonService().compare(
                snapshot(new ItemStack(Items.STONE, 10), new ItemStack(Items.DIRT, 4)),
                InventoryCounter.combine(
                        List.of(new ItemStack(Items.STONE, 6)),
                        List.of(new ItemStack(Items.STONE, 8), new ItemStack(Items.DIRT, 1))
                )
        );

        assertEquals(14, progress.required());
        assertEquals(11, progress.available());
        assertEquals(3, progress.missing());
        assertEquals(78, progress.percentage());
        assertFalse(progress.complete());
    }

    @Test
    void comparesAllDataComponentsExactly() {
        ItemStack named = new ItemStack(Items.STONE, 2);
        named.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Named"));

        PresetProgress progress = new PresetComparisonService().compare(
                snapshot(named),
                new InventoryCounter(List.of(new ItemStack(Items.STONE, 64)))
        );

        assertEquals(2, progress.missing());
        assertEquals(java.util.Set.of(0), progress.missingSlots());
    }

    @Test
    void emptyPresetIsComplete() {
        PresetProgress progress = new PresetComparisonService().compare(
                VirtualInventorySnapshot.empty(), new InventoryCounter(List.of())
        );

        assertTrue(progress.complete());
        assertEquals(100, progress.percentage());
    }

    @Test
    void percentageDoesNotOverflowAtLongBoundary() {
        PresetRequirement requirement = new PresetRequirement(stoneKey(), Long.MAX_VALUE, Long.MAX_VALUE - 1L);

        assertEquals(99, requirement.percentage());
    }

    private static ExactItemKey stoneKey() {
        return new ExactItemKey(new ItemStack(Items.STONE));
    }

    private static VirtualInventorySnapshot snapshot(ItemStack... input) {
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        for (int index = 0; index < input.length; index++) {
            stacks.set(index, input[index]);
        }
        return new VirtualInventorySnapshot(stacks);
    }
}
