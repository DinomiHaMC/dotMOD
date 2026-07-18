package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.feature.fullbrightness.FullBrightnessController;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @ModifyArg(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
                    ordinal = 3
            ),
            index = 0,
            require = 1
    )
    private float dotmod$fullBrightness(float original) {
        return FullBrightnessController.isActive() ? 1.0F : original;
    }
}
