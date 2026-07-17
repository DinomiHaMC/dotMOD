package com.dinomiha.dotmod.feature.death.model;

import java.nio.file.Path;
import java.util.Objects;

public record DeathScreenshot(ScreenshotStatus status, String relativePath, String error) {
    public static final int MAX_ERROR_LENGTH = 1024;

    public DeathScreenshot {
        status = Objects.requireNonNull(status, "status");
        switch (status) {
            case PENDING -> {
                if (relativePath != null || error != null) {
                    throw new IllegalArgumentException("Pending screenshot cannot have a path or error");
                }
            }
            case SAVED -> {
                relativePath = normalizeRelativePath(relativePath);
                if (error != null) {
                    throw new IllegalArgumentException("Saved screenshot cannot have an error");
                }
            }
            case FAILED -> {
                if (relativePath != null) {
                    throw new IllegalArgumentException("Failed screenshot cannot have a path");
                }
                error = requireError(error);
            }
        }
    }

    public static DeathScreenshot pending() {
        return new DeathScreenshot(ScreenshotStatus.PENDING, null, null);
    }

    public static DeathScreenshot saved(Path relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");
        return new DeathScreenshot(ScreenshotStatus.SAVED, relativePath.toString(), null);
    }

    public static DeathScreenshot failed(String error) {
        return new DeathScreenshot(ScreenshotStatus.FAILED, null, error);
    }

    private static String normalizeRelativePath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Saved screenshot path is missing");
        }
        Path path = Path.of(value);
        if (path.isAbsolute() || path.normalize().startsWith("..")) {
            throw new IllegalArgumentException("Screenshot path must stay within the repository root");
        }
        String normalized = path.normalize().toString();
        if (normalized.isEmpty() || normalized.equals(".")) {
            throw new IllegalArgumentException("Saved screenshot path is missing");
        }
        return normalized.replace('\\', '/');
    }

    private static String requireError(String value) {
        String error = Objects.requireNonNull(value, "error").strip();
        if (error.isEmpty()) {
            throw new IllegalArgumentException("Screenshot failure error is missing");
        }
        if (error.length() > MAX_ERROR_LENGTH) {
            error = error.substring(0, MAX_ERROR_LENGTH);
        }
        return error;
    }
}
