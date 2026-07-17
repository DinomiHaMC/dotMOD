package com.dinomiha.dotmod.feature.invsee.persistence;

import com.dinomiha.dotmod.feature.invsee.VirtualInventory;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Strict registry-aware JSON serializer for component-bearing ItemStacks. */
public final class VirtualInventorySerializer {
    private static final Codec<ItemStack> STACK_CODEC = ItemStack.VALIDATED_CODEC;

    public VirtualInventoryDocument encode(VirtualInventorySnapshot snapshot, RegistryWrapper.WrapperLookup registries) {
        RegistryOps<JsonElement> ops = registries.getOps(JsonOps.INSTANCE);
        VirtualInventoryDocument document = new VirtualInventoryDocument();
        for (int index = 0; index < VirtualInventory.SLOT_COUNT; index++) {
            ItemStack stack = snapshot.getStack(index);
            if (!stack.isEmpty()) {
                document.slots.add(new VirtualInventoryDocument.SlotEntry(index, encodeStack(stack, ops)));
            }
        }
        return document;
    }

    public VirtualInventorySnapshot decode(VirtualInventoryDocument document, RegistryWrapper.WrapperLookup registries) {
        document.validateStructure();
        RegistryOps<JsonElement> ops = registries.getOps(JsonOps.INSTANCE);
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        for (VirtualInventoryDocument.SlotEntry entry : document.slots) {
            stacks.set(entry.index, decodeStack(entry.stack, ops));
        }
        return new VirtualInventorySnapshot(stacks);
    }

    public JsonElement encodeStack(ItemStack stack, RegistryWrapper.WrapperLookup registries) {
        return encodeStack(stack, registries.getOps(JsonOps.INSTANCE));
    }

    private static JsonElement encodeStack(ItemStack stack, RegistryOps<JsonElement> ops) {
        return strict(STACK_CODEC.encodeStart(ops, stack), "Could not encode ItemStack");
    }

    private static ItemStack decodeStack(JsonElement json, RegistryOps<JsonElement> ops) {
        return strict(STACK_CODEC.parse(ops, json), "Could not decode ItemStack");
    }

    private static <T> T strict(DataResult<T> result, String operation) {
        return result.getOrThrow(message -> new VirtualInventoryFormatException(operation + ": " + message));
    }
}
