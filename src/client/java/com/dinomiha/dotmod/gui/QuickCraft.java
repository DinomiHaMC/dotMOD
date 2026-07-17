package com.dinomiha.dotmod.gui;

import com.dinomiha.dotmod.config.DotModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public final class QuickCraft {
    private QuickCraft() {
    }

    public static void perform(ScreenHandler handler) {
        MinecraftClient client = MinecraftClient.getInstance();
        DotModConfig config = DotModConfig.get();
        if (client.player == null || client.interactionManager == null || !config.modEnabled || !config.quickCraftEnabled) {
            return;
        }
        if (client.player.currentScreenHandler != handler || !handler.getCursorStack().isEmpty()) {
            return;
        }
        boolean playerInventory = handler instanceof PlayerScreenHandler;
        boolean craftingTable = handler instanceof CraftingScreenHandler;
        if (!playerInventory && !craftingTable) {
            return;
        }

        List<Integer> sources = playerInventory ? config.quickCraftSlots2x2 : config.quickCraftSlots3x3;
        int[] targetIds = playerInventory ? new int[]{1, 2, 3, 4} : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        int count = Math.min(sources.size(), targetIds.length);
        for (int i = 0; i < count; i++) {
            Integer logicalIndex = sources.get(i);
            if (logicalIndex == null) {
                continue;
            }
            int sourceSlotId = findPlayerInventorySlot(handler, client.player.getInventory(), logicalIndex);
            int targetSlotId = targetIds[i];
            if (sourceSlotId < 0 || targetSlotId >= handler.slots.size()) {
                continue;
            }
            Slot source = handler.getSlot(sourceSlotId);
            Slot target = handler.getSlot(targetSlotId);
            if (!source.hasStack() || target.hasStack()) {
                continue;
            }
            click(handler, sourceSlotId);
            click(handler, targetSlotId);
            if (!client.player.currentScreenHandler.getCursorStack().isEmpty()) {
                click(handler, sourceSlotId);
            }
        }
    }

    private static int findPlayerInventorySlot(ScreenHandler handler, PlayerInventory inventory, int logicalIndex) {
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot.inventory == inventory && slot.getIndex() == logicalIndex) {
                return i;
            }
        }
        return -1;
    }

    private static void click(ScreenHandler handler, int slotId) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, client.player);
    }
}
