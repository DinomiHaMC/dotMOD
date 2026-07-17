package com.dinomiha.dotmod.keybind;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.dinomiha.dotmod.feature.preset.PresetClientService;
import com.dinomiha.dotmod.feature.preset.PresetException;
import com.dinomiha.dotmod.feature.preset.PresetRecord;
import com.dinomiha.dotmod.feature.preset.helper.PresetHelperClientService;
import com.dinomiha.dotmod.util.NameColorManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.StickyKeyBinding;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
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
    private static KeyBinding presetHelper;
    private static boolean forcingSneak;

    private DotModKeybinds() {
    }

    public static void register() {
        greenName = register("key.dotmod.green_name", GLFW.GLFW_KEY_G);
        redName = register("key.dotmod.red_name", GLFW.GLFW_KEY_R);
        resetName = register("key.dotmod.reset_name", GLFW.GLFW_KEY_V);
        uniformNameTags = register("key.dotmod.uniform_name_tags", GLFW.GLFW_KEY_N);
        presetHelper = register("key.dotmod.preset_helper", InputUtil.UNKNOWN_KEY.getCode());
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

    public static Text description(String translationKey) {
        KeyBinding binding = KeyBinding.byId(translationKey);
        Text boundKey = binding == null ? Text.translatable("key.keyboard.unknown") : binding.getBoundKeyLocalizedText();
        return Text.translatable("config.dotmod.keybinds.entry", Text.translatable(translationKey), boundKey);
    }

    private static void tick(MinecraftClient client) {
        DotModConfig config = ConfigService.get().config();
        if (!config.general.enabled) {
            updateSneakState(client);
            return;
        }
        while (greenName.wasPressed()) {
            if (config.playerColors.enabled) {
                NameColorManager.colorTargetedPlayer(config.playerColors.greenColor);
            }
        }
        while (redName.wasPressed()) {
            if (config.playerColors.enabled) {
                NameColorManager.colorTargetedPlayer(config.playerColors.redColor);
            }
        }
        while (resetName.wasPressed()) {
            if (config.playerColors.enabled) {
                NameColorManager.resetTargetedPlayer();
            }
        }
        while (uniformNameTags.wasPressed()) {
            if (config.hud.uniformNameTags.enabled) {
                config.hud.uniformNameTags.active = !config.hud.uniformNameTags.active;
                boolean saved = ConfigService.get().save();
                if (config.keybinds.showToggleMessages) {
                    MessageService.sendOverlay(
                            saved
                                    ? Text.translatable(
                                            "message.dotmod.uniform_name_tags",
                                            Text.translatable(config.hud.uniformNameTags.active ? "options.on" : "options.off")
                                    )
                                    : Text.translatable("message.dotmod.config.save_failed"),
                            saved ? MessageType.INFO : MessageType.ERROR
                    );
                }
            }
        }
        while (toggleShift.wasPressed()) {
            if (config.toggleWalk.toggleShift.enabled) {
                config.toggleWalk.toggleShift.active = !config.toggleWalk.toggleShift.active;
                boolean saved = ConfigService.get().save();
                if (config.keybinds.showToggleMessages) {
                    MessageService.sendOverlay(
                            saved
                                    ? Text.translatable(
                                            "message.dotmod.toggle_shift",
                                            Text.translatable(config.toggleWalk.toggleShift.active ? "options.on" : "options.off")
                                    )
                                    : Text.translatable("message.dotmod.config.save_failed"),
                            saved ? MessageType.INFO : MessageType.ERROR
                    );
                }
            }
        }
        while (presetHelper.wasPressed()) {
            if (config.inventoryPresets.enabled) {
                openPresetHelper(client);
            }
        }
        updateSneakState(client);
    }

    private static void openPresetHelper(MinecraftClient client) {
        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
            MessageService.sendChat(Text.translatable("message.dotmod.preset.helper.no_world"), MessageType.WARNING);
            return;
        }
        if (client.currentScreen != null && !(client.currentScreen instanceof HandledScreen<?>)) {
            return;
        }
        try {
            PresetRecord active = PresetClientService.active(client).orElse(null);
            if (active == null) {
                MessageService.sendChat(Text.translatable("command.dotmod.preset.helper.no_active"), MessageType.WARNING);
                return;
            }
            PresetHelperClientService.open(client, client.currentScreen, active);
        } catch (PresetException exception) {
            PresetClientService.report(exception);
        }
    }

    private static void updateSneakState(MinecraftClient client) {
        DotModConfig config = ConfigService.get().config();
        boolean shouldForce = config.general.enabled
                && config.toggleWalk.toggleShift.enabled
                && config.toggleWalk.toggleShift.active;
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
