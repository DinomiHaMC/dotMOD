package com.dinomiha.dotmod.ui.component;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public final class DotConfirmationDialog {
    private DotConfirmationDialog() {
    }

    public static void open(
            MinecraftClient client,
            Screen parent,
            Text title,
            Text message,
            Runnable onConfirm
    ) {
        client.setScreen(new ConfirmScreen(
                confirmed -> {
                    client.setScreen(parent);
                    if (confirmed) {
                        onConfirm.run();
                    }
                },
                title,
                message,
                ScreenTexts.YES,
                ScreenTexts.CANCEL
        ));
    }
}
