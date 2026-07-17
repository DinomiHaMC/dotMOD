package com.dinomiha.dotmod.feature.death.command;

import com.dinomiha.dotmod.feature.death.DeathActions;
import com.dinomiha.dotmod.feature.death.DeathClientService;
import com.dinomiha.dotmod.feature.death.DeathException;
import com.dinomiha.dotmod.feature.death.model.DeathRecord;
import com.dinomiha.dotmod.feature.death.screen.DeathHistoryScreen;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.dinomiha.dotmod.ui.component.DotConfirmationDialog;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class DeathCommands {
    private DeathCommands() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return literal("deaths")
                .executes(context -> openHistory(context.getSource()))
                .then(literal("list").executes(context -> openHistory(context.getSource())))
                .then(recordAction("show", DeathActions::show))
                .then(recordAction("inventory", record -> {
                    var client = net.minecraft.client.MinecraftClient.getInstance();
                    DeathActions.inventory(client, client.currentScreen, record);
                }))
                .then(recordAction("view", record -> {
                    var client = net.minecraft.client.MinecraftClient.getInstance();
                    DeathActions.view(client, client.currentScreen, record);
                }))
                .then(recordAction("copy", record -> DeathActions.copy(net.minecraft.client.MinecraftClient.getInstance(), record)))
                .then(recordAction("open", DeathActions::open))
                .then(literal("delete").then(argument("id-prefix", StringArgumentType.word())
                        .executes(context -> confirmDelete(context.getSource(), StringArgumentType.getString(context, "id-prefix")))))
                .then(literal("clear").executes(context -> confirmClear(context.getSource())));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> recordAction(String name, Consumer<DeathRecord> action) {
        return literal(name).then(argument("id-prefix", StringArgumentType.word()).executes(context -> {
            try {
                DeathRecord record = require(StringArgumentType.getString(context, "id-prefix"));
                context.getSource().getClient().execute(() -> action.accept(record));
                return Command.SINGLE_SUCCESS;
            } catch (DeathException exception) {
                error(context.getSource());
                return 0;
            }
        }));
    }

    private static int openHistory(FabricClientCommandSource source) {
        source.getClient().execute(() -> source.getClient().setScreen(new DeathHistoryScreen(null)));
        return Command.SINGLE_SUCCESS;
    }

    private static int confirmDelete(FabricClientCommandSource source, String id) {
        try {
            DeathRecord record = require(id);
            source.getClient().execute(() -> DotConfirmationDialog.open(source.getClient(), source.getClient().currentScreen,
                    Text.translatable("command.dotmod.death.delete.title"),
                    Text.translatable("command.dotmod.death.delete.message", DeathActions.shortId(record)), () -> {
                        DeathClientService.get().delete(record.id());
                        MessageService.sendChat(Text.translatable("command.dotmod.death.deleted", DeathActions.shortId(record)), MessageType.SUCCESS);
                    }));
            return Command.SINGLE_SUCCESS;
        } catch (DeathException exception) {
            error(source);
            return 0;
        }
    }

    private static int confirmClear(FabricClientCommandSource source) {
        int count;
        try {
            count = DeathClientService.get().list().size();
        } catch (DeathException exception) {
            error(source);
            return 0;
        }
        int records = count;
        source.getClient().execute(() -> DotConfirmationDialog.open(source.getClient(), source.getClient().currentScreen,
                Text.translatable("command.dotmod.death.clear.title"),
                Text.translatable("command.dotmod.death.clear.message", records), () -> {
                    int cleared = DeathClientService.get().clear();
                    MessageService.sendChat(Text.translatable("command.dotmod.death.cleared", cleared), MessageType.SUCCESS);
                }));
        return Command.SINGLE_SUCCESS;
    }

    private static DeathRecord require(String id) {
        return DeathClientService.get().get(id).orElseThrow(() ->
                new DeathException(com.dinomiha.dotmod.feature.death.DeathError.NOT_FOUND, "Death not found"));
    }

    private static void error(FabricClientCommandSource source) {
        MessageService.send(source, Text.translatable("message.dotmod.death.not_found"), MessageType.ERROR);
    }
}
