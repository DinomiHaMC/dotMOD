package com.dinomiha.dotmod.feature.invsee.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Set;
import java.util.function.Consumer;

/** Optional read-only context supplied by a feature that embeds ISM view mode. */
public record InvSeeSupplement(
        Text summary,
        Text actionLabel,
        Consumer<Screen> action,
        Set<Integer> warningSlots
) {
    public InvSeeSupplement {
        if (summary == null || actionLabel == null || action == null || warningSlots == null) {
            throw new IllegalArgumentException("Invalid ISM supplement");
        }
        warningSlots = Set.copyOf(warningSlots);
    }
}
