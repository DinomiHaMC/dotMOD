package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.hud.HudTransform;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void dotmod$pushHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.push(context, HudElement.HOTBAR);
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void dotmod$popHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.pop(context);
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"))
    private void dotmod$pushEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.push(context, HudElement.EFFECTS);
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("RETURN"))
    private void dotmod$popEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.pop(context);
    }

    @Inject(method = "renderBossBarHud", at = @At("HEAD"))
    private void dotmod$pushBossbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.push(context, HudElement.BOSSBAR);
    }

    @Inject(method = "renderBossBarHud", at = @At("RETURN"))
    private void dotmod$popBossbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.pop(context);
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"))
    private void dotmod$pushScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.push(context, HudElement.SCOREBOARD);
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("RETURN"))
    private void dotmod$popScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudTransform.pop(context);
    }

    @Inject(method = "renderArmor", at = @At("HEAD"))
    private static void dotmod$pushArmor(DrawContext context, PlayerEntity player, int i, int j, int k, int x, CallbackInfo ci) {
        HudTransform.push(context, HudElement.ARMOR);
    }

    @Inject(method = "renderArmor", at = @At("RETURN"))
    private static void dotmod$popArmor(DrawContext context, PlayerEntity player, int i, int j, int k, int x, CallbackInfo ci) {
        HudTransform.pop(context);
    }

    @Inject(method = "renderHealthBar", at = @At("HEAD"))
    private void dotmod$pushHearts(DrawContext context, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking, CallbackInfo ci) {
        HudTransform.push(context, HudElement.HEARTS);
    }

    @Inject(method = "renderHealthBar", at = @At("RETURN"))
    private void dotmod$popHearts(DrawContext context, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking, CallbackInfo ci) {
        HudTransform.pop(context);
    }

    @Inject(method = "renderFood", at = @At("HEAD"))
    private void dotmod$pushHunger(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        HudTransform.push(context, HudElement.HUNGER);
    }

    @Inject(method = "renderFood", at = @At("RETURN"))
    private void dotmod$popHunger(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        HudTransform.pop(context);
    }
}
