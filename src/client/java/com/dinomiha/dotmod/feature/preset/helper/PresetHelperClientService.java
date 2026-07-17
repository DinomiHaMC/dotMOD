package com.dinomiha.dotmod.feature.preset.helper;

import com.dinomiha.dotmod.feature.invsee.InvSeeMode;
import com.dinomiha.dotmod.feature.invsee.InvSeeSession;
import com.dinomiha.dotmod.feature.invsee.screen.InvSeeMenu;
import com.dinomiha.dotmod.feature.invsee.screen.InvSeeSupplement;
import com.dinomiha.dotmod.feature.preset.PresetError;
import com.dinomiha.dotmod.feature.preset.PresetException;
import com.dinomiha.dotmod.feature.preset.PresetRecord;
import com.dinomiha.dotmod.feature.preset.screen.PresetHelperScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public final class PresetHelperClientService {
    private PresetHelperClientService() {
    }

    public static void open(MinecraftClient client, Screen parent, PresetRecord preset) {
        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
            throw new PresetException(PresetError.READ_ONLY, "Preset helper requires an active world");
        }
        ClientInventorySnapshot snapshot = ClientInventorySnapshot.capture(client)
                .orElseThrow(() -> new PresetException(PresetError.STALE_DATA, "Inventory changed during helper capture"));
        InventoryCounter available = InventoryCounter.combine(snapshot.playerStacks(), snapshot.containerStacks());
        PresetProgress progress = new PresetComparisonService().compare(preset.preset().inventory(), available);
        PresetHelperModel model = new PresetHelperModel(
                preset, progress, available, ClientRecipeCatalog.capture(client), snapshot.containerOpen()
        );
        ClientPlayNetworkHandler connection = client.getNetworkHandler();
        ScreenHandler handler = client.player.currentScreenHandler;
        InvSeeSupplement supplement = new InvSeeSupplement(
                Text.translatable(
                        "screen.dotmod.preset.helper.summary",
                        progress.available(), progress.required(), progress.missing(), progress.percentage()
                ),
                Text.translatable("screen.dotmod.preset.helper.details"),
                ism -> client.setScreen(new PresetHelperScreen(ism, model)),
                progress.missingSlots()
        );
        client.setScreen(new InvSeeMenu(
                parent,
                Text.translatable("screen.dotmod.preset.helper.ism", preset.preset().name()),
                new InvSeeSession(InvSeeMode.VIEW, preset.preset().inventory()),
                null,
                client.getNetworkHandler().getRegistryManager(),
                supplement,
                () -> parent != null
                        && client.getNetworkHandler() == connection
                        && client.player != null
                        && client.player.currentScreenHandler == handler
                        && (!(parent instanceof HandledScreen<?> handled)
                                || handled.getScreenHandler() == handler)
        ));
    }
}
