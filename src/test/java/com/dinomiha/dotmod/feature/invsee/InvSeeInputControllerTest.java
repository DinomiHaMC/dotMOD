package com.dinomiha.dotmod.feature.invsee;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvSeeInputControllerTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void validatesButtonsAndAmountsBeforeMutatingSession() {
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        stacks.set(0, new ItemStack(Items.STONE, 4));
        InvSeeSession session = new InvSeeSession(InvSeeMode.EDIT, new VirtualInventorySnapshot(stacks));
        InvSeeInputController controller = new InvSeeInputController(session);

        assertEquals(MutationResult.INVALID, controller.clickSlot(0, 2));
        assertEquals(MutationResult.INVALID, controller.setAmount(0, "abc"));
        assertEquals(MutationResult.INVALID, controller.setAmount(0, "65"));
        assertEquals(MutationResult.APPLIED, controller.setAmount(0, "2"));
        assertEquals(2, session.getStack(0).getCount());
    }
}
