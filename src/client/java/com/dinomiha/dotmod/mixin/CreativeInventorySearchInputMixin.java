package com.dinomiha.dotmod.mixin;

import com.dinomiha.dotmod.feature.inventorysearch.screen.InventorySearchController;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventorySearchInputMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void dotmod$inventorySearchCharTyped(CharInput input, CallbackInfoReturnable<Boolean> cir) {
        if (InventorySearchController.charTyped((CreativeInventoryScreen)(Object)this, input)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void dotmod$inventorySearchKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (InventorySearchController.keyPressed((CreativeInventoryScreen)(Object)this, input)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void dotmod$inventorySearchMouseClicked(
            Click click,
            boolean doubled,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (InventorySearchController.mouseClicked((CreativeInventoryScreen)(Object)this, click, doubled)) {
            cir.setReturnValue(true);
        }
    }
}
