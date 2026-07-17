package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.feature.death.DeathClientService;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
abstract class ClientPlayNetworkHandlerDeathMixin {
    @Inject(
            method = "onDeathMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void dotmod$captureLocalDeath(DeathMessageS2CPacket packet, CallbackInfo ci) {
        DeathClientService.get().capture(packet, (ClientPlayNetworkHandler) (Object) this);
    }
}
