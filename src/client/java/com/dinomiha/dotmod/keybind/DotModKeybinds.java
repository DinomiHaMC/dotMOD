package com.dinomiha.dotmod.keybind;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.dinomiha.dotmod.feature.preset.PresetClientService;
import com.dinomiha.dotmod.feature.preset.PresetException;
import com.dinomiha.dotmod.feature.preset.PresetRecord;
import com.dinomiha.dotmod.feature.preset.helper.PresetHelperClientService;
import com.dinomiha.dotmod.feature.commandlist.screen.FastCommandListScreen;
import com.dinomiha.dotmod.util.NameColorManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class DotModKeybinds {
    private static KeyBinding greenName;
    private static KeyBinding redName;
    private static KeyBinding resetName;
    private static KeyBinding uniformNameTags;
    private static KeyBinding presetHelper;
    private static KeyBinding fastCommands;

    private DotModKeybinds() {
    }

    public static void register() {
        greenName = register("key.dotmod.green_name", GLFW.GLFW_KEY_G);
        redName = register("key.dotmod.red_name", GLFW.GLFW_KEY_R);
        resetName = register("key.dotmod.reset_name", GLFW.GLFW_KEY_V);
        uniformNameTags = register("key.dotmod.uniform_name_tags", GLFW.GLFW_KEY_N);
        presetHelper = register("key.dotmod.preset_helper", InputUtil.UNKNOWN_KEY.getCode());
        fastCommands = register("key.dotmod.fast_commands", InputUtil.UNKNOWN_KEY.getCode());
        ClientTickEvents.END_CLIENT_TICK.register(DotModKeybinds::tick);
    }

    private static KeyBinding register(String translationKey, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey, InputUtil.Type.KEYSYM, key, DotModKeybindCategory.INSTANCE));
    }

    public static Text description(String translationKey) {
        KeyBinding binding = KeyBinding.byId(translationKey);
        Text boundKey = binding == null ? Text.translatable("key.keyboard.unknown") : binding.getBoundKeyLocalizedText();
        return Text.translatable("config.dotmod.keybinds.entry", Text.translatable(translationKey), boundKey);
    }

    private static void tick(MinecraftClient client) {
        DotModConfig config = ConfigService.get().config();
        if (!config.general.enabled) {
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
        while (presetHelper.wasPressed()) {
            if (config.inventoryPresets.enabled) {
                openPresetHelper(client);
            }
        }
        while (fastCommands.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new FastCommandListScreen(null));
            }
        }
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

}
