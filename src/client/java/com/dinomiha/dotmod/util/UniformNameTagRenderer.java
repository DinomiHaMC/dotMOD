package com.dinomiha.dotmod.util;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.DotModConfig;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class UniformNameTagRenderer {
    private static final double REFERENCE_DISTANCE = 4.0;
    private static final ThreadLocal<Float> SCALE = new ThreadLocal<>();

    private UniformNameTagRenderer() {
    }

    public static void submit(
            OrderedRenderCommandQueue queue,
            MatrixStack matrices,
            Vec3d position,
            int yOffset,
            Text text,
            boolean discrete,
            int light,
            double squaredDistanceToCamera,
            CameraRenderState cameraState
    ) {
        DotModConfig config = ConfigService.get().config();
        if (!config.general.enabled || !config.hud.uniformNameTags.enabled || !config.hud.uniformNameTags.active) {
            queue.submitLabel(matrices, position, yOffset, text, discrete, light, squaredDistanceToCamera, cameraState);
            return;
        }

        float distanceScale = (float) (Math.sqrt(Math.max(0.01, squaredDistanceToCamera)) / REFERENCE_DISTANCE);
        SCALE.set(distanceScale * config.hud.uniformNameTags.size);
        try {
            queue.submitLabel(matrices, position, yOffset, text, discrete, light, squaredDistanceToCamera, cameraState);
        } finally {
            SCALE.remove();
        }
    }

    public static float scale() {
        Float scale = SCALE.get();
        return scale == null ? 1.0F : scale;
    }

    public static int backgroundColor(int vanillaColor) {
        if (SCALE.get() == null || vanillaColor == 0) {
            return vanillaColor;
        }
        return 0xFF000000 | ColorUtil.parseRgb(ConfigService.get().config().hud.uniformNameTags.backgroundColor, 0);
    }
}
