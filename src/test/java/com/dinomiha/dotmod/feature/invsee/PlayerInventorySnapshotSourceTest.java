package com.dinomiha.dotmod.feature.invsee;

import com.dinomiha.dotmod.feature.invsee.source.PlayerInventorySnapshotSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerInventorySnapshotSourceTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void readsOnlyIndicesZeroThroughFortyAndCopiesStacks() {
        List<Integer> requested = new ArrayList<>();
        ItemStack source = new ItemStack(Items.STONE, 3);

        VirtualInventorySnapshot snapshot = PlayerInventorySnapshotSource.capture(index -> {
            requested.add(index);
            return index == 40 ? source : ItemStack.EMPTY;
        });
        source.setCount(1);

        assertEquals(41, requested.size());
        assertEquals(0, requested.getFirst());
        assertEquals(40, requested.getLast());
        assertEquals(3, snapshot.getStack(40).getCount());
    }
}
