package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.feature.inventorysearch.screen.InventorySearchController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenSearchMixin {
    @Inject(
            method = "renderMain(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlots(Lnet/minecraft/client/gui/DrawContext;II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void dotmod$renderInventorySearchMasks(
            DrawContext context,
            int mouseX,
            int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        InventorySearchController.renderMasks((HandledScreen<?>)(Object)this, context);
    }
}
