package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.util.NameColorManager;
import com.dinomiha.dotmod.util.UniformNameTagRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Redirect(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V"
            )
    )
    private void dotmod$renderUniformNameTag(
            OrderedRenderCommandQueue queue,
            MatrixStack matrices,
            Vec3d position,
            int yOffset,
            Text text,
            boolean discrete,
            int light,
            double squaredDistanceToCamera,
            CameraRenderState cameraState
    ) {
        UniformNameTagRenderer.submit(
                queue,
                matrices,
                position,
                yOffset,
                text,
                discrete,
                light,
                squaredDistanceToCamera,
                cameraState
        );
    }
}
