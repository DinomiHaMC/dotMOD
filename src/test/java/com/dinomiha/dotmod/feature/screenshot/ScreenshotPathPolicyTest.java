package com.dinomiha.dotmod.feature.screenshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScreenshotPathPolicyTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesSafePngNamesUnderTheAllowedRoot() {
        ScreenshotPathPolicy policy = new ScreenshotPathPolicy(temporaryDirectory.resolve("shots"));

        assertEquals(policy.allowedRoot().resolve("A quote ' and $; 世界.png"),
                policy.resolve("A quote ' and $; 世界.png"));
    }

    @Test
    void rejectsAbsoluteTraversalSeparatorsNulAndNonPngNames() {
        ScreenshotPathPolicy policy = new ScreenshotPathPolicy(temporaryDirectory);

        assertThrows(IllegalArgumentException.class, () -> policy.resolve(temporaryDirectory.resolve("shot.png").toString()));
        assertThrows(IllegalArgumentException.class, () -> policy.resolve("../shot.png"));
        assertThrows(IllegalArgumentException.class, () -> policy.resolve("folder/shot.png"));
        assertThrows(IllegalArgumentException.class, () -> policy.resolve("folder\\shot.png"));
        assertThrows(IllegalArgumentException.class, () -> policy.resolve("shot\0.png"));
        assertThrows(IllegalArgumentException.class, () -> policy.resolve("shot.jpg"));
        assertThrows(IllegalArgumentException.class, () -> policy.resolve(".png"));
    }

    @Test
    void validatesExistingRegularFilesUsingRealPaths() throws IOException {
        Path root = Files.createDirectory(temporaryDirectory.resolve("shots"));
        Path screenshot = Files.writeString(root.resolve("valid.png"), "png");
        Path nonPng = Files.writeString(root.resolve("not-an-image.txt"), "text");
        ScreenshotPathPolicy policy = new ScreenshotPathPolicy(root);

        assertEquals(screenshot.toRealPath(), policy.validateExisting(screenshot));
        assertThrows(IllegalArgumentException.class, () -> policy.validateExisting(root));
        assertThrows(IllegalArgumentException.class, () -> policy.validateExisting(nonPng));
        assertThrows(IllegalArgumentException.class,
                () -> policy.validateExisting(temporaryDirectory.resolve("missing.png")));
    }

    @Test
    void rejectsSymlinkFilesAndParentSymlinksThatEscapeTheRoot() throws IOException {
        Path root = Files.createDirectory(temporaryDirectory.resolve("shots"));
        Path outside = Files.writeString(temporaryDirectory.resolve("outside.png"), "png");
        ScreenshotPathPolicy policy = new ScreenshotPathPolicy(root);

        Path fileLink = Files.createSymbolicLink(root.resolve("linked.png"), outside);
        assertThrows(IllegalArgumentException.class, () -> policy.validateExisting(fileLink));

        Path outsideDirectory = Files.createDirectory(temporaryDirectory.resolve("outside"));
        Path escaped = Files.writeString(outsideDirectory.resolve("escaped.png"), "png");
        Path directoryLink = Files.createSymbolicLink(root.resolve("linked-directory"), outsideDirectory);
        assertThrows(IllegalArgumentException.class,
                () -> policy.validateExisting(directoryLink.resolve(escaped.getFileName())));
    }
}
