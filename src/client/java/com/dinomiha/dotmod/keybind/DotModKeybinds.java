package com.dinomiha.dotmod.keybind;

import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.util.NameColorManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.StickyKeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class DotModKeybinds {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("dotmod", "controls"));
    private static KeyBinding greenName;
    private static KeyBinding redName;
    private static KeyBinding resetName;
    private static KeyBinding uniformNameTags;
    private static KeyBinding toggleShift;
    private static boolean forcingSneak;

    private DotModKeybinds() {
    }

    public static void register() {
        greenName = register("key.dotmod.green_name", GLFW.GLFW_KEY_G);
        redName = register("key.dotmod.red_name", GLFW.GLFW_KEY_R);
        resetName = register("key.dotmod.reset_name", GLFW.GLFW_KEY_V);
        uniformNameTags = register("key.dotmod.uniform_name_tags", GLFW.GLFW_KEY_N);
        toggleShift = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dotmod.toggle_shift",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(DotModKeybinds::tick);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> updateSneakState(client));
    }

    private static KeyBinding register(String translationKey, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(translationKey, InputUtil.Type.KEYSYM, key, CATEGORY));
    }

    private static void tick(MinecraftClient client) {
        DotModConfig config = DotModConfig.get();
        if (!config.modEnabled) {
            updateSneakState(client);
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
        while (uniformNameTags.wasPressed()) {
            if (config.uniformNameTagsEnabled) {
                config.uniformNameTagsActive = !config.uniformNameTagsActive;
                DotModConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable(
                            "message.dotmod.uniform_name_tags",
                            Text.translatable(config.uniformNameTagsActive ? "options.on" : "options.off")
                    ), true);
                }
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
        updateSneakState(client);
    }

    private static void updateSneakState(MinecraftClient client) {
        DotModConfig config = DotModConfig.get();
        boolean shouldForce = config.modEnabled && config.toggleShiftEnabled && config.toggleShiftActive;
        if (shouldForce) {
            setSneakPressed(client.options.sneakKey, true);
            forcingSneak = true;
        } else if (forcingSneak) {
            setSneakPressed(client.options.sneakKey, isSneakKeyPhysicallyPressed(client));
            forcingSneak = false;
        }
    }

    private static void setSneakPressed(KeyBinding sneakKey, boolean pressed) {
        if (sneakKey instanceof StickyKeyBinding) {
            if (pressed && !sneakKey.isPressed()) {
                sneakKey.setPressed(true);
            } else if (!pressed) {
                sneakKey.setPressed(false);
                if (sneakKey.isPressed()) {
                    sneakKey.setPressed(true);
                }
            }
            return;
        }
        sneakKey.setPressed(pressed);
    }

    private static boolean isSneakKeyPhysicallyPressed(MinecraftClient client) {
        if (client.currentScreen != null) {
            return false;
        }
        InputUtil.Key key = InputUtil.fromTranslationKey(client.options.sneakKey.getBoundKeyTranslationKey());
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), key.getCode()) == GLFW.GLFW_PRESS;
        }
        return key.getCategory() == InputUtil.Type.KEYSYM && InputUtil.isKeyPressed(client.getWindow(), key.getCode());
    }
}
