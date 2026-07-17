package com.dinomiha.dotmod.feature.death;

import com.dinomiha.dotmod.feature.screenshot.ClientIoExecutor;
import com.dinomiha.dotmod.feature.screenshot.ScreenshotPathPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

public final class DeathScreenshotQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger("dotmod/death-screenshot");
    private static final Queue<PendingCapture> PENDING = new ArrayDeque<>();
    private static boolean captureInFlight;

    private DeathScreenshotQueue() {
    }

    static void enqueue(UUID recordId, DeathRepository repository, Object networkIdentity, Object worldIdentity) {
        PENDING.add(new PendingCapture(recordId, repository, networkIdentity, worldIdentity));
    }

    public static void processFrame(MinecraftClient client) {
        if (captureInFlight) {
            return;
        }
        PendingCapture capture = PENDING.poll();
        if (capture == null) {
            return;
        }
        captureInFlight = true;
        if (client.getNetworkHandler() != capture.networkIdentity || client.world != capture.worldIdentity) {
            fail(client, capture, "Connection changed before screenshot capture");
            return;
        }
        try {
            ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), image -> {
                if (client.getNetworkHandler() != capture.networkIdentity || client.world != capture.worldIdentity) {
                    image.close();
                    fail(client, capture, "Connection changed during screenshot capture");
                } else {
                    write(client, capture, image);
                }
            });
        } catch (RuntimeException exception) {
            fail(client, capture, message(exception));
        }
    }

    private static void write(MinecraftClient client, PendingCapture capture, NativeImage image) {
        try {
            ClientIoExecutor.INSTANCE.execute(() -> {
                String error = null;
                Path relative = Path.of("images", capture.recordId + ".png");
                try (image) {
                    Path deaths = DeathClientService.get().root();
                    Files.createDirectories(deaths);
                    Path realDeaths = deaths.toRealPath();
                    Path images = deaths.resolve("images");
                    if (Files.isSymbolicLink(images)) {
                        throw new IllegalArgumentException("Death image directory must not be a symbolic link");
                    }
                    Files.createDirectories(images);
                    Path realImages = images.toRealPath();
                    if (!realImages.startsWith(realDeaths)) {
                        throw new IllegalArgumentException("Death image directory is outside death storage");
                    }
                    ScreenshotPathPolicy policy = new ScreenshotPathPolicy(realImages);
                    Path target = policy.resolve(capture.recordId + ".png");
                    image.writeTo(target);
                    policy.validateExisting(target);
                } catch (Exception exception) {
                    error = message(exception);
                    LOGGER.error("Could not save death screenshot {}", capture.recordId, exception);
                }
                String failure = error;
                client.execute(() -> {
                    try {
                        if (failure == null) {
                            capture.repository.markScreenshotSaved(capture.recordId, relative);
                        } else {
                            capture.repository.markScreenshotFailed(capture.recordId, failure);
                        }
                    } catch (RuntimeException exception) {
                        LOGGER.error("Could not update death screenshot state {}", capture.recordId, exception);
                    } finally {
                        captureInFlight = false;
                    }
                });
            });
        } catch (RuntimeException exception) {
            image.close();
            fail(client, capture, message(exception));
        }
    }

    private static void fail(MinecraftClient client, PendingCapture capture, String error) {
        client.execute(() -> {
            try {
                capture.repository.markScreenshotFailed(capture.recordId, error);
            } catch (RuntimeException exception) {
                LOGGER.error("Could not mark death screenshot failed {}", capture.recordId, exception);
            } finally {
                captureInFlight = false;
            }
        });
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record PendingCapture(UUID recordId, DeathRepository repository, Object networkIdentity, Object worldIdentity) {
    }
}
