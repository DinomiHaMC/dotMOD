package com.dinomiha.dotmod;

import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.gui.InventoryButtons;
import com.dinomiha.dotmod.keybind.DotModKeybinds;
import com.dinomiha.dotmod.mixin.HandledScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public final class DotModClient implements ClientModInitializer {
    public static final String MOD_ID = "dotmod";
    public static final String MOD_NAME = "dotMOD";

    @Override
    public void onInitializeClient() {
        DotModConfig.load();
        DotModKeybinds.register();
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen<?> handledScreen) {
                HandledScreenAccessor accessor = (HandledScreenAccessor) handledScreen;
                InventoryButtons.add(
                        handledScreen,
                        button -> Screens.getButtons(screen).add(button),
                        accessor.dotmod$getX(),
                        accessor.dotmod$getY(),
                        accessor.dotmod$getBackgroundWidth()
                );
            }
        });
    }
}
