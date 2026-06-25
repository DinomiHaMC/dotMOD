package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.util.NameColorManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void dotmod$colorPlayerName(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (state.playerName != null) {
            state.playerName = NameColorManager.coloredCopy(state.playerName, entity.getUuid());
        }
        if (state.displayName != null) {
            state.displayName = NameColorManager.coloredCopy(state.displayName, entity.getUuid());
        }
    }
}
