package com.dinomiha.dotmod.command;

import com.dinomiha.dotmod.DotModClient;
import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.DotModConfigScreen;
import com.dinomiha.dotmod.config.MessagePrefixMode;
import com.dinomiha.dotmod.config.PlayerColorService;
import com.dinomiha.dotmod.feature.invsee.InvSeeMode;
import com.dinomiha.dotmod.feature.invsee.InvSeeService;
import com.dinomiha.dotmod.gui.HudEditorScreen;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class DotClientCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(DotModClient.MOD_ID + "/commands");

    private DotClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerRoot(dispatcher, "dot");
            registerRoot(dispatcher, "dotmod");
        });
    }

    private static void registerRoot(CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
        if (dispatcher.getRoot().getChild(root) != null) {
            LOGGER.warn("Another client mod already registered /{}; dotMOD will not modify that command tree", root);
            return;
        }
        dispatcher.register(literal(root)
                .executes(context -> showHelp(context.getSource(), root))
                .then(literal("help")
                        .executes(context -> showHelp(context.getSource(), root)))
                .then(literal("config")
                        .executes(context -> openConfig(context.getSource())))
                .then(literal("hud")
                        .executes(context -> openHudEditor(context.getSource())))
                .then(literal("ism")
                        .executes(context -> openIsm(context.getSource(), InvSeeMode.VIEW))
                        .then(literal("view").executes(context -> openIsm(context.getSource(), InvSeeMode.VIEW)))
                        .then(literal("edit").executes(context -> openIsm(context.getSource(), InvSeeMode.EDIT)))
                        .then(literal("creative").executes(context -> openIsm(context.getSource(), InvSeeMode.CREATIVE))))
                .then(literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(literal("prefix")
                        .executes(context -> showPrefix(context.getSource()))
                        .then(prefixMode("dotmod", MessagePrefixMode.DOTMOD_COLON))
                        .then(prefixMode("brackets", MessagePrefixMode.BRACKETED))
                        .then(prefixMode("dot", MessagePrefixMode.DOT))
                        .then(literal("custom")
                                .then(argument("value", StringArgumentType.string())
                                        .executes(context -> setCustomPrefix(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "value")
                                        ))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> prefixMode(
            String name,
            MessagePrefixMode mode
    ) {
        return literal(name).executes(context -> setPrefix(context.getSource(), mode));
    }

    private static int showHelp(FabricClientCommandSource source, String root) {
        MutableText actions = Text.translatable("command.dotmod.help.intro").append(Text.literal(" "));
        actions.append(MessageService.commandAction(
                Text.translatable("command.dotmod.help.config"),
                "/" + root + " config",
                Text.translatable("command.dotmod.help.config.tooltip")
        ));
        actions.append(Text.literal("  "));
        actions.append(MessageService.commandAction(
                Text.translatable("command.dotmod.help.hud"),
                "/" + root + " hud",
                Text.translatable("command.dotmod.help.hud.tooltip")
        ));
        actions.append(Text.literal("  "));
        actions.append(MessageService.commandAction(
                Text.translatable("command.dotmod.help.ism"),
                "/" + root + " ism",
                Text.translatable("command.dotmod.help.ism.tooltip")
        ));
        MessageService.send(source, actions, MessageType.INFO);
        return Command.SINGLE_SUCCESS;
    }

    private static int openConfig(FabricClientCommandSource source) {
        source.getClient().send(() -> source.getClient().setScreen(DotModConfigScreen.create(null)));
        return Command.SINGLE_SUCCESS;
    }

    private static int openHudEditor(FabricClientCommandSource source) {
        source.getClient().send(() -> source.getClient().setScreen(new HudEditorScreen(null)));
        return Command.SINGLE_SUCCESS;
    }

    private static int openIsm(FabricClientCommandSource source, InvSeeMode mode) {
        source.getClient().send(() -> InvSeeService.open(source.getClient(), mode));
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(FabricClientCommandSource source) {
        boolean configLoaded = ConfigService.get().reload();
        boolean colorsLoaded = PlayerColorService.get().reload();
        MessageType type = configLoaded && colorsLoaded ? MessageType.SUCCESS : MessageType.WARNING;
        MessageService.send(source, Text.translatable(
                type == MessageType.SUCCESS ? "command.dotmod.reload.success" : "command.dotmod.reload.recovered"
        ), type);
        return Command.SINGLE_SUCCESS;
    }

    private static int showPrefix(FabricClientCommandSource source) {
        MessagePrefixMode prefix = ConfigService.get().config().commands.prefix;
        MessageService.send(source, Text.translatable(
                "command.dotmod.prefix.current",
                Text.translatable("config.dotmod.commands.prefix." + prefix.name().toLowerCase(Locale.ROOT))
        ), MessageType.INFO);
        return Command.SINGLE_SUCCESS;
    }

    private static int setPrefix(FabricClientCommandSource source, MessagePrefixMode mode) {
        ConfigService.get().config().commands.prefix = mode;
        return savePrefix(source);
    }

    private static int setCustomPrefix(FabricClientCommandSource source, String value) {
        ConfigService.get().config().commands.prefix = MessagePrefixMode.CUSTOM;
        ConfigService.get().config().commands.customPrefix = value;
        return savePrefix(source);
    }

    private static int savePrefix(FabricClientCommandSource source) {
        boolean saved = ConfigService.get().save();
        MessageService.send(source, Text.translatable(
                saved ? "command.dotmod.prefix.saved" : "message.dotmod.config.save_failed"
        ), saved ? MessageType.SUCCESS : MessageType.ERROR);
        return saved ? Command.SINGLE_SUCCESS : 0;
    }
}
