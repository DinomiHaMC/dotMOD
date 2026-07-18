package com.dinomiha.dotmod.config;

public final class FreelookConfig {
    public boolean enabled = true;
    public FreelookActivation activation = FreelookActivation.HOLD;
    public FreelookPerspective perspective = FreelookPerspective.PRESERVE;
    public float sensitivity = 1.0F;
    public boolean invertX;
    public boolean invertY;
    public int returnDurationMs = 200;
    public boolean showIndicator = true;
}
