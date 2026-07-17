package com.dinomiha.dotmod.feature.screenshot;

import java.nio.file.Path;
import java.util.Objects;

public record ScreenshotResult(Status status, Path path, String message) {
    public ScreenshotResult {
        Objects.requireNonNull(status, "status");
        if (status == Status.SUCCESS) {
            Objects.requireNonNull(path, "path");
        } else if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("A failed screenshot result requires a message");
        }
    }

    public static ScreenshotResult success(Path path) {
        return new ScreenshotResult(Status.SUCCESS, path, null);
    }

    public static ScreenshotResult failure(String message) {
        return new ScreenshotResult(Status.FAILURE, null, message);
    }

    public boolean succeeded() {
        return status == Status.SUCCESS;
    }

    public enum Status {
        SUCCESS,
        FAILURE
    }
}
