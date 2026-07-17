package com.dinomiha.dotmod.hud.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public interface HudWidget {
    String id();

    boolean hasContent(MinecraftClient client, boolean preview);

    void render(DrawContext context, MinecraftClient client, float alpha, boolean preview);
}
