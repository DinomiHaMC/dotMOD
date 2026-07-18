package com.dinomiha.dotmod.feature.fullbrightness;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.keybind.DotModKeybindCategory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class FullBrightnessController {
    private static final FullBrightnessController INSTANCE = new FullBrightnessController();

    private final FullBrightnessState state = new FullBrightnessState();
    private KeyBinding toggleKey;

    private FullBrightnessController() {
    }

    public static void initialize() {
        INSTANCE.toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dotmod.full_brightness", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(),
                DotModKeybindCategory.INSTANCE));
        ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> INSTANCE.deactivate(client));
        ClientLifecycleEvents.CLIENT_STOPPING.register(INSTANCE::deactivate);
    }

    public static boolean isActive() {
        return INSTANCE.state.active();
    }

    private void tick(MinecraftClient client) {
        boolean enabled = ConfigService.get().config().general.enabled
                && ConfigService.get().config().fullBrightness.enabled;
        if (!enabled) {
            drainPresses();
            refresh(client, state.deactivate());
            return;
        }
        while (toggleKey.wasPressed()) {
            refresh(client, state.toggle(true));
        }
    }

    private void deactivate(MinecraftClient client) {
        drainPresses();
        refresh(client, state.deactivate());
    }

    private void drainPresses() {
        if (toggleKey != null) while (toggleKey.wasPressed()) {
            // A disabled feature must not queue a later activation.
        }
    }

    private static void refresh(MinecraftClient client, FullBrightnessState.Update update) {
        if (update.changed() && client.world != null) {
            client.gameRenderer.getLightmapTextureManager().tick();
        }
    }
}
