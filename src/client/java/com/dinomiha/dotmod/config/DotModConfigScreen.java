package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.DotModClient;
import com.dinomiha.dotmod.hud.widget.HudAnchor;
import com.dinomiha.dotmod.hud.widget.HudWidgetDefaults;
import com.dinomiha.dotmod.keybind.DotModKeybinds;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.dinomiha.dotmod.util.ColorUtil;
import com.dinomiha.dotmod.util.SlotListParser;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DotModConfigScreen {
    private DotModConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigService service = ConfigService.get();
        DotModConfig config = service.config();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal(DotModClient.MOD_NAME))
                .setSavingRunnable(() -> {
                    MessageType type = service.save() ? MessageType.SUCCESS : MessageType.ERROR;
                    MessageService.sendChat(Text.translatable(
                            type == MessageType.SUCCESS ? "message.dotmod.config.saved" : "message.dotmod.config.save_failed"
                    ), type);
                });
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.general"));
        general.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.general.enabled"), config.general.enabled)
                .setSaveConsumer(value -> config.general.enabled = value)
                .build());
        general.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.death_history.enabled"), config.deathHistory.enabled)
                .setSaveConsumer(value -> config.deathHistory.enabled = value)
                .build());
        general.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.screenshots.enabled"), config.screenshots.enabled)
                .setSaveConsumer(value -> config.screenshots.enabled = value)
                .build());

        ConfigCategory commands = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.commands"));
        commands.addEntry(entries.startEnumSelector(
                        Text.translatable("config.dotmod.commands.prefix"),
                        MessagePrefixMode.class,
                        config.commands.prefix
                )
                .setEnumNameProvider(value -> Text.translatable(
                        "config.dotmod.commands.prefix." + value.name().toLowerCase(Locale.ROOT)
                ))
                .setSaveConsumer(value -> config.commands.prefix = value)
                .build());
        commands.addEntry(entries.startStrField(Text.translatable("config.dotmod.commands.custom_prefix"), config.commands.customPrefix)
                .setTooltip(Text.translatable("config.dotmod.commands.custom_prefix.tooltip"))
                .setSaveConsumer(value -> config.commands.customPrefix = value)
                .build());
        commands.addEntry(entries.startBooleanToggle(
                        Text.translatable("config.dotmod.command_aliases.enabled"), config.commandAliases.enabled)
                .setSaveConsumer(value -> config.commandAliases.enabled = value)
                .build());

        ConfigCategory quickCraft = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.quick_craft"));
        quickCraft.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.quick_craft.enabled"), config.quickCraft.enabled)
                .setSaveConsumer(value -> config.quickCraft.enabled = value)
                .build());

        ConfigCategory presets = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.inventory_presets"));
        presets.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.inventory_presets.enabled"), config.inventoryPresets.enabled)
                .setSaveConsumer(value -> config.inventoryPresets.enabled = value)
                .build());
        presets.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.inventory_presets.expanded"), config.inventoryPresets.panelExpanded)
                .setSaveConsumer(value -> config.inventoryPresets.panelExpanded = value)
                .build());
        presets.addEntry(entries.startEnumSelector(
                        Text.translatable("config.dotmod.inventory_presets.side"),
                        PresetPanelSide.class,
                        config.inventoryPresets.panelSide
                )
                .setEnumNameProvider(value -> Text.translatable(
                        "config.dotmod.inventory_presets.side." + value.name().toLowerCase(Locale.ROOT)
                ))
                .setSaveConsumer(value -> config.inventoryPresets.panelSide = value)
                .build());

        ConfigCategory inventorySearch = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.inventory_search"));
        inventorySearch.addEntry(entries.startBooleanToggle(
                        Text.translatable("config.dotmod.inventory_search.enabled"), config.inventorySearch.enabled)
                .setSaveConsumer(value -> config.inventorySearch.enabled = value)
                .build());
        inventorySearch.addEntry(entries.startEnumSelector(
                        Text.translatable("config.dotmod.inventory_search.mode"),
                        InventorySearchDisplayMode.class,
                        config.inventorySearch.displayMode)
                .setEnumNameProvider(value -> Text.translatable(
                        "config.dotmod.inventory_search.mode." + value.name().toLowerCase(Locale.ROOT)))
                .setSaveConsumer(value -> config.inventorySearch.displayMode = value)
                .build());
        quickCraft.addEntry(entries.startStrField(Text.translatable("config.dotmod.quick_craft.slots_2x2"), SlotListParser.format(config.quickCraft.slots2x2))
                .setTooltip(Text.translatable("config.dotmod.quick_craft.slots_2x2.tooltip"))
                .setSaveConsumer(value -> config.quickCraft.slots2x2 = SlotListParser.parse(value, List.of(9, 10, 18, 19)))
                .build());
        quickCraft.addEntry(entries.startStrField(Text.translatable("config.dotmod.quick_craft.slots_3x3"), SlotListParser.format(config.quickCraft.slots3x3))
                .setTooltip(Text.translatable("config.dotmod.quick_craft.slots_3x3.tooltip"))
                .setSaveConsumer(value -> config.quickCraft.slots3x3 = SlotListParser.parse(value, List.of(9, 10, 11, 18, 19, 20, 27, 28, 29)))
                .build());
        quickCraft.addEntry(entries.startIntField(Text.translatable("config.dotmod.common.button_offset_x"), config.quickCraft.buttonOffsetX)
                .setSaveConsumer(value -> config.quickCraft.buttonOffsetX = value)
                .build());
        quickCraft.addEntry(entries.startIntField(Text.translatable("config.dotmod.common.button_offset_y"), config.quickCraft.buttonOffsetY)
                .setSaveConsumer(value -> config.quickCraft.buttonOffsetY = value)
                .build());
        quickCraft.addEntry(entries.startStrField(Text.translatable("config.dotmod.common.button_text"), config.quickCraft.buttonText)
                .setSaveConsumer(value -> config.quickCraft.buttonText = value)
                .build());
        quickCraft.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.quick_craft.reset_slots"), false)
                .setTooltip(Text.translatable("config.dotmod.quick_craft.reset_slots.tooltip"))
                .setSaveConsumer(value -> {
                    if (value) {
                        config.quickCraft.slots2x2 = new ArrayList<>(List.of(9, 10, 18, 19));
                        config.quickCraft.slots3x3 = new ArrayList<>(List.of(9, 10, 11, 18, 19, 20, 27, 28, 29));
                    }
                })
                .build());

        ConfigCategory hud = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.hud"));
        hud.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.hud.enabled"), config.hud.editorEnabled)
                .setSaveConsumer(value -> config.hud.editorEnabled = value)
                .build());
        hud.addEntry(entries.startIntField(Text.translatable("config.dotmod.hud.button_offset_x"), config.hud.editorButtonOffsetX)
                .setSaveConsumer(value -> config.hud.editorButtonOffsetX = value)
                .build());
        hud.addEntry(entries.startIntField(Text.translatable("config.dotmod.hud.button_offset_y"), config.hud.editorButtonOffsetY)
                .setSaveConsumer(value -> config.hud.editorButtonOffsetY = value)
                .build());
        hud.addEntry(entries.startStrField(Text.translatable("config.dotmod.hud.button_text"), config.hud.editorButtonText)
                .setSaveConsumer(value -> config.hud.editorButtonText = value)
                .build());
        hud.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.hud.snap_to_grid"), config.hud.snapToGrid)
                .setSaveConsumer(value -> config.hud.snapToGrid = value)
                .build());
        hud.addEntry(entries.startIntSlider(Text.translatable("config.dotmod.hud.grid_size"), config.hud.gridSize, 1, 16)
                .setSaveConsumer(value -> config.hud.gridSize = value)
                .build());
        hud.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.hud.magnetic_snapping"), config.hud.magneticSnapping)
                .setTooltip(Text.translatable("config.dotmod.hud.magnetic_snapping.tooltip"))
                .setSaveConsumer(value -> config.hud.magneticSnapping = value)
                .build());
        hud.addEntry(entries.startIntSlider(Text.translatable("config.dotmod.hud.magnetic_distance"), config.hud.magneticSnapDistance, 1, 16)
                .setTooltip(Text.translatable("config.dotmod.hud.magnetic_distance.tooltip"))
                .setSaveConsumer(value -> config.hud.magneticSnapDistance = value)
                .build());
        for (var definition : HudWidgetDefaults.definitions()) {
            var settings = config.hud.widget(definition.id());
            Text elementName = Text.translatable(definition.translationKey());
            hud.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.hud.visible", elementName), settings.visible)
                    .setSaveConsumer(value -> config.hud.widget(definition.id()).visible = value)
                    .build());
            hud.addEntry(entries.startEnumSelector(
                            Text.translatable("config.dotmod.hud.anchor", elementName), HudAnchor.class, settings.anchor)
                    .setEnumNameProvider(value -> Text.translatable(
                            "config.dotmod.hud.anchor." + value.name().toLowerCase(Locale.ROOT)))
                    .setSaveConsumer(value -> config.hud.widget(definition.id()).anchor = value)
                    .build());
            hud.addEntry(entries.startIntField(Text.translatable("config.dotmod.hud.offset_x", elementName), settings.offsetX)
                    .setSaveConsumer(value -> config.hud.widget(definition.id()).offsetX = value)
                    .build());
            hud.addEntry(entries.startIntField(Text.translatable("config.dotmod.hud.offset_y", elementName), settings.offsetY)
                    .setSaveConsumer(value -> config.hud.widget(definition.id()).offsetY = value)
                    .build());
            hud.addEntry(entries.startFloatField(Text.translatable("config.dotmod.hud.scale", elementName), settings.scale)
                    .setMin(0.25F).setMax(4.0F)
                    .setSaveConsumer(value -> config.hud.widget(definition.id()).scale = value)
                    .build());
            if (definition.custom()) {
                hud.addEntry(entries.startFloatField(Text.translatable("config.dotmod.hud.alpha", elementName), settings.alpha)
                        .setMin(0.0F).setMax(1.0F)
                        .setSaveConsumer(value -> config.hud.widget(definition.id()).alpha = value)
                        .build());
            }
        }
        hud.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.hud.reset_offsets"), false)
                .setSaveConsumer(value -> {
                    if (value) {
                        config.hud.resetOffsets();
                    }
                })
                .build());

        ConfigCategory durability = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.durability"));
        durability.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.durability.enabled"), config.durability.enabled)
                .setSaveConsumer(value -> config.durability.enabled = value)
                .build());
        durability.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.durability.warnings"), config.durability.warningsEnabled)
                .setSaveConsumer(value -> config.durability.warningsEnabled = value)
                .build());
        durability.addEntry(entries.startFloatField(Text.translatable("config.dotmod.durability.threshold"), config.durability.warningThreshold)
                .setMin(0.0F).setMax(1.0F)
                .setSaveConsumer(value -> config.durability.warningThreshold = value)
                .build());
        durability.addEntry(entries.startIntField(Text.translatable("config.dotmod.durability.cooldown"), config.durability.warningCooldownSeconds)
                .setMin(0).setMax(86400)
                .setSaveConsumer(value -> config.durability.warningCooldownSeconds = value)
                .build());
        durability.addEntry(entries.startStrField(Text.translatable("config.dotmod.durability.low_color"), config.durability.lowColor)
                .setSaveConsumer(value -> config.durability.lowColor = ColorUtil.normalizeHex(value, "#FF5555"))
                .build());
        durability.addEntry(entries.startStrField(Text.translatable("config.dotmod.durability.middle_color"), config.durability.middleColor)
                .setSaveConsumer(value -> config.durability.middleColor = ColorUtil.normalizeHex(value, "#FFFF55"))
                .build());
        durability.addEntry(entries.startStrField(Text.translatable("config.dotmod.durability.high_color"), config.durability.highColor)
                .setSaveConsumer(value -> config.durability.highColor = ColorUtil.normalizeHex(value, "#55FF55"))
                .build());

        ConfigCategory nameColors = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.player_colors"));
        nameColors.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.player_colors.enabled"), config.playerColors.enabled)
                .setSaveConsumer(value -> config.playerColors.enabled = value)
                .build());
        nameColors.addEntry(entries.startStrField(Text.translatable("config.dotmod.player_colors.green"), config.playerColors.greenColor)
                .setSaveConsumer(value -> config.playerColors.greenColor = ColorUtil.normalizeHex(value, "#55FF55"))
                .build());
        nameColors.addEntry(entries.startStrField(Text.translatable("config.dotmod.player_colors.red"), config.playerColors.redColor)
                .setSaveConsumer(value -> config.playerColors.redColor = ColorUtil.normalizeHex(value, "#FF5555"))
                .build());
        nameColors.addEntry(entries.startStrField(Text.translatable("config.dotmod.player_colors.default"), config.playerColors.defaultColor)
                .setSaveConsumer(value -> config.playerColors.defaultColor = ColorUtil.normalizeHex(value, "#FFFFFF"))
                .build());
        nameColors.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.player_colors.persist"), config.playerColors.persist)
                .setSaveConsumer(value -> config.playerColors.persist = value)
                .build());
        nameColors.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.player_colors.notify"), config.playerColors.notifyChanges)
                .setSaveConsumer(value -> config.playerColors.notifyChanges = value)
                .build());

        ConfigCategory uniformNameTags = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.name_tags"));
        uniformNameTags.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.name_tags.enabled"), config.hud.uniformNameTags.enabled)
                .setSaveConsumer(value -> config.hud.uniformNameTags.enabled = value)
                .build());
        uniformNameTags.addEntry(entries.startFloatField(Text.translatable("config.dotmod.name_tags.size"), config.hud.uniformNameTags.size)
                .setTooltip(Text.translatable("config.dotmod.name_tags.size.tooltip"))
                .setMin(0.1F)
                .setMax(5.0F)
                .setSaveConsumer(value -> config.hud.uniformNameTags.size = value)
                .build());
        uniformNameTags.addEntry(entries.startStrField(Text.translatable("config.dotmod.name_tags.background"), config.hud.uniformNameTags.backgroundColor)
                .setTooltip(Text.translatable("config.dotmod.name_tags.background.tooltip"))
                .setSaveConsumer(value -> config.hud.uniformNameTags.backgroundColor = ColorUtil.normalizeHex(value, "#000000"))
                .build());

        ConfigCategory movement = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.movement"));
        movement.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.toggle_walk.enabled"), config.toggleWalk.enabled)
                .setTooltip(Text.translatable("config.dotmod.toggle_walk.enabled.tooltip"))
                .setSaveConsumer(value -> config.toggleWalk.enabled = value)
                .build());
        movement.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.toggle_walk.retain_sprint"), config.toggleWalk.retainSprint)
                .setTooltip(Text.translatable("config.dotmod.toggle_walk.retain_sprint.tooltip"))
                .setSaveConsumer(value -> config.toggleWalk.retainSprint = value)
                .build());
        movement.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.toggle_walk.deactivate_chat"), config.toggleWalk.deactivateInChat)
                .setSaveConsumer(value -> config.toggleWalk.deactivateInChat = value)
                .build());
        movement.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.toggle_walk.deactivate_screens"), config.toggleWalk.deactivateInOtherScreens)
                .setSaveConsumer(value -> config.toggleWalk.deactivateInOtherScreens = value)
                .build());
        movement.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.toggle_shift.enabled"), config.toggleWalk.toggleShift.enabled)
                .setSaveConsumer(value -> config.toggleWalk.toggleShift.enabled = value)
                .build());

        ConfigCategory fullBrightness = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.full_brightness"));
        fullBrightness.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.full_brightness.enabled"), config.fullBrightness.enabled)
                .setTooltip(Text.translatable("config.dotmod.full_brightness.enabled.tooltip"))
                .setSaveConsumer(value -> config.fullBrightness.enabled = value)
                .build());

        ConfigCategory freelook = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.freelook"));
        freelook.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.freelook.enabled"), config.freelook.enabled)
                .setTooltip(Text.translatable("config.dotmod.freelook.enabled.tooltip"))
                .setSaveConsumer(value -> config.freelook.enabled = value).build());
        freelook.addEntry(entries.startEnumSelector(Text.translatable("config.dotmod.freelook.activation"),
                        com.dinomiha.dotmod.config.FreelookActivation.class, config.freelook.activation)
                .setEnumNameProvider(value -> Text.translatable("config.dotmod.freelook.activation." + value.name().toLowerCase(Locale.ROOT)))
                .setSaveConsumer(value -> config.freelook.activation = value).build());
        freelook.addEntry(entries.startFloatField(Text.translatable("config.dotmod.freelook.sensitivity"), config.freelook.sensitivity)
                .setMin(0.1F).setMax(4.0F).setSaveConsumer(value -> config.freelook.sensitivity = value).build());
        freelook.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.freelook.invert_x"), config.freelook.invertX)
                .setSaveConsumer(value -> config.freelook.invertX = value).build());
        freelook.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.freelook.invert_y"), config.freelook.invertY)
                .setSaveConsumer(value -> config.freelook.invertY = value).build());
        freelook.addEntry(entries.startIntSlider(Text.translatable("config.dotmod.freelook.return_duration"), config.freelook.returnDurationMs, 0, 1000)
                .setTooltip(Text.translatable("config.dotmod.freelook.return_duration.tooltip"))
                .setSaveConsumer(value -> config.freelook.returnDurationMs = value).build());
        freelook.addEntry(entries.startBooleanToggle(Text.translatable("config.dotmod.freelook.indicator"), config.freelook.showIndicator)
                .setSaveConsumer(value -> config.freelook.showIndicator = value).build());

        ConfigCategory keybinds = builder.getOrCreateCategory(Text.translatable("config.dotmod.category.keybinds"));
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.green_name")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.red_name")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.reset_name")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.uniform_name_tags")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.toggle_shift")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.toggle_walk")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.emergency_release")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.freelook")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.full_brightness")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.preset_helper")).build());
        keybinds.addEntry(entries.startTextDescription(DotModKeybinds.description("key.dotmod.fast_commands")).build());
        keybinds.addEntry(entries.startTextDescription(Text.translatable("config.dotmod.keybinds.controls_hint")).build());

        return builder.build();
    }
}
