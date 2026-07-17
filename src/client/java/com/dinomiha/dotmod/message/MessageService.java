package com.dinomiha.dotmod.message;

import com.dinomiha.dotmod.config.CommandsConfig;
import com.dinomiha.dotmod.config.ConfigService;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class MessageService {
    private MessageService() {
    }

    public static Text format(Text message, MessageType type) {
        return prefix()
                .append(Text.literal(" "))
                .append(message.copy().formatted(type.color()));
    }

    public static void send(FabricClientCommandSource source, Text message, MessageType type) {
        Text formatted = format(message, type);
        if (type == MessageType.ERROR) {
            source.sendError(formatted);
        } else {
            source.sendFeedback(formatted);
        }
    }

    public static void sendChat(Text message, MessageType type) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(format(message, type), false);
        } else {
            SystemToast.add(
                    client.getToastManager(),
                    SystemToast.Type.PERIODIC_NOTIFICATION,
                    Text.literal("dotMOD"),
                    format(message, type)
            );
        }
    }

    public static void sendOverlay(Text message, MessageType type) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(format(message, type), true);
        }
    }

    public static MutableText commandAction(Text label, String command, Text hoverText) {
        return label.copy().styled(style -> style
                .withColor(Formatting.AQUA)
                .withUnderline(true)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(hoverText)));
    }

    private static MutableText prefix() {
        CommandsConfig commands = ConfigService.get().config().commands;
        String value = switch (commands.prefix) {
            case DOTMOD_COLON -> "dotMod:";
            case BRACKETED -> "[dotMod]";
            case DOT -> ".";
            case CUSTOM -> commands.customPrefix;
        };
        return Text.literal(value).formatted(Formatting.DARK_AQUA, Formatting.BOLD);
    }
}
