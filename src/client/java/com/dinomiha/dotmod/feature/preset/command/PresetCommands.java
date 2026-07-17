package com.dinomiha.dotmod.feature.preset.command;

import com.dinomiha.dotmod.feature.preset.PresetClientService;
import com.dinomiha.dotmod.feature.preset.PresetException;
import com.dinomiha.dotmod.feature.preset.PresetError;
import com.dinomiha.dotmod.feature.preset.PresetNameValidator;
import com.dinomiha.dotmod.feature.preset.PresetRecord;
import com.dinomiha.dotmod.feature.preset.helper.PresetHelperClientService;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.dinomiha.dotmod.ui.component.DotConfirmationDialog;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class PresetCommands {
    private PresetCommands() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build(String commandRoot) {
        return literal("pst")
                .executes(context -> list(context.getSource(), commandRoot))
                .then(literal("lst").executes(context -> list(context.getSource(), commandRoot)))
                .then(literal("slc").then(existingName("name")
                        .executes(context -> select(context.getSource(), name(context, "name")))))
                .then(literal("crt").then(argument("name", StringArgumentType.string())
                        .executes(context -> create(context.getSource(), name(context, "name")))))
                .then(literal("dlt").then(existingName("name")
                        .executes(context -> requestDelete(context.getSource(), name(context, "name")))))
                .then(literal("shw").then(existingName("name")
                        .executes(context -> show(context.getSource(), name(context, "name")))))
                .then(literal("hlp")
                        .executes(context -> helper(context.getSource(), null))
                        .then(existingName("name")
                                .executes(context -> helper(context.getSource(), name(context, "name")))))
                .then(literal("ren").then(existingName("name")
                        .then(argument("new_name", StringArgumentType.string())
                                .executes(context -> rename(
                                        context.getSource(),
                                        name(context, "name"),
                                        name(context, "new_name")
                                )))))
                .then(literal("dup").then(existingName("name")
                        .then(argument("new_name", StringArgumentType.string())
                                .executes(context -> duplicate(
                                        context.getSource(),
                                        name(context, "name"),
                                        name(context, "new_name")
                                )))))
                .then(literal("exp").then(existingName("name")
                        .executes(context -> exportPreset(context.getSource(), name(context, "name")))))
                .then(literal("imp").executes(context -> importPreset(context.getSource())));
    }

    private static int list(FabricClientCommandSource source, String root) {
        return run(source, () -> {
            List<PresetRecord> records = PresetClientService.list(source.getClient());
            if (records.isEmpty()) {
                MessageService.send(source, Text.translatable("command.dotmod.preset.list.empty"), MessageType.INFO);
                return;
            }
            MessageService.send(source, Text.translatable("command.dotmod.preset.list.header", records.size()), MessageType.INFO);
            for (PresetRecord record : records) {
                String escaped = StringArgumentType.escapeIfRequired(record.preset().name());
                MutableText line = Text.literal(record.active() ? "* " : "  ").append(record.preset().name()).append(Text.literal(" "));
                line.append(MessageService.commandAction(
                        Text.translatable("command.dotmod.preset.action.show"),
                        "/" + root + " pst shw " + escaped,
                        Text.translatable("command.dotmod.preset.action.show.tooltip")
                ));
                line.append(Text.literal(" "));
                line.append(MessageService.commandAction(
                        Text.translatable("command.dotmod.preset.action.select"),
                        "/" + root + " pst slc " + escaped,
                        Text.translatable("command.dotmod.preset.action.select.tooltip")
                ));
                MessageService.send(source, line, MessageType.INFO);
            }
        });
    }

    private static int select(FabricClientCommandSource source, String name) {
        return run(source, () -> {
            PresetRecord record = PresetClientService.require(source.getClient(), name);
            PresetClientService.select(source.getClient(), record);
            MessageService.send(source, Text.translatable("command.dotmod.preset.selected", record.preset().name()), MessageType.SUCCESS);
        });
    }

    private static int create(FabricClientCommandSource source, String name) {
        return run(source, () -> {
            String normalized = PresetNameValidator.normalize(name);
            String key = PresetNameValidator.conflictKey(normalized);
            boolean conflict = PresetClientService.list(source.getClient()).stream()
                    .anyMatch(record -> PresetNameValidator.conflictKey(record.preset().name()).equals(key));
            if (conflict) {
                throw new PresetException(PresetError.NAME_CONFLICT, "Preset name already exists");
            }
            source.getClient().send(() -> {
            try {
                PresetClientService.openCreate(source.getClient(), null, normalized);
            } catch (PresetException exception) {
                PresetClientService.report(exception);
            }
            });
        });
    }

    private static int show(FabricClientCommandSource source, String name) {
        return run(source, () -> {
            PresetRecord record = PresetClientService.require(source.getClient(), name);
            source.getClient().send(() -> {
                try {
                    PresetRecord fresh = PresetClientService.require(source.getClient(), record.preset().id());
                    if (!fresh.revision().equals(record.revision())) {
                        throw new PresetException(PresetError.STALE_DATA, "Preset changed before opening");
                    }
                    PresetClientService.openView(source.getClient(), null, fresh);
                } catch (PresetException exception) {
                    PresetClientService.report(exception);
                }
            });
        });
    }

    private static int helper(FabricClientCommandSource source, String name) {
        return run(source, () -> {
            PresetRecord record;
            if (name == null) {
                record = PresetClientService.active(source.getClient()).orElse(null);
                if (record == null) {
                    MessageService.send(source, Text.translatable("command.dotmod.preset.helper.no_active"), MessageType.WARNING);
                    return;
                }
            } else {
                record = PresetClientService.require(source.getClient(), name);
            }
            PresetRecord requested = record;
            source.getClient().send(() -> {
                try {
                    PresetRecord fresh = PresetClientService.require(source.getClient(), requested.preset().id());
                    if (!fresh.revision().equals(requested.revision())) {
                        throw new PresetException(PresetError.STALE_DATA, "Preset changed before helper capture");
                    }
                    PresetHelperClientService.open(source.getClient(), null, fresh);
                } catch (PresetException exception) {
                    PresetClientService.report(exception);
                }
            });
        });
    }

    private static int rename(FabricClientCommandSource source, String name, String newName) {
        return run(source, () -> {
            PresetRecord record = PresetClientService.require(source.getClient(), name);
            PresetRecord renamed = PresetClientService.rename(source.getClient(), record, newName);
            MessageService.send(source, Text.translatable("command.dotmod.preset.renamed", renamed.preset().name()), MessageType.SUCCESS);
        });
    }

    private static int duplicate(FabricClientCommandSource source, String name, String newName) {
        return run(source, () -> {
            PresetRecord record = PresetClientService.require(source.getClient(), name);
            PresetRecord duplicate = PresetClientService.duplicate(source.getClient(), record, newName);
            MessageService.send(source, Text.translatable("command.dotmod.preset.duplicated", duplicate.preset().name()), MessageType.SUCCESS);
        });
    }

    private static int exportPreset(FabricClientCommandSource source, String name) {
        return run(source, () -> {
            PresetRecord record = PresetClientService.require(source.getClient(), name);
            source.getClient().keyboard.setClipboard(PresetClientService.exportPreset(source.getClient(), record));
            MessageService.send(source, Text.translatable("command.dotmod.preset.exported", record.preset().name()), MessageType.SUCCESS);
        });
    }

    private static int importPreset(FabricClientCommandSource source) {
        return run(source, () -> {
            PresetRecord imported = PresetClientService.importPreset(source.getClient(), source.getClient().keyboard.getClipboard());
            MessageService.send(source, Text.translatable("command.dotmod.preset.imported", imported.preset().name()), MessageType.SUCCESS);
        });
    }

    private static int requestDelete(FabricClientCommandSource source, String name) {
        return run(source, () -> {
            PresetRecord record = PresetClientService.require(source.getClient(), name);
            source.getClient().send(() -> DotConfirmationDialog.open(
                    source.getClient(),
                    null,
                    Text.translatable("command.dotmod.preset.delete.title"),
                    Text.translatable("command.dotmod.preset.delete.message", record.preset().name()),
                    () -> {
                        try {
                            PresetClientService.delete(source.getClient(), record);
                            MessageService.sendChat(Text.translatable("command.dotmod.preset.deleted", record.preset().name()), MessageType.SUCCESS);
                        } catch (PresetException exception) {
                            PresetClientService.report(exception);
                        }
                    }
            ));
        });
    }

    private static RequiredArgumentBuilder<FabricClientCommandSource, String> existingName(String argumentName) {
        return argument(argumentName, StringArgumentType.string())
                .suggests((context, builder) -> suggestNames(context.getSource(), builder));
    }

    private static CompletableFuture<Suggestions> suggestNames(FabricClientCommandSource source, SuggestionsBuilder builder) {
        try {
            String probe = builder.getRemainingLowerCase().replaceFirst("^\"", "");
            for (PresetRecord record : PresetClientService.list(source.getClient())) {
                String name = record.preset().name();
                if (CommandSource.shouldSuggest(probe, name.toLowerCase(Locale.ROOT))) {
                    builder.suggest(StringArgumentType.escapeIfRequired(name));
                }
            }
        } catch (PresetException ignored) {
        }
        return builder.buildFuture();
    }

    private static String name(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context, String argument) {
        return StringArgumentType.getString(context, argument);
    }

    private static int run(FabricClientCommandSource source, Action action) {
        try {
            action.run();
            return Command.SINGLE_SUCCESS;
        } catch (PresetException exception) {
            MessageService.send(source, errorText(exception), MessageType.ERROR);
            return 0;
        }
    }

    private static Text errorText(PresetException exception) {
        return Text.translatable("message.dotmod.preset.error." + exception.error().name().toLowerCase(Locale.ROOT));
    }

    @FunctionalInterface
    private interface Action {
        void run();
    }
}
