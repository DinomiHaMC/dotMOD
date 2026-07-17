package com.dinomiha.dotmod.feature.preset.helper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A frozen, copy-only view of inventory data already synchronized to this client. */
public record ClientInventorySnapshot(
        List<ItemStack> playerStacks,
        List<ItemStack> containerStacks,
        boolean containerOpen
) {
    public ClientInventorySnapshot {
        playerStacks = copy(playerStacks);
        containerStacks = copy(containerStacks);
    }

    public static Optional<ClientInventorySnapshot> capture(MinecraftClient client) {
        if (client == null || !client.isOnThread() || client.player == null || client.world == null) {
            return Optional.empty();
        }
        PlayerInventory inventory = client.player.getInventory();
        ScreenHandler active = client.player.currentScreenHandler;
        List<ItemStack> player = new ArrayList<>(inventory.size());
        for (int index = 0; index < inventory.size(); index++) {
            player.add(inventory.getStack(index).copy());
        }

        ScreenHandler container = null;
        if (!(client.currentScreen instanceof CreativeInventoryScreen)
                && client.currentScreen instanceof HandledScreen<?> handled
                && handled.getScreenHandler() == active
                && active != client.player.playerScreenHandler) {
            container = active;
        }
        List<ItemStack> visibleContainer = new ArrayList<>();
        if (container != null) {
            List<InventorySlot> seen = new ArrayList<>();
            for (Slot slot : List.copyOf(container.slots)) {
                if (slot.inventory == inventory || slot instanceof CraftingResultSlot || !slot.isEnabled()) {
                    continue;
                }
                InventorySlot identity = new InventorySlot(slot.inventory, slot.getIndex());
                if (seen.stream().anyMatch(identity::sameAs)) {
                    continue;
                }
                seen.add(identity);
                visibleContainer.add(slot.getStack().copy());
            }
        }
        if (client.player == null || client.world == null || client.player.getInventory() != inventory
                || client.player.currentScreenHandler != active) {
            return Optional.empty();
        }
        return Optional.of(new ClientInventorySnapshot(player, visibleContainer, container != null));
    }

    @Override
    public List<ItemStack> playerStacks() {
        return copy(playerStacks);
    }

    @Override
    public List<ItemStack> containerStacks() {
        return copy(containerStacks);
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private record InventorySlot(net.minecraft.inventory.Inventory inventory, int index) {
        private boolean sameAs(InventorySlot other) {
            return inventory == other.inventory && index == other.index;
        }
    }
}
