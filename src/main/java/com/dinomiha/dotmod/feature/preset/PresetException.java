package com.dinomiha.dotmod.feature.preset;

public final class PresetException extends RuntimeException {
    private final PresetError error;

    public PresetException(PresetError error, String message) {
        super(message);
        this.error = error;
    }

    public PresetException(PresetError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public PresetError error() {
        return error;
    }
}
