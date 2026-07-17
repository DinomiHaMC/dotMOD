package com.dinomiha.dotmod.feature.death;

import com.dinomiha.dotmod.feature.death.model.DeathRecord;
import com.dinomiha.dotmod.feature.invsee.InvSeeMode;
import com.dinomiha.dotmod.feature.invsee.InvSeeSession;
import com.dinomiha.dotmod.feature.invsee.screen.InvSeeMenu;
import com.dinomiha.dotmod.feature.screenshot.DesktopPlatformService;
import com.dinomiha.dotmod.feature.screenshot.screen.ImageViewerScreen;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

import java.nio.file.Path;

public final class DeathActions {
    private DeathActions() {
    }

    public static void show(DeathRecord record) {
        var snapshot = record.snapshot();
        MessageService.sendChat(Text.translatable("command.dotmod.death.details",
                shortId(record), snapshot.deathMessage(), snapshot.dimension(),
                snapshot.blockX(), snapshot.blockY(), snapshot.blockZ(), snapshot.experienceLevel()), MessageType.INFO);
    }

    public static void inventory(MinecraftClient client, Screen parent, DeathRecord record) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            MessageService.sendChat(Text.translatable("message.dotmod.death.no_connection"), MessageType.ERROR);
            return;
        }
        client.setScreen(new InvSeeMenu(parent, Text.translatable("screen.dotmod.death.inventory", shortId(record)),
                new InvSeeSession(InvSeeMode.VIEW, record.snapshot().inventory()), null, handler.getRegistryManager()));
    }

    public static void view(MinecraftClient client, Screen parent, DeathRecord record) {
        try {
            Path path = DeathClientService.get().screenshotPath(record);
            client.setScreen(new ImageViewerScreen(parent, DeathClientService.get().root().resolve("images"), path));
        } catch (RuntimeException exception) {
            MessageService.sendChat(Text.translatable("message.dotmod.death.no_screenshot"), MessageType.ERROR);
        }
    }

    public static void copy(MinecraftClient client, DeathRecord record) {
        try {
            DesktopPlatformService.copyPath(client, DeathClientService.get().screenshotPath(record));
            MessageService.sendChat(Text.translatable("command.dotmod.death.copied"), MessageType.SUCCESS);
        } catch (RuntimeException exception) {
            MessageService.sendChat(Text.translatable("message.dotmod.death.no_screenshot"), MessageType.ERROR);
        }
    }

    public static void open(DeathRecord record) {
        try {
            DesktopPlatformService.open(DeathClientService.get().screenshotPath(record));
        } catch (RuntimeException exception) {
            MessageService.sendChat(Text.translatable("message.dotmod.death.no_screenshot"), MessageType.ERROR);
        }
    }

    public static String shortId(DeathRecord record) {
        return record.id().toString().substring(0, 8);
    }
}
