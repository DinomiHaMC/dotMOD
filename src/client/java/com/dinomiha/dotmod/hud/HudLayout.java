package com.dinomiha.dotmod.hud;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.DotModConfig;

public final class HudLayout {
    private HudLayout() {
    }

    public static Rect rect(HudElement element, int width, int height) {
        Rect base = baseRect(element, width, height);
        DotModConfig.HudOffset offset = ConfigService.get().config().hud.offset(element);
        return new Rect(base.x + offset.dx, base.y + offset.dy, base.width, base.height);
    }

    public static DotModConfig.HudOffset activeOffset(HudElement element) {
        DotModConfig config = ConfigService.get().config();
        if (!config.general.enabled || !config.hud.editorEnabled) {
            return new DotModConfig.HudOffset();
        }
        return config.hud.offset(element);
    }

    private static Rect baseRect(HudElement element, int width, int height) {
        int center = width / 2;
        return switch (element) {
            case HEARTS -> new Rect(center - 91, height - 39, element.width(), element.height());
            case ARMOR -> new Rect(center - 91, height - 49, element.width(), element.height());
            case HUNGER -> new Rect(center + 10, height - 39, element.width(), element.height());
            case HOTBAR -> new Rect(center - 91, height - 22, element.width(), element.height());
            case EXP_BAR -> new Rect(center - 91, height - 31, element.width(), element.height());
            case EXP_LEVEL -> new Rect(center - 20, height - 33, element.width(), element.height());
            case EFFECTS -> new Rect(width - 126, 2, element.width(), element.height());
            case BOSSBAR -> new Rect(center - 91, 12, element.width(), element.height());
            case SCOREBOARD -> new Rect(width - 124, height / 2 - 60, element.width(), element.height());
        };
    }

    public record Rect(int x, int y, int width, int height) {
    }
}
