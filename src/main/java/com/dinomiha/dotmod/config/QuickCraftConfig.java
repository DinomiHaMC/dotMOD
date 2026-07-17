package com.dinomiha.dotmod.config;

import java.util.ArrayList;
import java.util.List;

public final class QuickCraftConfig {
    public boolean enabled = true;
    public List<Integer> slots2x2 = new ArrayList<>(List.of(9, 10, 18, 19));
    public List<Integer> slots3x3 = new ArrayList<>(List.of(9, 10, 11, 18, 19, 20, 27, 28, 29));
    public int buttonOffsetX = 4;
    public int buttonOffsetY = 4;
    public String buttonText = "Craft";
}
