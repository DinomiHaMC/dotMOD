package com.dinomiha.dotmod.feature.togglewalk;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.keybind.DotModKeybindCategory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.StickyKeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class MovementLifecycle {
    private static final MovementLifecycle INSTANCE = new MovementLifecycle();

    private final ToggleWalkController controller = new ToggleWalkController();
    private KeyBinding toggleWalk;
    private KeyBinding toggleSprint;
    private KeyBinding toggleShift;
    private KeyBinding emergencyRelease;
    private Object ownerPlayer;
    private Object ownerWorld;
    private Object ownerConnection;
    private boolean ownsForward;
    private boolean ownsSprint;
    private boolean ownsJump;
    private boolean ownsSneak;

    private MovementLifecycle() {
    }

    public static void initialize() {
        INSTANCE.toggleWalk = register("key.dotmod.toggle_walk", InputUtil.UNKNOWN_KEY.getCode());
        INSTANCE.toggleSprint = register("key.dotmod.toggle_sprint", InputUtil.UNKNOWN_KEY.getCode());
        INSTANCE.toggleShift = register("key.dotmod.toggle_shift", GLFW.GLFW_KEY_RIGHT_SHIFT);
        INSTANCE.emergencyRelease = register("key.dotmod.emergency_release", InputUtil.UNKNOWN_KEY.getCode());
        ClientTickEvents.START_CLIENT_TICK.register(INSTANCE::tick);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) ->
                INSTANCE.hardRelease(client, MovementReleaseReason.WORLD_CHANGE));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                INSTANCE.hardRelease(client, MovementReleaseReason.CONNECTION_CHANGE));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!screenAllowed(client)) INSTANCE.hardRelease(client, MovementReleaseReason.SCREEN);
            ScreenKeyboardEvents.allowKeyPress(screen).register((ignored, input) -> {
                if (!INSTANCE.emergencyRelease.matchesKey(input)) return true;
                INSTANCE.emergency(client);
                return false;
            });
            ScreenMouseEvents.allowMouseClick(screen).register((ignored, click) -> {
                if (!INSTANCE.emergencyRelease.matchesMouse(click)) return true;
                INSTANCE.emergency(client);
                return false;
            });
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
                INSTANCE.hardRelease(client, MovementReleaseReason.CLIENT_STOPPING));
    }

    public static MovementSnapshot snapshot() {
        return INSTANCE.controller.snapshot();
    }

    private static KeyBinding register(String translation, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translation, InputUtil.Type.KEYSYM, key, DotModKeybindCategory.INSTANCE));
    }

    private void tick(MinecraftClient client) {
        boolean emergency = false;
        while (emergencyRelease.wasPressed()) {
            emergency(client);
            emergency = true;
        }
        if (emergency) return;

        Object connection = client.getNetworkHandler();
        if (ownerPlayer != null && ownerPlayer != client.player) {
            hardRelease(client, MovementReleaseReason.PLAYER_CHANGE);
        } else if (ownerWorld != null && ownerWorld != client.world) {
            hardRelease(client, MovementReleaseReason.WORLD_CHANGE);
        } else if (ownerConnection != null && ownerConnection != connection) {
            hardRelease(client, MovementReleaseReason.CONNECTION_CHANGE);
        }
        ownerPlayer = client.player;
        ownerWorld = client.world;
        ownerConnection = connection;

        DotModConfig config = ConfigService.get().config();
        if (!config.toggleWalk.enabled && controller.snapshot().movementActive())
            controller.deactivateWalk(MovementReleaseReason.DISABLED);
        if (!config.toggleWalk.toggleSprint.enabled && controller.snapshot().toggleSprintActive())
            controller.deactivateSprint(MovementReleaseReason.DISABLED);
        if (!config.toggleWalk.toggleShift.enabled && controller.snapshot().sneaking())
            controller.deactivateSneak(MovementReleaseReason.DISABLED);
        MovementContext context = context(client);
        boolean userChanged = false;
        while (toggleWalk.wasPressed()) {
            if (ConfigService.get().config().toggleWalk.enabled) {
                controller.toggleWalk(context, captureMovement(client));
                userChanged = true;
            }
        }
        while (toggleShift.wasPressed()) {
            if (ConfigService.get().config().toggleWalk.toggleShift.enabled) {
                controller.toggleSneak(context);
                userChanged = true;
            }
        }
        while (toggleSprint.wasPressed()) {
            if (ConfigService.get().config().toggleWalk.toggleSprint.enabled) {
                controller.toggleSprint(context);
                userChanged = true;
            }
        }
        apply(client, controller.update(context).forcedKeys(), userChanged);
    }

    private void emergency(MinecraftClient client) {
        hardRelease(client, MovementReleaseReason.EMERGENCY);
        while (toggleWalk.wasPressed()) {
            // Prevent a conflicting binding from reactivating movement.
        }
        while (toggleShift.wasPressed()) {
            // Prevent a conflicting binding from reactivating movement.
        }
        while (toggleSprint.wasPressed()) {
            // Prevent a conflicting binding from reactivating movement.
        }
    }

    private static MovementContext context(MinecraftClient client) {
        DotModConfig config = ConfigService.get().config();
        boolean live = client.player != null && client.world != null && client.getNetworkHandler() != null;
        return new MovementContext(
                config.general.enabled,
                live,
                live && !client.player.isDead(),
                screenAllowed(client),
                client.isWindowFocused(),
                config.toggleWalk.retainSprint,
                live && client.player.isSprinting()
        );
    }

    private static boolean screenAllowed(MinecraftClient client) {
        DotModConfig config = ConfigService.get().config();
        return client.currentScreen == null
                || client.currentScreen instanceof ChatScreen && !config.toggleWalk.deactivateInChat
                || !(client.currentScreen instanceof ChatScreen) && !config.toggleWalk.deactivateInOtherScreens;
    }

    private void hardRelease(MinecraftClient client, MovementReleaseReason reason) {
        controller.release(reason);
        apply(client, ForcedKeyState.NONE, false);
        ownerPlayer = client.player;
        ownerWorld = client.world;
        ownerConnection = client.getNetworkHandler();
    }

    private void apply(MinecraftClient client, ForcedKeyState state, boolean restorePhysical) {
        ownsForward = apply(client, client.options.forwardKey, ownsForward, state.forward(), restorePhysical);
        ownsSprint = apply(client, client.options.sprintKey, ownsSprint, state.sprint(), restorePhysical);
        ownsJump = apply(client, client.options.jumpKey, ownsJump, state.jump(), restorePhysical);
        ownsSneak = apply(client, client.options.sneakKey, ownsSneak, state.sneak(), restorePhysical);
    }

    private static ForcedKeyState captureMovement(MinecraftClient client) {
        return new ForcedKeyState(
                physicallyPressed(client, client.options.forwardKey),
                physicallyPressed(client, client.options.sprintKey),
                physicallyPressed(client, client.options.jumpKey),
                false
        );
    }

    private static boolean apply(MinecraftClient client, KeyBinding binding, boolean owned, boolean force, boolean restorePhysical) {
        if (force) {
            if (owned && binding instanceof StickyKeyBinding sticky) {
                sticky.shouldRestoreOnScreenClose();
            }
            if (!binding.isPressed()) {
                setPressed(binding, true);
                return true;
            }
            return owned;
        }
        if (owned) {
            setPressed(binding, restorePhysical && physicallyPressed(client, binding));
            if (binding instanceof StickyKeyBinding sticky) {
                sticky.shouldRestoreOnScreenClose();
            }
        }
        return false;
    }

    private static void setPressed(KeyBinding binding, boolean pressed) {
        if (binding instanceof StickyKeyBinding) {
            // False is effective in hold mode and harmless in toggle mode. The
            // conditional true then converges in either mode, including a mode switch.
            binding.setPressed(false);
            if (binding.isPressed() != pressed) {
                binding.setPressed(true);
            }
        } else {
            binding.setPressed(pressed);
        }
    }

    private static boolean physicallyPressed(MinecraftClient client, KeyBinding binding) {
        if (client.currentScreen != null || !client.isWindowFocused()) return false;
        InputUtil.Key key = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), key.getCode()) == GLFW.GLFW_PRESS;
        }
        return key.getCategory() == InputUtil.Type.KEYSYM && InputUtil.isKeyPressed(client.getWindow(), key.getCode());
    }
}
