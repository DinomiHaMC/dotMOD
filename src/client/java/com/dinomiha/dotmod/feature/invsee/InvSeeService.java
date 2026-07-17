package com.dinomiha.dotmod.feature.invsee;

import com.dinomiha.dotmod.DotModClient;
import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.feature.invsee.persistence.InvSeeDraftRepository;
import com.dinomiha.dotmod.feature.invsee.screen.InvSeeMenu;
import com.dinomiha.dotmod.feature.invsee.source.PlayerInventorySnapshotSource;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InvSeeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DotModClient.MOD_ID + "/ism");

    private InvSeeService() {
    }

    public static void open(MinecraftClient client, InvSeeMode mode) {
        if (client.player == null) {
            MessageService.sendChat(Text.translatable("message.dotmod.ism.no_player"), MessageType.ERROR);
            return;
        }
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) {
            MessageService.sendChat(Text.translatable("message.dotmod.ism.no_registries"), MessageType.ERROR);
            return;
        }

        try {
            RegistryWrapper.WrapperLookup registries = networkHandler.getRegistryManager();
            VirtualInventorySnapshot initial = PlayerInventorySnapshotSource.capture(client.player);
            InvSeeSaveTarget saveTarget = null;

            if (mode != InvSeeMode.VIEW) {
                InvSeeDraftRepository repository = new InvSeeDraftRepository(
                        ConfigService.get().paths().invSeeDraftFile(),
                        new GsonBuilder().setPrettyPrinting().create(),
                        registries
                );
                InvSeeDraftRepository.LoadOutcome draft = repository.load();
                if (mode == InvSeeMode.CREATIVE) {
                    initial = draft.snapshot().orElseGet(VirtualInventorySnapshot::empty);
                }
                if (draft.recovered()) {
                    MessageService.sendChat(Text.translatable("message.dotmod.ism.draft_recovered"), MessageType.WARNING);
                }
                if (draft.writeBlocked()) {
                    MessageService.sendChat(Text.translatable(
                            draft.decodeFailed()
                                    ? "message.dotmod.ism.draft_unavailable"
                                    : "message.dotmod.ism.draft_read_only"
                    ), MessageType.WARNING);
                }
                saveTarget = repository::save;
            }

            client.setScreen(new InvSeeMenu(
                    null,
                    Text.translatable("screen.dotmod.ism.title"),
                    new InvSeeSession(mode, initial),
                    saveTarget,
                    registries
            ));
        } catch (RuntimeException exception) {
            LOGGER.error("Could not open InvSeeMenu", exception);
            MessageService.sendChat(Text.translatable("message.dotmod.ism.open_failed"), MessageType.ERROR);
        }
    }
}
