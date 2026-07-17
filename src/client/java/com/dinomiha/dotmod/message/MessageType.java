package com.dinomiha.dotmod.message;

import net.minecraft.util.Formatting;

public enum MessageType {
    INFO(Formatting.GRAY),
    WARNING(Formatting.YELLOW),
    ERROR(Formatting.RED),
    SUCCESS(Formatting.GREEN);

    private final Formatting color;

    MessageType(Formatting color) {
        this.color = color;
    }

    public Formatting color() {
        return color;
    }
}
