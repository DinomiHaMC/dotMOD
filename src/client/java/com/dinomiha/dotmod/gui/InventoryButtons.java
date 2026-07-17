package com.dinomiha.dotmod.gui;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.mixin.HandledScreenAccessor;
import com.dinomiha.dotmod.ui.component.DotButton;
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
        DotModConfig config = ConfigService.get().config();
        boolean quickCraftSupported = screen instanceof InventoryScreen || screen instanceof CraftingScreen;
        boolean hudSupported = quickCraftSupported || screen instanceof CreativeInventoryScreen;
        if (!hudSupported) {
            return;
        }
        int right = x + backgroundWidth;
        ButtonWidget quickCraftButton;
        if (quickCraftSupported) {
            Text label = localizedButtonText(config.quickCraft.buttonText, "Craft", "button.dotmod.quick_craft");
            int width = Math.max(44, MinecraftClient.getInstance().textRenderer.getWidth(label) + 16);
            quickCraftButton = DotButton.create(
                    right + config.quickCraft.buttonOffsetX,
                    y + config.quickCraft.buttonOffsetY,
                    width,
                    label,
                    Text.translatable("button.dotmod.quick_craft.tooltip"),
                    button -> QuickCraft.perform(screen.getScreenHandler())
            );
            quickCraftButton.visible = config.general.enabled && config.quickCraft.enabled;
            keepOnScreen(quickCraftButton, screen);
            adder.add(quickCraftButton);
        } else {
            quickCraftButton = null;
        }
        ButtonWidget hudButton;
        if (hudSupported) {
            Text label = localizedButtonText(config.hud.editorButtonText, "HUD", "button.dotmod.hud_editor");
            int width = Math.max(44, MinecraftClient.getInstance().textRenderer.getWidth(label) + 16);
            hudButton = DotButton.create(
                    right + config.hud.editorButtonOffsetX,
                    y + config.hud.editorButtonOffsetY,
                    width,
                    label,
                    Text.translatable("button.dotmod.hud_editor.tooltip"),
                    button -> MinecraftClient.getInstance().setScreen(new HudEditorScreen(screen))
            );
            hudButton.visible = config.general.enabled && config.hud.editorEnabled;
            keepOnScreen(hudButton, screen);
            adder.add(hudButton);
        } else {
            hudButton = null;
        }

        ScreenEvents.beforeRender(screen).register((ignored, context, mouseX, mouseY, tickDelta) -> {
            HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
            int currentRight = accessor.dotmod$getX() + accessor.dotmod$getBackgroundWidth();
            DotModConfig currentConfig = ConfigService.get().config();
            if (quickCraftButton != null) {
                Text label = localizedButtonText(currentConfig.quickCraft.buttonText, "Craft", "button.dotmod.quick_craft");
                quickCraftButton.setMessage(label);
                quickCraftButton.setWidth(Math.max(44, MinecraftClient.getInstance().textRenderer.getWidth(label) + 16));
                quickCraftButton.visible = currentConfig.general.enabled && currentConfig.quickCraft.enabled;
                quickCraftButton.setPosition(
                        currentRight + currentConfig.quickCraft.buttonOffsetX,
                        accessor.dotmod$getY() + currentConfig.quickCraft.buttonOffsetY
                );
                keepOnScreen(quickCraftButton, screen);
            }
            if (hudButton != null) {
                Text label = localizedButtonText(currentConfig.hud.editorButtonText, "HUD", "button.dotmod.hud_editor");
                hudButton.setMessage(label);
                hudButton.setWidth(Math.max(44, MinecraftClient.getInstance().textRenderer.getWidth(label) + 16));
                hudButton.visible = currentConfig.general.enabled && currentConfig.hud.editorEnabled;
                hudButton.setPosition(
                        currentRight + currentConfig.hud.editorButtonOffsetX,
                        accessor.dotmod$getY() + currentConfig.hud.editorButtonOffsetY
                );
                keepOnScreen(hudButton, screen);
            }
        });
    }

    private static Text localizedButtonText(String configured, String legacyDefault, String translationKey) {
        if (configured == null || configured.isBlank() || legacyDefault.equals(configured)) {
            return Text.translatable(translationKey);
        }
        return Text.literal(configured);
    }

    private static void keepOnScreen(ButtonWidget button, HandledScreen<?> screen) {
        int x = Math.max(2, Math.min(screen.width - button.getWidth() - 2, button.getX()));
        int y = Math.max(2, Math.min(screen.height - button.getHeight() - 2, button.getY()));
        button.setPosition(x, y);
    }

    public interface ButtonAdder {
        void add(ButtonWidget button);
    }
}
