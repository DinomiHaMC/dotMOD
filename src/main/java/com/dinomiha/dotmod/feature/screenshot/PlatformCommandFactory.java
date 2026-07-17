package com.dinomiha.dotmod.feature.screenshot;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class PlatformCommandFactory {
    private PlatformCommandFactory() {
    }

    public static List<String> open(PlatformKind platform, Path path) {
        Objects.requireNonNull(platform, "platform");
        String argument = Objects.requireNonNull(path, "path").toString();
        return switch (platform) {
            case LINUX -> List.of("xdg-open", argument);
            case WINDOWS -> List.of("rundll32", "url.dll,FileProtocolHandler", argument);
            case MACOS -> List.of("open", argument);
        };
    }

    public static List<String> reveal(PlatformKind platform, Path path) {
        Objects.requireNonNull(platform, "platform");
        Path target = Objects.requireNonNull(path, "path");
        return switch (platform) {
            case LINUX -> List.of("xdg-open", parentOf(target).toString());
            case WINDOWS -> List.of("explorer", "/select,", target.toString());
            case MACOS -> List.of("open", "-R", target.toString());
        };
    }

    private static Path parentOf(Path path) {
        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Screenshot path has no parent directory");
        }
        return parent;
    }
}
