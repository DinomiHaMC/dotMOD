package com.dinomiha.dotmod.feature.invsee;

import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventoryDocument;
import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventoryFormatException;
import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventorySerializer;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;
import com.google.gson.JsonObject;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualInventorySerializerTest {
    private static final VirtualInventorySerializer SERIALIZER = new VirtualInventorySerializer();

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void roundTripsComponentBearingStacksAndOmitsEmptySlots() {
        ItemStack original = new ItemStack(Items.DIAMOND, 3);
        original.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ISM codec"));
        NbtCompound nbt = new NbtCompound();
        nbt.putString("marker", "preserved");
        original.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        stacks.set(7, original);

        VirtualInventoryDocument document = SERIALIZER.encode(new VirtualInventorySnapshot(stacks), MinecraftTestBootstrap.registries());
        VirtualInventorySnapshot restored = SERIALIZER.decode(document, MinecraftTestBootstrap.registries());

        assertEquals(1, document.slots.size());
        assertEquals(7, document.slots.getFirst().index);
        assertTrue(ItemStack.areEqual(original, restored.getStack(7)));
        assertEquals("ISM codec", restored.getStack(7).getName().getString());
    }

    @Test
    void rejectsDuplicateSlotsUnknownItemsAndFutureSchema() {
        VirtualInventoryDocument duplicate = new VirtualInventoryDocument();
        JsonObject stone = new JsonObject();
        stone.addProperty("id", "minecraft:stone");
        duplicate.slots.add(new VirtualInventoryDocument.SlotEntry(0, stone));
        duplicate.slots.add(new VirtualInventoryDocument.SlotEntry(0, stone));
        assertThrows(VirtualInventoryFormatException.class, duplicate::validateStructure);

        VirtualInventoryDocument unknown = new VirtualInventoryDocument();
        JsonObject missing = new JsonObject();
        missing.addProperty("id", "dotmod_test:missing_item");
        unknown.slots.add(new VirtualInventoryDocument.SlotEntry(0, missing));
        assertThrows(VirtualInventoryFormatException.class, () -> SERIALIZER.decode(unknown, MinecraftTestBootstrap.registries()));

        VirtualInventoryDocument future = new VirtualInventoryDocument();
        future.schemaVersion = VirtualInventoryDocument.CURRENT_SCHEMA_VERSION + 1;
        assertThrows(UnsupportedDataVersionException.class, future::validateStructure);
    }
}
