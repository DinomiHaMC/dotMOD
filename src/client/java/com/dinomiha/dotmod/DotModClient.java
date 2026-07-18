package com.dinomiha.dotmod;

import com.dinomiha.dotmod.command.DotClientCommands;
import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.PlayerColorService;
import com.dinomiha.dotmod.gui.InventoryButtons;
import com.dinomiha.dotmod.feature.preset.screen.PresetPanelController;
import com.dinomiha.dotmod.feature.durability.DurabilityWarningController;
import com.dinomiha.dotmod.feature.inventorysearch.screen.InventorySearchController;
import com.dinomiha.dotmod.feature.commandalias.CommandClientService;
import com.dinomiha.dotmod.feature.commandalias.OutgoingCommandInterceptor;
import com.dinomiha.dotmod.feature.death.DeathClientService;
import com.dinomiha.dotmod.feature.freelook.FreelookController;
import com.dinomiha.dotmod.feature.togglewalk.MovementLifecycle;
import com.dinomiha.dotmod.hud.widget.HudWidgetRegistry;
import com.dinomiha.dotmod.keybind.DotModKeybinds;
import com.dinomiha.dotmod.mixin.HandledScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public final class DotModClient implements ClientModInitializer {
    public static final String MOD_ID = "dotmod";
    public static final String MOD_NAME = "dotMOD";

    @Override
    public void onInitializeClient() {
        ConfigService.initialize();
        DeathClientService.initialize();
        PlayerColorService.initialize();
        CommandClientService.initialize();
        OutgoingCommandInterceptor.register();
        HudWidgetRegistry.register();
        DurabilityWarningController.register();
        DotClientCommands.register();
        DotModKeybinds.register();
        MovementLifecycle.initialize();
        FreelookController.initialize();
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
                InventorySearchController.attach(client, handledScreen);
            }
            if (screen instanceof InventoryScreen inventoryScreen
                    && ConfigService.get().config().general.enabled
                    && ConfigService.get().config().inventoryPresets.enabled) {
                new PresetPanelController(client, inventoryScreen).attach();
            }
        });
    }
}
