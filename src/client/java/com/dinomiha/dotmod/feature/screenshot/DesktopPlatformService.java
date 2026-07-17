package com.dinomiha.dotmod.feature.screenshot;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class DesktopPlatformService {
    private static final Logger LOGGER = LoggerFactory.getLogger("dotmod/platform");

    private DesktopPlatformService() {
    }

    public static void copyPath(MinecraftClient client, Path path) {
        client.keyboard.setClipboard(path.toAbsolutePath().normalize().toString());
    }

    public static void open(Path path) {
        launch(PlatformCommandFactory.open(platform(), path));
    }

    public static void reveal(Path path) {
        launch(PlatformCommandFactory.reveal(platform(), path));
    }

    private static void launch(List<String> command) {
        ClientIoExecutor.INSTANCE.execute(() -> {
            try {
                new ProcessBuilder(command).start();
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Could not launch desktop command {}", command.getFirst(), exception);
            }
        });
    }

    private static PlatformKind platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return PlatformKind.WINDOWS;
        }
        if (os.contains("mac")) {
            return PlatformKind.MACOS;
        }
        return PlatformKind.LINUX;
    }
}
