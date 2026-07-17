package com.dinomiha.dotmod.feature.playercolor;

import com.dinomiha.dotmod.config.PlayerColorService;
import com.dinomiha.dotmod.feature.playercolor.screen.RecolorPickerScreen;
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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class RecolorCommands {
    private RecolorCommands() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return literal("recolor")
                .executes(context -> list(context.getSource()))
                .then(literal("list").executes(context -> list(context.getSource())))
                .then(literal("set").then(playerArgument().then(argument("hex", StringArgumentType.word())
                        .executes(context -> set(
                                context.getSource(),
                                StringArgumentType.getString(context, "player"),
                                StringArgumentType.getString(context, "hex")
                        )))))
                .then(literal("reset").then(playerArgument().executes(context -> reset(
                        context.getSource(), StringArgumentType.getString(context, "player")
                ))))
                .then(literal("pick").then(playerArgument().executes(context -> pick(
                        context.getSource(), StringArgumentType.getString(context, "player")
                ))));
    }

    private static int list(FabricClientCommandSource source) {
        var service = PlayerColorService.get();
        var colors = service.snapshot();
        if (colors.isEmpty()) {
            MessageService.send(source, Text.translatable("command.dotmod.recolor.list.empty"), MessageType.INFO);
            return Command.SINGLE_SUCCESS;
        }
        MessageService.send(source, Text.translatable("command.dotmod.recolor.list.header", colors.size()), MessageType.INFO);
        colors.forEach((uuid, color) -> MessageService.send(source, Text.translatable(
                "command.dotmod.recolor.list.entry", service.lastKnownName(uuid).orElse(uuid.toString()), color
        ), MessageType.INFO));
        return Command.SINGLE_SUCCESS;
    }

    private static int set(FabricClientCommandSource source, String name, String value) {
        PlayerIdentity player = require(source, name);
        if (player == null) {
            return 0;
        }
        String color = StrictHexColor.parse(value).orElse(null);
        if (color == null) {
            MessageService.send(source, Text.translatable("message.dotmod.recolor.invalid_hex"), MessageType.ERROR);
            return 0;
        }
        boolean saved = PlayerColorService.get().set(player.uuid(), color, player.name());
        MessageService.send(source, Text.translatable(
                saved ? "message.dotmod.player_colors.set" : "message.dotmod.player_colors.save_failed",
                player.name(), color
        ), saved ? MessageType.SUCCESS : MessageType.ERROR);
        return saved ? Command.SINGLE_SUCCESS : 0;
    }

    private static int reset(FabricClientCommandSource source, String name) {
        PlayerIdentity player = require(source, name);
        if (player == null) {
            return 0;
        }
        boolean saved = PlayerColorService.get().clear(player.uuid());
        MessageService.send(source, Text.translatable(
                saved ? "message.dotmod.player_colors.reset" : "message.dotmod.player_colors.save_failed", player.name()
        ), saved ? MessageType.SUCCESS : MessageType.ERROR);
        return saved ? Command.SINGLE_SUCCESS : 0;
    }

    private static int pick(FabricClientCommandSource source, String name) {
        PlayerIdentity player = require(source, name);
        if (player == null) {
            return 0;
        }
        source.getClient().send(() -> source.getClient().setScreen(new RecolorPickerScreen(null, player)));
        return Command.SINGLE_SUCCESS;
    }

    private static PlayerIdentity require(FabricClientCommandSource source, String name) {
        PlayerLookup.Result result = PlayerLookup.resolve(name, players(source));
        if (result.status() != PlayerLookup.Status.FOUND) {
            MessageService.send(source, Text.translatable(result.status() == PlayerLookup.Status.AMBIGUOUS
                    ? "message.dotmod.recolor.ambiguous" : "message.dotmod.recolor.unknown", name), MessageType.ERROR);
            return null;
        }
        return result.player();
    }

    private static List<PlayerIdentity> players(FabricClientCommandSource source) {
        if (source.getClient().getNetworkHandler() == null) {
            return List.of();
        }
        return source.getClient().getNetworkHandler().getListedPlayerListEntries().stream()
                .map(entry -> new PlayerIdentity(entry.getProfile().id(), entry.getProfile().name()))
                .toList();
    }

    private static RequiredArgumentBuilder<FabricClientCommandSource, String> playerArgument() {
        return argument("player", StringArgumentType.string())
                .suggests((context, builder) -> suggest(context.getSource(), builder));
    }

    private static CompletableFuture<Suggestions> suggest(FabricClientCommandSource source, SuggestionsBuilder builder) {
        String probe = builder.getRemainingLowerCase().replaceFirst("^\"", "");
        players(source).stream().map(PlayerIdentity::name).distinct().sorted(String.CASE_INSENSITIVE_ORDER)
                .filter(name -> CommandSource.shouldSuggest(probe, name.toLowerCase(Locale.ROOT)))
                .forEach(name -> builder.suggest(StringArgumentType.escapeIfRequired(name)));
        return builder.buildFuture();
    }
}
