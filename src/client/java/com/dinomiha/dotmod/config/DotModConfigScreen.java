package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.DotModClient;
import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.util.ColorUtil;
import com.dinomiha.dotmod.util.SlotListParser;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class DotModConfigScreen {
    private DotModConfigScreen() {
    }

    public static Screen create(Screen parent) {
        DotModConfig config = DotModConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal(DotModClient.MOD_NAME))
                .setSavingRunnable(DotModConfig::save);
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        general.addEntry(entries.startBooleanToggle(Text.literal("Enable dotMOD"), config.modEnabled)
                .setSaveConsumer(value -> config.modEnabled = value)
                .build());

        ConfigCategory quickCraft = builder.getOrCreateCategory(Text.literal("Quick Craft"));
        quickCraft.addEntry(entries.startBooleanToggle(Text.literal("Enable Quick Craft"), config.quickCraftEnabled)
                .setSaveConsumer(value -> config.quickCraftEnabled = value)
                .build());
        quickCraft.addEntry(entries.startStrField(Text.literal("2x2 source slots"), SlotListParser.format(config.quickCraftSlots2x2))
                .setTooltip(Text.literal("Logical player inventory slots 0-35. Default: 9,10,18,19."))
                .setSaveConsumer(value -> config.quickCraftSlots2x2 = SlotListParser.parse(value, List.of(9, 10, 18, 19)))
                .build());
        quickCraft.addEntry(entries.startStrField(Text.literal("3x3 source slots"), SlotListParser.format(config.quickCraftSlots3x3))
                .setTooltip(Text.literal("Logical player inventory slots 0-35. Default: 9,10,11,18,19,20,27,28,29."))
                .setSaveConsumer(value -> config.quickCraftSlots3x3 = SlotListParser.parse(value, List.of(9, 10, 11, 18, 19, 20, 27, 28, 29)))
                .build());
        quickCraft.addEntry(entries.startIntField(Text.literal("Button offset X"), config.quickCraftButtonOffsetX)
                .setSaveConsumer(value -> config.quickCraftButtonOffsetX = value)
                .build());
        quickCraft.addEntry(entries.startIntField(Text.literal("Button offset Y"), config.quickCraftButtonOffsetY)
                .setSaveConsumer(value -> config.quickCraftButtonOffsetY = value)
                .build());
        quickCraft.addEntry(entries.startStrField(Text.literal("Button text"), config.quickCraftButtonText)
                .setSaveConsumer(value -> config.quickCraftButtonText = value == null || value.isBlank() ? "Craft" : value)
                .build());
        quickCraft.addEntry(entries.startTextDescription(Text.literal("Presets: use Reset to restore default slot layouts.")).build());
        quickCraft.addEntry(entries.startBooleanToggle(Text.literal("Reset Quick Craft slot presets on save"), false)
                .setSaveConsumer(value -> {
                    if (value) {
                        config.quickCraftSlots2x2 = new ArrayList<>(List.of(9, 10, 18, 19));
                        config.quickCraftSlots3x3 = new ArrayList<>(List.of(9, 10, 11, 18, 19, 20, 27, 28, 29));
                    }
                })
                .build());

        ConfigCategory hud = builder.getOrCreateCategory(Text.literal("HUD Editor"));
        hud.addEntry(entries.startBooleanToggle(Text.literal("Enable HUD Editor"), config.hudEditorEnabled)
                .setSaveConsumer(value -> config.hudEditorEnabled = value)
                .build());
        hud.addEntry(entries.startIntField(Text.literal("HUD button offset X"), config.hudEditorButtonOffsetX)
                .setSaveConsumer(value -> config.hudEditorButtonOffsetX = value)
                .build());
        hud.addEntry(entries.startIntField(Text.literal("HUD button offset Y"), config.hudEditorButtonOffsetY)
                .setSaveConsumer(value -> config.hudEditorButtonOffsetY = value)
                .build());
        hud.addEntry(entries.startStrField(Text.literal("HUD button text"), config.hudEditorButtonText)
                .setSaveConsumer(value -> config.hudEditorButtonText = value == null || value.isBlank() ? "HUD" : value)
                .build());
        hud.addEntry(entries.startBooleanToggle(Text.literal("Snap to grid"), config.hudSnapToGrid)
                .setSaveConsumer(value -> config.hudSnapToGrid = value)
                .build());
        hud.addEntry(entries.startIntSlider(Text.literal("Grid size"), config.hudGridSize, 1, 16)
                .setSaveConsumer(value -> config.hudGridSize = Math.max(1, value))
                .build());
        hud.addEntry(entries.startBooleanToggle(Text.literal("Magnetic snapping"), config.hudMagneticSnapping)
                .setTooltip(Text.literal("Snap to zero offsets, edges and centers of other HUD elements."))
                .setSaveConsumer(value -> config.hudMagneticSnapping = value)
                .build());
        hud.addEntry(entries.startIntSlider(Text.literal("Magnetic snap distance"), config.hudMagneticSnapDistance, 1, 16)
                .setTooltip(Text.literal("Maximum alignment distance in pixels."))
                .setSaveConsumer(value -> config.hudMagneticSnapDistance = Math.max(1, Math.min(16, value)))
                .build());
        for (HudElement element : HudElement.values()) {
            DotModConfig.HudOffset offset = config.hudOffset(element);
            hud.addEntry(entries.startIntField(Text.literal(element.displayName() + " dx"), offset.dx)
                    .setSaveConsumer(value -> config.hudOffset(element).dx = value)
                    .build());
            hud.addEntry(entries.startIntField(Text.literal(element.displayName() + " dy"), offset.dy)
                    .setSaveConsumer(value -> config.hudOffset(element).dy = value)
                    .build());
        }
        hud.addEntry(entries.startBooleanToggle(Text.literal("Reset HUD offsets on save"), false)
                .setSaveConsumer(value -> {
                    if (value) {
                        DotModConfig.resetHud();
                    }
                })
                .build());

        ConfigCategory nameColors = builder.getOrCreateCategory(Text.literal("Name Colors"));
        nameColors.addEntry(entries.startBooleanToggle(Text.literal("Enable Name Colors"), config.nameColorsEnabled)
                .setSaveConsumer(value -> config.nameColorsEnabled = value)
                .build());
        nameColors.addEntry(entries.startStrField(Text.literal("Green color"), config.greenColor)
                .setSaveConsumer(value -> config.greenColor = ColorUtil.normalizeHex(value, "#55FF55"))
                .build());
        nameColors.addEntry(entries.startStrField(Text.literal("Red color"), config.redColor)
                .setSaveConsumer(value -> config.redColor = ColorUtil.normalizeHex(value, "#FF5555"))
                .build());
        nameColors.addEntry(entries.startStrField(Text.literal("Default color"), config.defaultColor)
                .setSaveConsumer(value -> config.defaultColor = ColorUtil.normalizeHex(value, "#FFFFFF"))
                .build());
        nameColors.addEntry(entries.startBooleanToggle(Text.literal("Persist colors between restarts"), config.persistNameColors)
                .setSaveConsumer(value -> config.persistNameColors = value)
                .build());
        nameColors.addEntry(entries.startBooleanToggle(Text.literal("Notify color changes in chat"), config.notifyNameColorChanges)
                .setSaveConsumer(value -> config.notifyNameColorChanges = value)
                .build());

        ConfigCategory uniformNameTags = builder.getOrCreateCategory(Text.literal("Uniform Name Tags"));
        uniformNameTags.addEntry(entries.startBooleanToggle(Text.literal("Enable Uniform Name Tags"), config.uniformNameTagsEnabled)
                .setSaveConsumer(value -> config.uniformNameTagsEnabled = value)
                .build());
        uniformNameTags.addEntry(entries.startFloatField(Text.literal("Name tag size"), config.uniformNameTagSize)
                .setTooltip(Text.literal("Screen-space size multiplier from 0.1 to 5.0."))
                .setMin(0.1F)
                .setMax(5.0F)
                .setSaveConsumer(value -> config.uniformNameTagSize = Math.max(0.1F, Math.min(5.0F, value)))
                .build());
        uniformNameTags.addEntry(entries.startStrField(Text.literal("Background color"), config.uniformNameTagBackgroundColor)
                .setTooltip(Text.literal("Opaque RGB color in #RRGGBB format."))
                .setSaveConsumer(value -> config.uniformNameTagBackgroundColor = ColorUtil.normalizeHex(value, "#000000"))
                .build());

        ConfigCategory toggleShift = builder.getOrCreateCategory(Text.literal("Toggle Shift"));
        toggleShift.addEntry(entries.startBooleanToggle(Text.literal("Enable Toggle Shift"), config.toggleShiftEnabled)
                .setSaveConsumer(value -> config.toggleShiftEnabled = value)
                .build());

        ConfigCategory keybinds = builder.getOrCreateCategory(Text.literal("Keybinds info"));
        keybinds.addEntry(entries.startTextDescription(Text.literal("Green name: G")).build());
        keybinds.addEntry(entries.startTextDescription(Text.literal("Red name: R")).build());
        keybinds.addEntry(entries.startTextDescription(Text.literal("Reset name color: V")).build());
        keybinds.addEntry(entries.startTextDescription(Text.literal("Uniform name tags: N")).build());
        keybinds.addEntry(entries.startTextDescription(Text.literal("Toggle Shift: Right Shift")).build());
        keybinds.addEntry(entries.startTextDescription(Text.literal("All keybinds are editable in Minecraft Controls.")).build());

        return builder.build();
    }
}
