package com.dinomiha.dotmod.feature.commandalias;

import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Locale;

public final class OutgoingCommandInterceptor {
    private static Plan pending;

    private OutgoingCommandInterceptor() {
    }

    public static void register() {
        ClientSendMessageEvents.ALLOW_COMMAND.register(OutgoingCommandInterceptor::allow);
        ClientSendMessageEvents.MODIFY_COMMAND.register(OutgoingCommandInterceptor::modify);
        ClientSendMessageEvents.COMMAND.register(OutgoingCommandInterceptor::accepted);
        ClientSendMessageEvents.COMMAND_CANCELED.register(command -> pending = null);
    }

    private static boolean allow(String command) {
        pending = null;
        try {
            CommandClientService service = CommandClientService.get();
            String result = command;
            if (service.aliasesEnabled()) {
                String root = command.stripLeading().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
                if (service.aliases().list().stream()
                        .anyMatch(alias -> alias.enabled() && alias.name().equals(root))) {
                    if (conflictsWithActiveCommand(root)) {
                        throw new AliasException(AliasError.COMMAND_CONFLICT, "Alias conflicts with an active command");
                    }
                    result = new AliasExpander(service.aliases().list()).expand(command);
                }
            }
            pending = new Plan(command, result);
            return true;
        } catch (AliasException exception) {
            MessageService.sendChat(
                    Text.translatable("message.dotmod.alias.error." + exception.error().name().toLowerCase(Locale.ROOT)),
                    MessageType.ERROR
            );
            return false;
        }
    }

    private static String modify(String command) {
        Plan plan = pending;
        if (plan == null || plan.original().equals(plan.expanded())) {
            return command;
        }
        return plan.expanded();
    }

    private static void accepted(String command) {
        try {
            CommandClientService.get().history().record(command);
        } catch (IllegalArgumentException ignored) {
            // Another message modifier may produce a command outside the history model's bounds.
        } finally {
            pending = null;
        }
    }

    public static boolean conflictsWithActiveCommand(String root) {
        String normalized = root.toLowerCase(Locale.ROOT);
        if (normalized.equals("dot") || normalized.equals("dotmod")) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        boolean serverConflict = client.getNetworkHandler() != null
                && client.getNetworkHandler().getCommandDispatcher().getRoot().getChild(normalized) != null;
        var clientDispatcher = ClientCommandManager.getActiveDispatcher();
        return serverConflict || clientDispatcher != null
                && clientDispatcher.getRoot().getChild(normalized) != null;
    }

    private record Plan(String original, String expanded) {
    }
}
