package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.hud.widget.HudWidgetDefaults;
import com.dinomiha.dotmod.hud.widget.HudWidgetSettings;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudConfig {
    public boolean editorEnabled = true;
    public int editorButtonOffsetX = 4;
    public int editorButtonOffsetY = 28;
    public String editorButtonText = "HUD";
    public boolean snapToGrid = true;
    public int gridSize = 2;
    public boolean magneticSnapping = true;
    public int magneticSnapDistance = 4;
    public Map<String, HudWidgetSettings> widgets = new LinkedHashMap<>(HudWidgetDefaults.settings());
    /** Legacy schema v3 data; cleared after migration. */
    public Map<HudElement, DotModConfig.HudOffset> offsets;
    public UniformNameTagsConfig uniformNameTags = new UniformNameTagsConfig();

    public DotModConfig.HudOffset offset(HudElement element) {
        if (offsets == null) {
            offsets = new EnumMap<>(HudElement.class);
        }
        return offsets.computeIfAbsent(element, ignored -> new DotModConfig.HudOffset());
    }

    public HudWidgetSettings widget(String id) {
        return widgets.computeIfAbsent(id, ignored -> HudWidgetDefaults.require(id).defaults());
    }

    public void resetOffsets() {
        if (widgets == null) {
            widgets = new LinkedHashMap<>();
        }
        HudWidgetDefaults.settings().forEach(widgets::put);
        offsets = null;
    }
}
