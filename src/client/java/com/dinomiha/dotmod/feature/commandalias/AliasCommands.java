package com.dinomiha.dotmod.feature.commandalias;

import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class AliasCommands {
    private AliasCommands() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return literal("alias")
                .executes(context -> list(context.getSource()))
                .then(literal("list").executes(context -> list(context.getSource())))
                .then(literal("set").then(argument("name", StringArgumentType.word())
                        .then(argument("template", StringArgumentType.greedyString())
                                .executes(context -> set(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "name"),
                                        StringArgumentType.getString(context, "template")
                                )))))
                .then(literal("remove").then(existingName()
                        .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "name")))))
                .then(literal("enable").then(existingName()
                        .executes(context -> enabled(context.getSource(), StringArgumentType.getString(context, "name"), true))))
                .then(literal("disable").then(existingName()
                        .executes(context -> enabled(context.getSource(), StringArgumentType.getString(context, "name"), false))));
    }

    private static int list(FabricClientCommandSource source) {
        var aliases = CommandClientService.get().aliases().list();
        if (aliases.isEmpty()) {
            MessageService.send(source, Text.translatable("command.dotmod.alias.list.empty"), MessageType.INFO);
            return Command.SINGLE_SUCCESS;
        }
        MessageService.send(source, Text.translatable("command.dotmod.alias.list.header", aliases.size()), MessageType.INFO);
        aliases.forEach(alias -> MessageService.send(source, Text.translatable(
                "command.dotmod.alias.list.entry",
                alias.name(),
                alias.template(),
                Text.translatable(alias.enabled() ? "options.on" : "options.off")
        ), MessageType.INFO));
        return Command.SINGLE_SUCCESS;
    }

    private static int set(FabricClientCommandSource source, String name, String template) {
        return run(source, () -> {
            String normalized = CommandAlias.normalizeName(name);
            if (OutgoingCommandInterceptor.conflictsWithActiveCommand(normalized)) {
                throw new AliasException(AliasError.COMMAND_CONFLICT, "Alias conflicts with an active command");
            }
            boolean enabled = CommandClientService.get().aliases().list().stream()
                    .filter(alias -> alias.name().equals(normalized))
                    .findFirst()
                    .map(CommandAlias::enabled)
                    .orElse(true);
            CommandClientService.get().aliases().upsert(new CommandAlias(normalized, template, enabled));
            MessageService.send(source, Text.translatable("command.dotmod.alias.saved", normalized), MessageType.SUCCESS);
        });
    }

    private static int remove(FabricClientCommandSource source, String name) {
        return run(source, () -> {
            if (!CommandClientService.get().aliases().remove(name)) {
                throw new AliasException(AliasError.NOT_FOUND, "Alias not found");
            }
            MessageService.send(source, Text.translatable("command.dotmod.alias.removed", name), MessageType.SUCCESS);
        });
    }

    private static int enabled(FabricClientCommandSource source, String name, boolean enabled) {
        return run(source, () -> {
            CommandAlias existing = CommandClientService.get().aliases().list().stream()
                    .filter(alias -> alias.name().equals(CommandAlias.normalizeName(name)))
                    .findFirst()
                    .orElseThrow(() -> new AliasException(AliasError.NOT_FOUND, "Alias not found"));
            if (enabled && OutgoingCommandInterceptor.conflictsWithActiveCommand(existing.name())) {
                throw new AliasException(AliasError.COMMAND_CONFLICT, "Alias conflicts with an active command");
            }
            CommandClientService.get().aliases().upsert(new CommandAlias(existing.name(), existing.template(), enabled));
            MessageService.send(source, Text.translatable(
                    enabled ? "command.dotmod.alias.enabled" : "command.dotmod.alias.disabled", existing.name()
            ), MessageType.SUCCESS);
        });
    }

    private static RequiredArgumentBuilder<FabricClientCommandSource, String> existingName() {
        return argument("name", StringArgumentType.word()).suggests((context, builder) -> suggest(builder));
    }

    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder) {
        String probe = builder.getRemainingLowerCase();
        for (CommandAlias alias : CommandClientService.get().aliases().list()) {
            if (CommandSource.shouldSuggest(probe, alias.name())) {
                builder.suggest(alias.name());
            }
        }
        return builder.buildFuture();
    }

    private static int run(FabricClientCommandSource source, Action action) {
        try {
            action.run();
            return Command.SINGLE_SUCCESS;
        } catch (AliasException exception) {
            MessageService.send(source, Text.translatable(
                    "message.dotmod.alias.error." + exception.error().name().toLowerCase(Locale.ROOT)
            ), MessageType.ERROR);
            return 0;
        }
    }

    @FunctionalInterface
    private interface Action {
        void run();
    }
}
