package com.dinomiha.dotmod.ui.component;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class DotButton {
    public static final int HEIGHT = 20;

    private DotButton() {
    }

    public static ButtonWidget create(int x, int y, int width, Text label, ButtonWidget.PressAction action) {
        return ButtonWidget.builder(label, action)
                .dimensions(x, y, width, HEIGHT)
                .build();
    }

    public static ButtonWidget create(
            int x,
            int y,
            int width,
            Text label,
            Text tooltip,
            ButtonWidget.PressAction action
    ) {
        ButtonWidget button = ButtonWidget.builder(label, action)
                .dimensions(x, y, width, HEIGHT)
                .build();
        DotTooltip.attach(button, tooltip);
        return button;
    }
}
