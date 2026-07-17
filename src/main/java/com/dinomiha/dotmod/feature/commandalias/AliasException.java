package com.dinomiha.dotmod.feature.commandalias;

public final class AliasException extends RuntimeException {
    private final AliasError error;

    public AliasException(AliasError error, String message) {
        super(message);
        this.error = error;
    }

    public AliasError error() {
        return error;
    }
}
