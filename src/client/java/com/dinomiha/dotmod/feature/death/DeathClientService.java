package com.dinomiha.dotmod.feature.death;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.feature.death.model.DeathEffect;
import com.dinomiha.dotmod.feature.death.model.DeathRecord;
import com.dinomiha.dotmod.feature.death.model.DeathSnapshot;
import com.dinomiha.dotmod.feature.invsee.source.PlayerInventorySnapshotSource;
import com.dinomiha.dotmod.feature.screenshot.ScreenshotPathPolicy;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DeathClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger("dotmod/deaths");
    private static DeathClientService instance;

    private final ConfigService config;
    private ClientPlayNetworkHandler repositoryConnection;
    private DeathRepository repository;
    private ClientPlayerEntity capturedPlayer;
    private boolean deathCaptured;

    private DeathClientService(ConfigService config) {
        this.config = config;
    }

    public static void initialize() {
        instance = new DeathClientService(ConfigService.get());
        ClientTickEvents.END_CLIENT_TICK.register(instance::tick);
    }

    public static DeathClientService get() {
        if (instance == null) {
            throw new IllegalStateException("DeathClientService has not been initialized");
        }
        return instance;
    }

    public Path root() {
        return config.paths().deathsDirectory().toAbsolutePath().normalize();
    }

    public void capture(DeathMessageS2CPacket packet, ClientPlayNetworkHandler handler) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!enabled() || player == null || client.world == null || handler != client.getNetworkHandler()
                || packet.playerId() != player.getId() || deathCaptured && capturedPlayer == player) {
            return;
        }
        deathCaptured = true;
        capturedPlayer = player;
        try {
            BlockPos block = player.getBlockPos();
            DamageSource damage = player.getRecentDamageSource();
            Entity attacker = damage == null ? null : damage.getAttacker();
            List<DeathEffect> effects = player.getStatusEffects().stream()
                    .limit(DeathSnapshot.MAX_EFFECTS)
                    .map(effect -> new DeathEffect(
                            effect.getEffectType().getIdAsString(),
                            effect.getEffectType().value().getName().getString(),
                            effect.getAmplifier(), effect.getDuration(), effect.isAmbient(),
                            effect.shouldShowParticles(), effect.shouldShowIcon()
                    ))
                    .toList();
            DeathSnapshot snapshot = new DeathSnapshot(
                    player.getUuid(), player.getName().getString(), client.world.getRegistryKey().getValue().toString(),
                    player.getX(), player.getY(), player.getZ(), block.getX(), block.getY(), block.getZ(),
                    packet.message().getString(), damage == null ? null : damage.getTypeRegistryEntry().getIdAsString(),
                    attacker == null ? null : attacker.getUuid(), attacker == null ? null : attacker.getName().getString(),
                    player.getInventory().getSelectedSlot(), player.experienceLevel, player.totalExperience,
                    player.experienceProgress, effects, PlayerInventorySnapshotSource.capture(player)
            );
            DeathRecord record = repository(handler).create(snapshot);
            sendCapturedMessage(record);
            if (config.config().screenshots.enabled) {
                DeathScreenshotQueue.enqueue(record.id(), repository, handler, client.world);
            } else {
                repository.markScreenshotFailed(record.id(), "Death screenshots are disabled");
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Could not capture local death", exception);
            MessageService.sendChat(Text.translatable("message.dotmod.death.capture_failed"), MessageType.ERROR);
        }
    }

    public List<DeathRecord> list() {
        return currentRepository().list();
    }

    public Optional<DeathRecord> get(String id) {
        return currentRepository().get(id);
    }

    public void delete(UUID id) {
        currentRepository().deleteToTrash(id);
    }

    public int clear() {
        return currentRepository().clearToTrash();
    }

    public void markScreenshotSaved(UUID id, Path relative) {
        currentRepository().markScreenshotSaved(id, relative);
    }

    public void markScreenshotFailed(UUID id, String error) {
        currentRepository().markScreenshotFailed(id, error);
    }

    public Path screenshotPath(DeathRecord record) {
        if (record.screenshot().relativePath() == null) {
            throw new DeathException(DeathError.NOT_FOUND, "Death screenshot is unavailable");
        }
        Path images = root().resolve("images");
        Path path = root().resolve(record.screenshot().relativePath()).normalize();
        try {
            return new ScreenshotPathPolicy(images).validateExisting(path);
        } catch (IOException | IllegalArgumentException exception) {
            throw new DeathException(DeathError.INVALID_DATA, "Unsafe or missing death screenshot", exception);
        }
    }

    private DeathRepository currentRepository() {
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) {
            if (repository == null) {
                throw new DeathException(DeathError.IO_FAILURE, "Death history requires an active connection");
            }
            return repository;
        }
        return repository(handler);
    }

    private DeathRepository repository(ClientPlayNetworkHandler handler) {
        if (repository == null || repositoryConnection != handler) {
            repository = new DeathRepository(root(), handler.getRegistryManager());
            repositoryConnection = handler;
        }
        return repository;
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            deathCaptured = false;
            capturedPlayer = null;
            repository = null;
            repositoryConnection = null;
        } else if (client.player != capturedPlayer || client.player.isAlive()) {
            deathCaptured = false;
            capturedPlayer = client.player;
        }
    }

    private boolean enabled() {
        return config.config().general.enabled && config.config().deathHistory.enabled;
    }

    private void sendCapturedMessage(DeathRecord record) {
        String id = record.id().toString().substring(0, 8);
        MutableText message = Text.translatable("message.dotmod.death.captured", id).append(Text.literal(" "));
        message.append(MessageService.commandAction(Text.translatable("command.dotmod.death.action.show"),
                "/dot deaths show " + id, Text.translatable("command.dotmod.death.action.show.tooltip")));
        message.append(Text.literal(" "));
        message.append(MessageService.commandAction(Text.translatable("command.dotmod.death.action.copy"),
                "/dot deaths copy " + id, Text.translatable("command.dotmod.death.action.copy.tooltip")));
        MessageService.sendChat(message, MessageType.SUCCESS);
    }
}
