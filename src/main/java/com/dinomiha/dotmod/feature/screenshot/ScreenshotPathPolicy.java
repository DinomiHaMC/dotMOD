package com.dinomiha.dotmod.feature.screenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

public final class ScreenshotPathPolicy {
    private final Path allowedRoot;

    public ScreenshotPathPolicy(Path allowedRoot) {
        this.allowedRoot = Objects.requireNonNull(allowedRoot, "allowedRoot").toAbsolutePath().normalize();
    }

    public Path allowedRoot() {
        return allowedRoot;
    }

    public Path resolve(String fileName) {
        validateFileName(fileName);
        Path resolved = allowedRoot.resolve(fileName).normalize();
        if (!resolved.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Screenshot path is outside the allowed root");
        }
        return resolved;
    }

    public Path validateExisting(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Screenshot path is outside the allowed root");
        }
        Path fileName = normalized.getFileName();
        validateFileName(fileName == null ? null : fileName.toString());
        if (Files.isSymbolicLink(normalized)
                || !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Screenshot path must be an existing regular non-symlink file");
        }

        Path realRoot = allowedRoot.toRealPath();
        Path realPath = normalized.toRealPath();
        if (!realPath.startsWith(realRoot)) {
            throw new IllegalArgumentException("Screenshot real path is outside the allowed root");
        }
        return realPath;
    }

    private static void validateFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Screenshot file name is required");
        }
        if (fileName.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Screenshot file name contains NUL");
        }
        if (fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Screenshot file name must not contain separators");
        }
        Path candidate = Path.of(fileName);
        if (candidate.isAbsolute() || fileName.equals(".") || fileName.equals("..")) {
            throw new IllegalArgumentException("Screenshot file name must be a simple relative name");
        }
        if (!fileName.endsWith(".png") || fileName.length() == ".png".length()) {
            throw new IllegalArgumentException("Screenshot file name must end in .png");
        }
    }
}
