package com.dinomiha.dotmod.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int dotmod$getX();

    @Accessor("y")
    int dotmod$getY();

    @Accessor("backgroundWidth")
    int dotmod$getBackgroundWidth();
}
