package com.dinomiha.dotmod.feature.screenshot.screen;

import com.dinomiha.dotmod.DotModClient;
import com.dinomiha.dotmod.feature.screenshot.ClientIoExecutor;
import com.dinomiha.dotmod.feature.screenshot.ScreenshotPathPolicy;
import com.dinomiha.dotmod.ui.component.DotButton;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class ImageViewerScreen extends Screen {
    private final Screen parent;
    private final Path imageRoot;
    private final Path imagePath;
    private final Identifier textureId;
    private NativeImageBackedTexture texture;
    private int imageWidth;
    private int imageHeight;
    private Text status = Text.translatable("screen.dotmod.image.loading");

    public ImageViewerScreen(Screen parent, Path imageRoot, Path imagePath) {
        super(Text.translatable("screen.dotmod.image.title"));
        this.parent = parent;
        this.imageRoot = imageRoot;
        this.imagePath = imagePath;
        this.textureId = Identifier.of(DotModClient.MOD_ID, "death_image/" + Integer.toUnsignedString(imagePath.hashCode()));
    }

    @Override
    protected void init() {
        addDrawableChild(DotButton.create(width / 2 - 50, height - 28, 100, Text.translatable("gui.back"), button -> close()));
        if (texture == null) {
            CompletableFuture.supplyAsync(this::loadImage, ClientIoExecutor.INSTANCE)
                    .thenAccept(image -> client.execute(() -> register(image)))
                    .exceptionally(exception -> {
                        client.execute(() -> status = Text.translatable("screen.dotmod.image.failed"));
                        return null;
                    });
        }
    }

    private NativeImage loadImage() {
        try {
            Path validated = new ScreenshotPathPolicy(imageRoot).validateExisting(imagePath);
            try (InputStream input = Files.newInputStream(validated)) {
                return NativeImage.read(input);
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void register(NativeImage image) {
        if (client.currentScreen != this) {
            image.close();
            return;
        }
        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        texture = new NativeImageBackedTexture(() -> "dotMOD death image", image);
        client.getTextureManager().registerTexture(textureId, texture);
        texture.upload();
        status = Text.empty();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF101216);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        if (texture == null) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height / 2, 0xFFE0E0E0);
        } else {
            int availableWidth = Math.max(1, width - 24);
            int availableHeight = Math.max(1, height - 64);
            double scale = Math.min((double) availableWidth / imageWidth, (double) availableHeight / imageHeight);
            int drawWidth = Math.max(1, (int) (imageWidth * scale));
            int drawHeight = Math.max(1, (int) (imageHeight * scale));
            int x = (width - drawWidth) / 2;
            int y = 28 + (availableHeight - drawHeight) / 2;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, textureId, x, y, 0, 0,
                    drawWidth, drawHeight, imageWidth, imageHeight);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        if (texture != null) {
            client.getTextureManager().destroyTexture(textureId);
            texture = null;
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
