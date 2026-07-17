package com.dinomiha.dotmod.feature.invsee;

import net.minecraft.item.ItemStack;

public final class InvSeeInputController {
    private final InvSeeSession session;

    public InvSeeInputController(InvSeeSession session) {
        this.session = session;
    }

    public MutationResult clickSlot(int index, int button) {
        return button == 0 ? session.leftClick(index) : button == 1 ? session.rightClick(index) : MutationResult.INVALID;
    }

    public MutationResult deleteSlot(int index) {
        return session.clear(index);
    }

    public MutationResult setAmount(int index, String amountText) {
        try {
            return session.setAmount(index, Integer.parseInt(amountText));
        } catch (NumberFormatException exception) {
            return MutationResult.INVALID;
        }
    }

    public MutationResult takeCatalogStack(ItemStack stack, boolean maximumAmount) {
        return session.takeCatalogStack(stack, maximumAmount);
    }
}
