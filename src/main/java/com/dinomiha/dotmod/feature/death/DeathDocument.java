package com.dinomiha.dotmod.feature.death;

import com.dinomiha.dotmod.feature.death.model.DeathEffect;
import com.dinomiha.dotmod.feature.death.model.DeathRecord;
import com.dinomiha.dotmod.feature.death.model.DeathScreenshot;
import com.dinomiha.dotmod.feature.death.model.DeathSnapshot;
import com.dinomiha.dotmod.feature.death.model.ScreenshotStatus;
import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventoryDocument;
import com.dinomiha.dotmod.feature.invsee.persistence.VirtualInventorySerializer;
import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;
import net.minecraft.registry.RegistryWrapper;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DeathDocument {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public String id;
    public String diedAt;
    public String playerId;
    public String playerName;
    public String dimension;
    public double x;
    public double y;
    public double z;
    public int blockX;
    public int blockY;
    public int blockZ;
    public String deathMessage;
    public String damageType;
    public String attackerId;
    public String attackerName;
    public int selectedSlot;
    public int experienceLevel;
    public int totalExperience;
    public float experienceProgress;
    public List<EffectDocument> effects = new ArrayList<>();
    public VirtualInventoryDocument inventory;
    public String screenshotStatus;
    public String screenshotPath;
    public String screenshotError;

    public static DeathDocument fromRecord(
            DeathRecord record,
            VirtualInventorySerializer serializer,
            RegistryWrapper.WrapperLookup registries
    ) {
        DeathSnapshot snapshot = record.snapshot();
        DeathDocument document = new DeathDocument();
        document.id = record.id().toString();
        document.diedAt = record.diedAt().toString();
        document.playerId = snapshot.playerId().toString();
        document.playerName = snapshot.playerName();
        document.dimension = snapshot.dimension();
        document.x = snapshot.x();
        document.y = snapshot.y();
        document.z = snapshot.z();
        document.blockX = snapshot.blockX();
        document.blockY = snapshot.blockY();
        document.blockZ = snapshot.blockZ();
        document.deathMessage = snapshot.deathMessage();
        document.damageType = snapshot.damageType();
        document.attackerId = snapshot.attackerId() == null ? null : snapshot.attackerId().toString();
        document.attackerName = snapshot.attackerName();
        document.selectedSlot = snapshot.selectedSlot();
        document.experienceLevel = snapshot.experienceLevel();
        document.totalExperience = snapshot.totalExperience();
        document.experienceProgress = snapshot.experienceProgress();
        document.effects = snapshot.effects().stream().map(EffectDocument::fromEffect).toList();
        document.inventory = serializer.encode(snapshot.inventory(), registries);
        document.screenshotStatus = record.screenshot().status().name();
        document.screenshotPath = record.screenshot().relativePath();
        document.screenshotError = record.screenshot().error();
        return document;
    }

    public DeathRecord toRecord(VirtualInventorySerializer serializer, RegistryWrapper.WrapperLookup registries) {
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedDataVersionException("Unsupported death record schema " + schemaVersion);
        }
        if (inventory == null || effects == null) {
            throw new DeathException(DeathError.INVALID_DATA, "Death record snapshot is incomplete");
        }
        try {
            UUID recordId = UUID.fromString(id);
            if (ScreenshotStatus.SAVED.name().equals(screenshotStatus)
                    && !Path.of("images", recordId + ".png").toString().replace('\\', '/').equals(screenshotPath)) {
                throw new DeathException(DeathError.INVALID_DATA, "Screenshot path does not belong to this death record");
            }
            UUID attacker = attackerId == null ? null : UUID.fromString(attackerId);
            List<DeathEffect> decodedEffects = effects.stream().map(EffectDocument::toEffect).toList();
            DeathSnapshot snapshot = new DeathSnapshot(
                    UUID.fromString(playerId), playerName, dimension,
                    x, y, z, blockX, blockY, blockZ,
                    deathMessage, damageType, attacker, attackerName,
                    selectedSlot, experienceLevel, totalExperience, experienceProgress,
                    decodedEffects, serializer.decode(inventory, registries)
            );
            DeathScreenshot screenshot = new DeathScreenshot(
                    ScreenshotStatus.valueOf(screenshotStatus), screenshotPath, screenshotError
            );
            return new DeathRecord(recordId, Instant.parse(diedAt), snapshot, screenshot);
        } catch (DeathException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DeathException(DeathError.INVALID_DATA, "Invalid death record", exception);
        }
    }

    public static final class EffectDocument {
        public String id;
        public String name;
        public int amplifier;
        public int durationTicks;
        public boolean ambient;
        public boolean showParticles;
        public boolean showIcon;

        public static EffectDocument fromEffect(DeathEffect effect) {
            EffectDocument document = new EffectDocument();
            document.id = effect.id();
            document.name = effect.name();
            document.amplifier = effect.amplifier();
            document.durationTicks = effect.durationTicks();
            document.ambient = effect.ambient();
            document.showParticles = effect.showParticles();
            document.showIcon = effect.showIcon();
            return document;
        }

        public DeathEffect toEffect() {
            return new DeathEffect(id, name, amplifier, durationTicks, ambient, showParticles, showIcon);
        }
    }
}
