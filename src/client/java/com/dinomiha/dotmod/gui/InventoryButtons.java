package com.dinomiha.dotmod.gui;

import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public final class InventoryButtons {
    private InventoryButtons() {
    }

    public static void add(HandledScreen<?> screen, ButtonAdder adder, int x, int y, int backgroundWidth) {
        DotModConfig config = DotModConfig.get();
        if (!config.modEnabled) {
            return;
        }
        boolean quickCraftSupported = screen instanceof InventoryScreen || screen instanceof CraftingScreen;
        boolean hudSupported = quickCraftSupported || screen instanceof CreativeInventoryScreen;
        if (!hudSupported) {
            return;
        }
        int right = x + backgroundWidth;
        ButtonWidget quickCraftButton;
        if (quickCraftSupported && config.quickCraftEnabled) {
            String label = config.quickCraftButtonText == null || config.quickCraftButtonText.isBlank() ? "Craft" : config.quickCraftButtonText;
            int width = Math.max(44, MinecraftClient.getInstance().textRenderer.getWidth(label) + 16);
            quickCraftButton = ButtonWidget.builder(Text.literal(label), button -> QuickCraft.perform(screen.getScreenHandler()))
                    .dimensions(right + config.quickCraftButtonOffsetX, y + config.quickCraftButtonOffsetY, width, 20)
                    .build();
            adder.add(quickCraftButton);
        } else {
            quickCraftButton = null;
        }
        ButtonWidget hudButton;
        if (config.hudEditorEnabled) {
            String label = config.hudEditorButtonText == null || config.hudEditorButtonText.isBlank() ? "HUD" : config.hudEditorButtonText;
            int width = Math.max(44, MinecraftClient.getInstance().textRenderer.getWidth(label) + 16);
            hudButton = ButtonWidget.builder(Text.literal(label), button -> MinecraftClient.getInstance().setScreen(new HudEditorScreen(screen)))
                    .dimensions(right + config.hudEditorButtonOffsetX, y + config.hudEditorButtonOffsetY, width, 20)
                    .build();
            adder.add(hudButton);
        } else {
            hudButton = null;
        }

        ScreenEvents.beforeRender(screen).register((ignored, context, mouseX, mouseY, tickDelta) -> {
            HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
            int currentRight = accessor.dotmod$getX() + accessor.dotmod$getBackgroundWidth();
            DotModConfig currentConfig = DotModConfig.get();
            if (quickCraftButton != null) {
                quickCraftButton.setPosition(
                        currentRight + currentConfig.quickCraftButtonOffsetX,
                        accessor.dotmod$getY() + currentConfig.quickCraftButtonOffsetY
                );
            }
            if (hudButton != null) {
                hudButton.setPosition(
                        currentRight + currentConfig.hudEditorButtonOffsetX,
                        accessor.dotmod$getY() + currentConfig.hudEditorButtonOffsetY
                );
            }
        });
    }

    public interface ButtonAdder {
        void add(ButtonWidget button);
    }
}
