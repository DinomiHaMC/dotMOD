package com.dinomiha.dotmod.feature.fullbrightness;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.keybind.DotModKeybindCategory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class FullBrightnessController {
    private static final FullBrightnessController INSTANCE = new FullBrightnessController();

    private final GammaOwnershipState state = new GammaOwnershipState();
    private KeyBinding toggleKey;

    private FullBrightnessController() {
    }

    public static void initialize() {
        INSTANCE.toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dotmod.full_brightness", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(),
                DotModKeybindCategory.INSTANCE));
        ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::tick);
        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
            if (screen instanceof VideoOptionsScreen) {
                INSTANCE.suspendForVideoOptions(client);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> INSTANCE.deactivate(client));
        ClientLifecycleEvents.CLIENT_STOPPING.register(INSTANCE::deactivate);
    }

    private void tick(MinecraftClient client) {
        boolean enabled = ConfigService.get().config().general.enabled
                && ConfigService.get().config().fullBrightness.enabled;
        if (!enabled) {
            drainPresses();
            apply(client, state.deactivate(gamma(client)));
            return;
        }
        while (toggleKey.wasPressed()) {
            apply(client, state.toggle(gamma(client)));
        }
        apply(client, state.observe(gamma(client), client.currentScreen instanceof VideoOptionsScreen, true));
    }

    private void deactivate(MinecraftClient client) {
        drainPresses();
        apply(client, state.deactivate(gamma(client)));
    }

    private void suspendForVideoOptions(MinecraftClient client) {
        apply(client, state.observe(gamma(client), true, true));
    }

    private void drainPresses() {
        if (toggleKey != null) while (toggleKey.wasPressed()) {
            // A disabled feature must not queue a later activation.
        }
    }

    private static double gamma(MinecraftClient client) {
        return client.options.getGamma().getValue();
    }

    private static void apply(MinecraftClient client, GammaOwnershipState.Update update) {
        if (update.gammaToApply() != null
                && Double.compare(gamma(client), update.gammaToApply()) != 0) {
            client.options.getGamma().setValue(update.gammaToApply());
        }
    }
}
