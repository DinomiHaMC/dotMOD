package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.feature.death.DeathScreenshotQueue;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientRenderMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void dotmod$captureDeferredDeathScreenshot(boolean tick, CallbackInfo ci) {
        DeathScreenshotQueue.processFrame((MinecraftClient) (Object) this);
    }
}
