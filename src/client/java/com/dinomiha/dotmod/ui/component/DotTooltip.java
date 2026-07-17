package com.dinomiha.dotmod.ui.component;

import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.time.Duration;

public final class DotTooltip {
    private DotTooltip() {
    }

    public static void attach(ClickableWidget widget, Text content) {
        widget.setTooltip(Tooltip.of(content));
        widget.setTooltipDelay(Duration.ofMillis(350));
    }
}
