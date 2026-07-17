package com.dinomiha.dotmod.hud.widget;

public record HudPlacement(int x, int y, int width, int height, float scale, float alpha, boolean visible) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
