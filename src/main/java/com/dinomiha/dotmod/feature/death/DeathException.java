package com.dinomiha.dotmod.feature.death;

public final class DeathException extends RuntimeException {
    private final DeathError error;

    public DeathException(DeathError error, String message) {
        super(message);
        this.error = error;
    }

    public DeathException(DeathError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public DeathError error() {
        return error;
    }
}
