package com.dinomiha.dotmod.hud.editor;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.hud.widget.HudAnchor;
import com.dinomiha.dotmod.hud.widget.HudPlacement;
import com.dinomiha.dotmod.hud.widget.HudPlacementResolver;
import com.dinomiha.dotmod.hud.widget.HudSnapper;
import com.dinomiha.dotmod.hud.widget.HudWidgetDefinition;
import com.dinomiha.dotmod.hud.widget.HudWidgetDefaults;
import com.dinomiha.dotmod.hud.widget.HudWidgetSettings;

import java.util.List;

public final class HudEditorController {
    private String draggingId;
    private int dragMouseX;
    private int dragMouseY;
    private int dragX;
    private int dragY;
    private Integer verticalGuide;
    private Integer horizontalGuide;

    public HudPlacement placement(HudWidgetDefinition definition, int screenWidth, int screenHeight) {
        HudWidgetSettings settings = settings(definition.id());
        return HudPlacementResolver.resolve(
                settings, definition.width(), definition.height(), screenWidth, screenHeight
        );
    }

    public void startDrag(HudWidgetDefinition definition, int mouseX, int mouseY, int width, int height) {
        HudPlacement placement = placement(definition, width, height);
        draggingId = definition.id();
        dragMouseX = mouseX;
        dragMouseY = mouseY;
        dragX = placement.x();
        dragY = placement.y();
        verticalGuide = null;
        horizontalGuide = null;
    }

    public boolean drag(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (draggingId == null) {
            return false;
        }
        HudWidgetDefinition definition = HudWidgetDefaults.require(draggingId);
        HudWidgetSettings settings = settings(draggingId);
        HudPlacement placement = placement(definition, screenWidth, screenHeight);
        int x = dragX + mouseX - dragMouseX;
        int y = dragY + mouseY - dragMouseY;
        var hud = ConfigService.get().config().hud;
        if (hud.snapToGrid) {
            int grid = Math.max(1, hud.gridSize);
            x = Math.round(x / (float) grid) * grid;
            y = Math.round(y / (float) grid) * grid;
        }
        if (hud.magneticSnapping) {
            List<HudPlacement> others = HudWidgetDefaults.definitions().stream()
                    .filter(other -> !other.id().equals(draggingId))
                    .filter(other -> settings(other.id()).visible)
                    .filter(other -> !other.id().equals(HudWidgetDefaults.DURABILITY)
                            || ConfigService.get().config().durability.enabled)
                    .map(other -> placement(other, screenWidth, screenHeight))
                    .toList();
            HudSnapper.SnapResult snapped = HudSnapper.snap(
                    x, y, placement.width(), placement.height(),
                    screenWidth, screenHeight, hud.magneticSnapDistance, others
            );
            x = snapped.x();
            y = snapped.y();
            verticalGuide = snapped.verticalGuide();
            horizontalGuide = snapped.horizontalGuide();
        } else {
            x = Math.max(0, Math.min(Math.max(0, screenWidth - placement.width()), x));
            y = Math.max(0, Math.min(Math.max(0, screenHeight - placement.height()), y));
            verticalGuide = null;
            horizontalGuide = null;
        }
        settings.offsetX = x - settings.anchor.x(screenWidth);
        settings.offsetY = y - settings.anchor.y(screenHeight);
        return true;
    }

    public void stopDrag() {
        draggingId = null;
        verticalGuide = null;
        horizontalGuide = null;
    }

    public void toggleVisible(String id) {
        HudWidgetSettings settings = settings(id);
        settings.visible = !settings.visible;
    }

    public void cycleScale(String id) {
        HudWidgetSettings settings = settings(id);
        settings.scale = next(settings.scale, new float[]{0.5F, 0.75F, 1.0F, 1.25F, 1.5F, 2.0F});
    }

    public void cycleAlpha(String id) {
        HudWidgetSettings settings = settings(id);
        settings.alpha = next(settings.alpha, new float[]{0.25F, 0.5F, 0.75F, 1.0F});
    }

    public void cycleAnchor(String id, int screenWidth, int screenHeight) {
        HudWidgetDefinition definition = HudWidgetDefaults.require(id);
        HudWidgetSettings settings = settings(id);
        HudPlacement placement = placement(definition, screenWidth, screenHeight);
        HudPlacementResolver.preservePositionForAnchor(
                settings, settings.anchor.next(), placement.x(), placement.y(), screenWidth, screenHeight
        );
    }

    public void reset(String id) {
        ConfigService.get().config().hud.widgets.put(id, HudWidgetDefaults.require(id).defaults());
    }

    public HudWidgetSettings settings(String id) {
        return ConfigService.get().config().hud.widget(id);
    }

    public String draggingId() {
        return draggingId;
    }

    public Integer verticalGuide() {
        return verticalGuide;
    }

    public Integer horizontalGuide() {
        return horizontalGuide;
    }

    private static float next(float current, float[] values) {
        for (float value : values) {
            if (value > current + 0.001F) {
                return value;
            }
        }
        return values[0];
    }
}
