package com.dinomiha.dotmod.feature.invsee;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeSessionTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registries();
    }

    @Test
    void viewModeDeniesEveryMutation() {
        InvSeeSession session = new InvSeeSession(InvSeeMode.VIEW, snapshotWith(new ItemStack(Items.STONE, 4)));

        assertEquals(MutationResult.DENIED, session.leftClick(0));
        assertEquals(MutationResult.DENIED, session.rightClick(0));
        assertEquals(MutationResult.DENIED, session.clear(0));
        assertEquals(MutationResult.DENIED, session.setAmount(0, 2));
        assertEquals(MutationResult.DENIED, session.takeCatalogStack(new ItemStack(Items.DIAMOND), false));
        assertEquals(MutationResult.DENIED, session.rollback());
        assertFalse(session.save(snapshot -> true));
        assertEquals(4, session.getStack(0).getCount());
        assertFalse(session.isDirty());
    }

    @Test
    void editorMovesChangesAmountAndRollsBackComponents() {
        ItemStack named = new ItemStack(Items.DIAMOND, 4);
        named.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Local test"));
        InvSeeSession session = new InvSeeSession(InvSeeMode.EDIT, snapshotWith(named));

        assertEquals(MutationResult.APPLIED, session.leftClick(0));
        assertEquals(MutationResult.APPLIED, session.leftClick(1));
        assertTrue(session.getStack(0).isEmpty());
        assertEquals("Local test", session.getStack(1).getName().getString());
        assertEquals(MutationResult.APPLIED, session.setAmount(1, 2));
        assertTrue(session.isDirty());

        session.rollback();
        assertEquals(4, session.getStack(0).getCount());
        assertEquals("Local test", session.getStack(0).getName().getString());
        assertTrue(session.getStack(1).isEmpty());
        assertFalse(session.isDirty());
    }

    @Test
    void saveIsExplicitAndFailedSaveKeepsDirtyState() {
        InvSeeSession session = new InvSeeSession(InvSeeMode.EDIT, snapshotWith(new ItemStack(Items.STONE, 4)));
        session.setAmount(0, 2);
        AtomicInteger calls = new AtomicInteger();

        assertFalse(session.save(snapshot -> {
            calls.incrementAndGet();
            return false;
        }));
        assertEquals(1, calls.get());
        assertTrue(session.isDirty());

        assertTrue(session.save(snapshot -> {
            calls.incrementAndGet();
            return snapshot.getStack(0).getCount() == 2;
        }));
        assertEquals(2, calls.get());
        assertFalse(session.isDirty());
    }

    @Test
    void creativeCatalogUsesCopiesAndNeverOverwritesCursor() {
        InvSeeSession session = new InvSeeSession(InvSeeMode.CREATIVE, VirtualInventorySnapshot.empty());
        ItemStack catalogStack = new ItemStack(Items.OAK_LOG);

        assertEquals(MutationResult.APPLIED, session.takeCatalogStack(catalogStack, true));
        assertEquals(64, session.getCursorStack().getCount());
        assertEquals(1, catalogStack.getCount());
        assertEquals(MutationResult.INVALID, session.takeCatalogStack(new ItemStack(Items.STONE), false));
        assertEquals(MutationResult.APPLIED, session.leftClick(0));
        assertEquals(64, session.getStack(0).getCount());
    }

    @Test
    void localCursorBlocksSaveBeforeTargetIsCalled() {
        InvSeeSession session = new InvSeeSession(InvSeeMode.EDIT, snapshotWith(new ItemStack(Items.STONE, 4)));
        AtomicInteger calls = new AtomicInteger();
        session.leftClick(0);

        assertFalse(session.save(snapshot -> {
            calls.incrementAndGet();
            return true;
        }));
        assertEquals(0, calls.get());
    }

    private static VirtualInventorySnapshot snapshotWith(ItemStack first) {
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(VirtualInventory.SLOT_COUNT, ItemStack.EMPTY));
        stacks.set(0, first);
        return new VirtualInventorySnapshot(stacks);
    }
}
