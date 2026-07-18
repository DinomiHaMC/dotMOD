package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.feature.freelook.FreelookController;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @WrapOperation(
            method = "updateMouse(D)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V")
    )
    private void dotmod$freelook(ClientPlayerEntity player, double yawDelta, double pitchDelta, Operation<Void> original) {
        if (!FreelookController.interceptLook(yawDelta, pitchDelta)) {
            original.call(player, yawDelta, pitchDelta);
        }
    }
}
