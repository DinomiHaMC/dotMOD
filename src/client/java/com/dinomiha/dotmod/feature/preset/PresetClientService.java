package com.dinomiha.dotmod.feature.preset;

import com.dinomiha.dotmod.DotModClient;
import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.feature.invsee.InvSeeMode;
import com.dinomiha.dotmod.feature.invsee.InvSeeSession;
import com.dinomiha.dotmod.feature.invsee.VirtualInventorySnapshot;
import com.dinomiha.dotmod.feature.invsee.screen.InvSeeMenu;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PresetClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DotModClient.MOD_ID + "/presets");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PresetClientService() {
    }

    public static List<PresetRecord> list(MinecraftClient client) {
        return context(client).repository().list();
    }

    public static Optional<PresetRecord> active(MinecraftClient client) {
        return context(client).repository().active();
    }

    public static PresetRecord require(MinecraftClient client, String name) {
        return context(client).repository().requireByName(name);
    }

    public static PresetRecord require(MinecraftClient client, UUID id) {
        return context(client).repository().list().stream()
                .filter(record -> record.preset().id().equals(id))
                .findFirst()
                .orElseThrow(() -> new PresetException(PresetError.NOT_FOUND, "Preset not found"));
    }

    public static void select(MinecraftClient client, PresetRecord record) {
        context(client).repository().select(record.preset().id());
    }

    public static PresetRecord rename(MinecraftClient client, PresetRecord record, String name) {
        return context(client).repository().rename(record.preset().id(), record.revision(), name);
    }

    public static PresetRecord duplicate(MinecraftClient client, PresetRecord record, String name) {
        return context(client).repository().duplicate(record.preset().id(), record.revision(), name);
    }

    public static void delete(MinecraftClient client, PresetRecord record) {
        context(client).repository().delete(record.preset().id(), record.revision());
    }

    public static String exportPreset(MinecraftClient client, PresetRecord record) {
        Context context = context(client);
        return context.exchange().exportPreset(record.preset());
    }

    public static PresetRecord importPreset(MinecraftClient client, String json) {
        Context context = context(client);
        return context.repository().importPreset(context.exchange().importPreset(json));
    }

    public static void openCreate(MinecraftClient client, Screen parent, String requestedName) {
        Context context = context(client);
        String name = PresetNameValidator.normalize(requestedName);
        if (context.repository().findByName(name).isPresent()) {
            throw new PresetException(PresetError.NAME_CONFLICT, "Preset name already exists");
        }
        ClientPlayNetworkHandler connection = client.getNetworkHandler();
        client.setScreen(new InvSeeMenu(
                parent,
                Text.translatable("screen.dotmod.preset.create", name),
                new InvSeeSession(InvSeeMode.CREATIVE, VirtualInventorySnapshot.empty()),
                snapshot -> saveCreated(client, connection, name, snapshot),
                context.registries()
        ));
    }

    public static void openView(MinecraftClient client, Screen parent, PresetRecord record) {
        Context context = context(client);
        client.setScreen(new InvSeeMenu(
                parent,
                Text.translatable("screen.dotmod.preset.view", record.preset().name()),
                new InvSeeSession(InvSeeMode.VIEW, record.preset().inventory()),
                null,
                context.registries()
        ));
    }

    public static void openEdit(MinecraftClient client, Screen parent, PresetRecord record) {
        Context context = context(client);
        ClientPlayNetworkHandler connection = client.getNetworkHandler();
        client.setScreen(new InvSeeMenu(
                parent,
                Text.translatable("screen.dotmod.preset.edit", record.preset().name()),
                new InvSeeSession(InvSeeMode.EDIT, record.preset().inventory()),
                snapshot -> saveEdited(client, connection, record, snapshot),
                context.registries()
        ));
    }

    private static boolean saveCreated(
            MinecraftClient client,
            ClientPlayNetworkHandler connection,
            String name,
            VirtualInventorySnapshot snapshot
    ) {
        if (connection == null || client.getNetworkHandler() != connection) {
            return false;
        }
        try {
            context(client).repository().create(name, snapshot);
            return true;
        } catch (PresetException exception) {
            report(exception);
            return false;
        }
    }

    private static boolean saveEdited(
            MinecraftClient client,
            ClientPlayNetworkHandler connection,
            PresetRecord record,
            VirtualInventorySnapshot snapshot
    ) {
        if (connection == null || client.getNetworkHandler() != connection) {
            return false;
        }
        try {
            context(client).repository().updateInventory(record.preset().id(), record.revision(), snapshot);
            return true;
        } catch (PresetException exception) {
            report(exception);
            return false;
        }
    }

    public static void report(PresetException exception) {
        LOGGER.warn("Preset operation failed: {}", exception.getMessage());
        MessageService.sendChat(
                Text.translatable("message.dotmod.preset.error." + exception.error().name().toLowerCase(java.util.Locale.ROOT)),
                MessageType.ERROR
        );
    }

    private static Context context(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            throw new PresetException(PresetError.READ_ONLY, "No active client registries");
        }
        RegistryWrapper.WrapperLookup registries = client.getNetworkHandler().getRegistryManager();
        PresetRepository repository = new PresetRepository(
                ConfigService.get().paths().presetsDirectory(),
                GSON,
                registries,
                Clock.systemUTC(),
                UUID::randomUUID
        );
        return new Context(repository, new PresetImportExportService(GSON, registries), registries);
    }

    private record Context(
            PresetRepository repository,
            PresetImportExportService exchange,
            RegistryWrapper.WrapperLookup registries
    ) {
    }
}
