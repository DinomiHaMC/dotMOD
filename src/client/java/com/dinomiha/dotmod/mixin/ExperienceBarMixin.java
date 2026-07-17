package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.hud.HudTransform;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceBar.class)
public abstract class ExperienceBarMixin {
    @Inject(method = "renderBar", at = @At("HEAD"))
    private void dotmod$pushExperienceBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.push(context, HudElement.EXP_BAR);
    }

    @Inject(method = "renderBar", at = @At("RETURN"))
    private void dotmod$popExperienceBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.pop(context);
    }
}
