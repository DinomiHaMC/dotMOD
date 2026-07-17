package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.util.UniformNameTagRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "net.minecraft.client.render.command.LabelCommandRenderer$Commands")
public abstract class LabelCommandRendererCommandsMixin {
    @ModifyArgs(
            method = "add",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"
            )
    )
    private void dotmod$scaleNameTag(Args args) {
        float scale = UniformNameTagRenderer.scale();
        args.set(0, (float) args.get(0) * scale);
        args.set(1, (float) args.get(1) * scale);
        args.set(2, (float) args.get(2) * scale);
    }

    @ModifyArgs(
            method = "add",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$LabelCommand;<init>(Lorg/joml/Matrix4f;FFLnet/minecraft/text/Text;IIID)V"
            )
    )
    private void dotmod$setNameTagBackgroundColor(Args args) {
        args.set(6, UniformNameTagRenderer.backgroundColor(args.get(6)));
    }
}
