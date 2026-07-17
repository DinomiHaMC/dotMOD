package com.dinomiha.dotmod.feature.screenshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotResultTest {
    @Test
    void representsSuccessAndFailure() {
        Path path = Path.of("shot.png");
        ScreenshotResult success = ScreenshotResult.success(path);
        ScreenshotResult failure = ScreenshotResult.failure("capture failed");

        assertTrue(success.succeeded());
        assertEquals(path, success.path());
        assertFalse(failure.succeeded());
        assertEquals("capture failed", failure.message());
    }

    @Test
    void enforcesResultInvariants() {
        assertThrows(NullPointerException.class,
                () -> new ScreenshotResult(ScreenshotResult.Status.SUCCESS, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ScreenshotResult(ScreenshotResult.Status.FAILURE, null, " "));
    }
}
