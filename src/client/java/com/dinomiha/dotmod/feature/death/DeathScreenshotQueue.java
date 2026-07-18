package com.dinomiha.dotmod.feature.death;

import com.dinomiha.dotmod.feature.screenshot.ClientIoExecutor;
import com.dinomiha.dotmod.feature.screenshot.ScreenshotPathPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
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
                Path publishedTarget = null;
                Object publishedFileKey = null;
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
                    Path temporary = Files.createTempFile(realImages, "." + capture.recordId + "-", ".tmp");
                    try {
                        image.writeTo(temporary);
                        publish(images, realImages, temporary, target);
                        policy.validateExisting(target);
                        publishedTarget = target;
                        publishedFileKey = Files.readAttributes(
                                target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey();
                    } finally {
                        Files.deleteIfExists(temporary);
                    }
                } catch (Exception exception) {
                    error = message(exception);
                    LOGGER.error("Could not save death screenshot {}", capture.recordId, exception);
                }
                try {
                    if (error == null) {
                        capture.repository.markScreenshotSaved(capture.recordId, relative);
                    } else {
                        capture.repository.markScreenshotFailed(capture.recordId, error);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.error("Could not update death screenshot state {}", capture.recordId, exception);
                    if (error == null) {
                        removePublishedFile(publishedTarget, publishedFileKey);
                    }
                } finally {
                    client.execute(() -> captureInFlight = false);
                }
            });
        } catch (RuntimeException exception) {
            image.close();
            fail(client, capture, message(exception));
        }
    }

    private static void removePublishedFile(Path target, Object expectedFileKey) {
        if (target == null) return;
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isSymbolicLink()
                    && (expectedFileKey == null || expectedFileKey.equals(attributes.fileKey()))) {
                Files.deleteIfExists(target);
            }
        } catch (java.nio.file.NoSuchFileException ignored) {
            // The record deletion or another cleanup already removed the image.
        } catch (Exception exception) {
            LOGGER.warn("Could not remove unreferenced death screenshot {}", target, exception);
        }
    }

    private static void fail(MinecraftClient client, PendingCapture capture, String error) {
        try {
            ClientIoExecutor.INSTANCE.execute(() -> {
                try {
                    capture.repository.markScreenshotFailed(capture.recordId, error);
                } catch (RuntimeException exception) {
                    LOGGER.error("Could not mark death screenshot failed {}", capture.recordId, exception);
                } finally {
                    client.execute(() -> captureInFlight = false);
                }
            });
        } catch (RuntimeException exception) {
            LOGGER.error("Could not queue death screenshot failure {}", capture.recordId, exception);
            captureInFlight = false;
        }
    }

    private static void publish(Path images, Path realImages, Path temporary, Path target) throws Exception {
        if (Files.isSymbolicLink(images) || !Files.isDirectory(images, LinkOption.NOFOLLOW_LINKS)
                || !images.toRealPath().equals(realImages)) {
            throw new IllegalArgumentException("Death image directory changed during screenshot write");
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(realImages)) {
            if (stream instanceof SecureDirectoryStream<Path> secure) {
                Path targetName = target.getFileName();
                BasicFileAttributeView attributes = secure.getFileAttributeView(
                        targetName, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
                try {
                    if (attributes.readAttributes().isSymbolicLink()) {
                        throw new IllegalArgumentException("Death screenshot target must not be a symbolic link");
                    }
                } catch (java.nio.file.NoSuchFileException ignored) {
                    // No previous screenshot to validate before the atomic move.
                }
                secure.move(temporary.getFileName(), secure, targetName);
                return;
            }
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new java.nio.file.FileAlreadyExistsException(target.toString());
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            // No REPLACE_EXISTING: a raced file or symlink makes publication fail safely.
            Files.move(temporary, target);
        }
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record PendingCapture(UUID recordId, DeathRepository repository, Object networkIdentity, Object worldIdentity) {
    }
}
