package com.dinomiha.dotmod.keybind;

import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.util.NameColorManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class DotModKeybinds {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("dotmod", "controls"));
    private static KeyBinding greenName;
    private static KeyBinding redName;
    private static KeyBinding resetName;
    private static KeyBinding toggleShift;

    private DotModKeybinds() {
    }

    public static void register() {
        greenName = register("key.dotmod.green_name", GLFW.GLFW_KEY_G);
        redName = register("key.dotmod.red_name", GLFW.GLFW_KEY_R);
        resetName = register("key.dotmod.reset_name", GLFW.GLFW_KEY_V);
        toggleShift = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dotmod.toggle_shift",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(DotModKeybinds::tick);
    }

    private static KeyBinding register(String translationKey, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(translationKey, InputUtil.Type.KEYSYM, key, CATEGORY));
    }

    private static void tick(MinecraftClient client) {
        DotModConfig config = DotModConfig.get();
        if (!config.modEnabled) {
            return;
        }
        while (greenName.wasPressed()) {
            if (config.nameColorsEnabled) {
                NameColorManager.colorTargetedPlayer(config.greenColor);
            }
        }
        while (redName.wasPressed()) {
            if (config.nameColorsEnabled) {
                NameColorManager.colorTargetedPlayer(config.redColor);
            }
        }
        while (resetName.wasPressed()) {
            if (config.nameColorsEnabled) {
                NameColorManager.resetTargetedPlayer();
            }
        }
        while (toggleShift.wasPressed()) {
            if (config.toggleShiftEnabled) {
                config.toggleShiftActive = !config.toggleShiftActive;
                DotModConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Toggle Shift: " + (config.toggleShiftActive ? "ON" : "OFF")), true);
                }
            }
        }
        if (config.toggleShiftEnabled && config.toggleShiftActive) {
            client.options.sneakKey.setPressed(true);
        }
    }
}
