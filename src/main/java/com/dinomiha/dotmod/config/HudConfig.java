package com.dinomiha.dotmod.config;

import com.dinomiha.dotmod.hud.HudElement;

import java.util.EnumMap;
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
    public Map<HudElement, DotModConfig.HudOffset> offsets = new EnumMap<>(HudElement.class);
    public UniformNameTagsConfig uniformNameTags = new UniformNameTagsConfig();

    public DotModConfig.HudOffset offset(HudElement element) {
        return offsets.computeIfAbsent(element, ignored -> new DotModConfig.HudOffset());
    }

    public void resetOffsets() {
        offsets.clear();
        for (HudElement element : HudElement.values()) {
            offsets.put(element, new DotModConfig.HudOffset());
        }
    }
}
