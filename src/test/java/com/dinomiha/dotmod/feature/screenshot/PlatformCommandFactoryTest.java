package com.dinomiha.dotmod.feature.screenshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformCommandFactoryTest {
    private static final Path PATH = Path.of("shots", "space 世界 'quote' $HOME;$(touch nope)&.png");

    @Test
    void createsLinuxArgumentArrays() {
        assertEquals(List.of("xdg-open", PATH.toString()),
                PlatformCommandFactory.open(PlatformKind.LINUX, PATH));
        assertEquals(List.of("xdg-open", PATH.toAbsolutePath().normalize().getParent().toString()),
                PlatformCommandFactory.reveal(PlatformKind.LINUX, PATH));
    }

    @Test
    void createsWindowsArgumentArrays() {
        assertEquals(List.of("rundll32", "url.dll,FileProtocolHandler", PATH.toString()),
                PlatformCommandFactory.open(PlatformKind.WINDOWS, PATH));
        assertEquals(List.of("explorer", "/select,", PATH.toString()),
                PlatformCommandFactory.reveal(PlatformKind.WINDOWS, PATH));
    }

    @Test
    void createsMacArgumentArrays() {
        assertEquals(List.of("open", PATH.toString()),
                PlatformCommandFactory.open(PlatformKind.MACOS, PATH));
        assertEquals(List.of("open", "-R", PATH.toString()),
                PlatformCommandFactory.reveal(PlatformKind.MACOS, PATH));
    }
}
