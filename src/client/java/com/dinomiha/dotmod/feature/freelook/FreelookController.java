package com.dinomiha.dotmod.feature.freelook;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.FreelookActivation;
import com.dinomiha.dotmod.config.FreelookPerspective;
import com.dinomiha.dotmod.keybind.DotModKeybindCategory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;

public final class FreelookController {
    public enum RuntimeState { ACTIVE, RETURNING, IDLE }

    private static final FreelookController INSTANCE = new FreelookController();

    private final FreelookCameraState camera = new FreelookCameraState();
    private KeyBinding activationKey;
    private RuntimeState state = RuntimeState.IDLE;
    private CameraReturnAnimation returnAnimation;
    private Object ownerPlayer;
    private Object ownerWorld;
    private Object ownerHandler;
    private Object ownerCamera;
    private Perspective previousPerspective;
    private boolean ownsPerspective;

    private FreelookController() {
    }

    public static void initialize() {
        INSTANCE.activationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dotmod.freelook", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(),
                DotModKeybindCategory.INSTANCE));
        ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::tick);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> INSTANCE.hardReset(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> INSTANCE.hardReset(client));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> INSTANCE.hardReset(client));
        ClientLifecycleEvents.CLIENT_STOPPING.register(INSTANCE::hardReset);
    }

    public static RuntimeState state() {
        return INSTANCE.state;
    }

    public static boolean interceptLook(double yawDelta, double pitchDelta) {
        if (INSTANCE.state != RuntimeState.ACTIVE) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getCameraEntity() != client.player) return false;
        var config = ConfigService.get().config().freelook;
        INSTANCE.camera.applyLookDelta(
                yawDelta,
                pitchDelta,
                client.player.getPitch(),
                config.sensitivity,
                config.invertX,
                config.invertY
        );
        return true;
    }

    public static float cameraYaw(float original) {
        INSTANCE.animate();
        return INSTANCE.state == RuntimeState.IDLE ? original : INSTANCE.camera.effectiveYaw(original);
    }

    public static float cameraPitch(float original) {
        INSTANCE.animate();
        return INSTANCE.state == RuntimeState.IDLE ? original : INSTANCE.camera.effectivePitch(original);
    }

    private void tick(MinecraftClient client) {
        var config = ConfigService.get().config();
        boolean valid = config.general.enabled && config.freelook.enabled
                && client.player != null && client.world != null && client.getNetworkHandler() != null
                && !client.player.isDead() && client.currentScreen == null && client.isWindowFocused()
                && client.mouse.isCursorLocked() && client.getCameraEntity() == client.player;
        boolean ownerChanged = ownerPlayer != null && (ownerPlayer != client.player || ownerWorld != client.world
                || ownerHandler != client.getNetworkHandler() || ownerCamera != client.getCameraEntity());
        if (!valid || ownerChanged) {
            hardReset(client);
            rememberOwners(client);
            return;
        }
        rememberOwners(client);
        relinquishPerspectiveIfChanged(client);

        if (config.freelook.activation == FreelookActivation.HOLD) {
            if (activationKey.isPressed() && state != RuntimeState.ACTIVE) activate(client);
            if (!activationKey.isPressed() && state == RuntimeState.ACTIVE) release(client);
            while (activationKey.wasPressed()) {
                // Hold mode follows physical state; discard queued press counts.
            }
        } else {
            while (activationKey.wasPressed()) {
                if (state == RuntimeState.ACTIVE) release(client); else activate(client);
            }
        }
        animate();
    }

    private void activate(MinecraftClient client) {
        if (state != RuntimeState.RETURNING) {
            camera.reset();
        } else {
            animate();
        }
        returnAnimation = null;
        state = RuntimeState.ACTIVE;
        var config = ConfigService.get().config().freelook;
        if (config.perspective == FreelookPerspective.SWITCH_TO_THIRD_PERSON_BACK && !ownsPerspective) {
            previousPerspective = client.options.getPerspective();
            if (previousPerspective != Perspective.THIRD_PERSON_BACK) {
                client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                ownsPerspective = true;
            }
        }
    }

    private void release(MinecraftClient client) {
        int duration = ConfigService.get().config().freelook.returnDurationMs;
        returnAnimation = new CameraReturnAnimation(camera.yawOffset(), camera.pitchOffset(), System.nanoTime(), duration);
        state = duration == 0 ? RuntimeState.IDLE : RuntimeState.RETURNING;
        if (state == RuntimeState.IDLE) {
            camera.reset();
            returnAnimation = null;
            restorePerspective(client);
        }
    }

    private void animate() {
        if (state != RuntimeState.RETURNING || returnAnimation == null) return;
        var sample = returnAnimation.sample(System.nanoTime());
        camera.setOffsets(sample.yawOffset(), sample.pitchOffset());
        if (sample.complete()) {
            camera.reset();
            returnAnimation = null;
            state = RuntimeState.IDLE;
            restorePerspective(MinecraftClient.getInstance());
        }
    }

    private void hardReset(MinecraftClient client) {
        drainActivationPresses();
        restorePerspective(client);
        camera.reset();
        returnAnimation = null;
        state = RuntimeState.IDLE;
    }

    private void drainActivationPresses() {
        if (activationKey != null) {
            while (activationKey.wasPressed()) {
                // Invalid contexts must not queue an activation for gameplay.
            }
        }
    }

    private void restorePerspective(MinecraftClient client) {
        if (ownsPerspective && client.options.getPerspective() == Perspective.THIRD_PERSON_BACK) {
            client.options.setPerspective(previousPerspective);
        }
        ownsPerspective = false;
        previousPerspective = null;
    }

    private void relinquishPerspectiveIfChanged(MinecraftClient client) {
        if (!ownsPerspective || client.options.getPerspective() == Perspective.THIRD_PERSON_BACK) return;
        ownsPerspective = false;
        previousPerspective = null;
        if (state == RuntimeState.RETURNING) {
            camera.reset();
            returnAnimation = null;
            state = RuntimeState.IDLE;
        }
    }

    private void rememberOwners(MinecraftClient client) {
        ownerPlayer = client.player;
        ownerWorld = client.world;
        ownerHandler = client.getNetworkHandler();
        ownerCamera = client.getCameraEntity();
    }
}
