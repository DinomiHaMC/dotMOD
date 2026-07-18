package com.dinomiha.dotmod.keybind;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;

public final class DotModKeybindCategory {
    public static final KeyBinding.Category INSTANCE =
            KeyBinding.Category.create(Identifier.of("dotmod", "controls"));

    private DotModKeybindCategory() {
    }
}
