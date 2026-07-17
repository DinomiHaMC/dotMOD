package com.dinomiha.dotmod.feature.invsee.source;

import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public final class PlayerInventorySnapshotSource {
    private PlayerInventorySnapshotSource() {
    }

    public static VirtualInventorySnapshot capture(ClientPlayerEntity player) {
        return capture(index -> player.getInventory().getStack(index));
    }

    public static VirtualInventorySnapshot capture(IntFunction<ItemStack> source) {
        List<ItemStack> stacks = new ArrayList<>(VirtualInventory.SLOT_COUNT);
        for (int index = 0; index < VirtualInventory.SLOT_COUNT; index++) {
            stacks.add(source.apply(index).copy());
        }
        return new VirtualInventorySnapshot(stacks);
    }
}
