package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.feature.freelook.FreelookController;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw(F)F"))
    private float dotmod$freelookYaw(float original) {
        return FreelookController.cameraYaw(original);
    }

    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPitch(F)F"))
    private float dotmod$freelookPitch(float original) {
        return FreelookController.cameraPitch(original);
    }
}
