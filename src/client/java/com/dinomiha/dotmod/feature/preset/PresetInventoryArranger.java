package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/** Best-effort whole-stack rearrangement using only normal synchronized clicks. */
public final class PresetInventoryArranger {
    private PresetInventoryArranger() {
    }

    public static Result arrange(MinecraftClient client, VirtualInventorySnapshot target) {
        if (client.player == null || client.interactionManager == null || target == null) {
            return Result.unavailable();
        }
        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler != client.player.playerScreenHandler || !handler.getCursorStack().isEmpty()) {
            return Result.unavailable();
        }
        PlayerInventory inventory = client.player.getInventory();
        int moved = 0;
        for (int targetIndex = 0; targetIndex < VirtualInventory.SLOT_COUNT; targetIndex++) {
            ItemStack wanted = target.getStack(targetIndex);
            if (wanted.isEmpty() || ItemStack.areEqual(inventory.getStack(targetIndex), wanted)) {
                continue;
            }
            int sourceIndex = sourceFor(inventory, target, wanted, targetIndex);
            if (sourceIndex < 0 || !swap(client, handler, inventory, sourceIndex, targetIndex)) {
                continue;
            }
            moved++;
        }
        int matched = 0;
        for (int index = 0; index < VirtualInventory.SLOT_COUNT; index++) {
            if (ItemStack.areEqual(inventory.getStack(index), target.getStack(index))) {
                matched++;
            }
        }
        return new Result(true, moved, matched, VirtualInventory.SLOT_COUNT - matched);
    }

    private static int sourceFor(
            PlayerInventory inventory,
            VirtualInventorySnapshot target,
            ItemStack wanted,
            int targetIndex
    ) {
        for (int index = 0; index < VirtualInventory.SLOT_COUNT; index++) {
            if (index != targetIndex
                    && ItemStack.areEqual(inventory.getStack(index), wanted)
                    && !ItemStack.areEqual(inventory.getStack(index), target.getStack(index))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean swap(
            MinecraftClient client,
            ScreenHandler handler,
            PlayerInventory inventory,
            int sourceIndex,
            int targetIndex
    ) {
        int sourceId = slotId(handler, inventory, sourceIndex);
        int targetId = slotId(handler, inventory, targetIndex);
        if (sourceId < 0 || targetId < 0) {
            return false;
        }
        Slot source = handler.getSlot(sourceId);
        Slot target = handler.getSlot(targetId);
        ItemStack sourceStack = source.getStack();
        ItemStack targetStack = target.getStack();
        if (!source.canTakeItems(client.player) || !target.canInsert(sourceStack)
                || !targetStack.isEmpty() && !source.canInsert(targetStack)) {
            return false;
        }
        click(client, handler, sourceId);
        click(client, handler, targetId);
        if (!handler.getCursorStack().isEmpty()) {
            click(client, handler, sourceId);
        }
        return handler.getCursorStack().isEmpty();
    }

    private static int slotId(ScreenHandler handler, PlayerInventory inventory, int logicalIndex) {
        for (int index = 0; index < handler.slots.size(); index++) {
            Slot slot = handler.slots.get(index);
            if (slot.inventory == inventory && slot.getIndex() == logicalIndex) {
                return index;
            }
        }
        return -1;
    }

    private static void click(MinecraftClient client, ScreenHandler handler, int slotId) {
        client.interactionManager.clickSlot(
                handler.syncId, slotId, 0, SlotActionType.PICKUP, client.player
        );
    }

    public record Result(boolean attempted, int moved, int matched, int unresolved) {
        static Result unavailable() {
            return new Result(false, 0, 0, VirtualInventory.SLOT_COUNT);
        }
    }
}
