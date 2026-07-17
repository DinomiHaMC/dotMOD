package com.dinomiha.dotmod.util;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.config.PlayerColorService;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public final class NameColorManager {
    private NameColorManager() {
    }

    public static void colorTargetedPlayer(String hexColor) {
        Optional<PlayerEntity> target = targetedPlayer();
        if (target.isEmpty()) {
            notify(Text.translatable("message.dotmod.player_colors.no_target"), MessageType.WARNING);
            return;
        }
        PlayerEntity player = target.get();
        String color = ColorUtil.normalizeHex(hexColor, "#FFFFFF");
        boolean saved = PlayerColorService.get().set(player.getUuid(), color);
        notify(
                saved
                        ? Text.translatable("message.dotmod.player_colors.set", player.getName(), color)
                        : Text.translatable("message.dotmod.player_colors.save_failed", player.getName()),
                saved ? MessageType.SUCCESS : MessageType.ERROR
        );
    }

    public static void resetTargetedPlayer() {
        Optional<PlayerEntity> target = targetedPlayer();
        if (target.isEmpty()) {
            notify(Text.translatable("message.dotmod.player_colors.no_target"), MessageType.WARNING);
            return;
        }
        PlayerEntity player = target.get();
        boolean saved = PlayerColorService.get().clear(player.getUuid());
        notify(
                Text.translatable(saved ? "message.dotmod.player_colors.reset" : "message.dotmod.player_colors.save_failed", player.getName()),
                saved ? MessageType.SUCCESS : MessageType.ERROR
        );
    }

    public static Optional<Integer> colorFor(UUID uuid) {
        DotModConfig config = ConfigService.get().config();
        if (!config.general.enabled || !config.playerColors.enabled || uuid == null) {
            return Optional.empty();
        }
        String color = PlayerColorService.get().color(uuid).orElse(null);
        if (color == null) {
            return Optional.empty();
        }
        return Optional.of(ColorUtil.parseRgb(color, ColorUtil.parseRgb(config.playerColors.defaultColor, 0xFFFFFF)));
    }

    public static Text coloredCopy(Text text, UUID uuid) {
        Optional<Integer> color = colorFor(uuid);
        if (color.isEmpty()) {
            return text;
        }
        MutableText copy = text.copy();
        return copy.styled(style -> style.withColor(color.get()));
    }

    private static Optional<PlayerEntity> targetedPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        HitResult hit = client.crosshairTarget;
        Entity entity = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        if (entity instanceof PlayerEntity player) {
            return Optional.of(player);
        }
        if (client.player == null || client.world == null) {
            return Optional.empty();
        }
        Vec3d start = client.player.getCameraPosVec(1.0F);
        Vec3d direction = client.player.getRotationVec(1.0F);
        double range = 6.0D;
        Vec3d end = start.add(direction.multiply(range));
        Box searchBox = client.player.getBoundingBox().stretch(direction.multiply(range)).expand(1.0D);
        return client.world.getPlayers().stream()
                .filter(player -> player != client.player && searchBox.intersects(player.getBoundingBox()))
                .filter(player -> player.getBoundingBox().expand(0.35D).raycast(start, end).isPresent())
                .min(Comparator.comparingDouble(player -> player.squaredDistanceTo(client.player)))
                .map(player -> player);
    }

    private static void notify(Text text, MessageType type) {
        DotModConfig config = ConfigService.get().config();
        if (config.playerColors.notifyChanges) {
            MessageService.sendChat(text, type);
        }
    }
}
