package com.dinomiha.dotmod.feature.durability;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public final class DurabilityWarningController {
    private static final DurabilityWarningService WARNINGS = new DurabilityWarningService();

    private DurabilityWarningController() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(DurabilityWarningController::tick);
    }

    private static void tick(MinecraftClient client) {
        var config = ConfigService.get().config();
        if (!config.general.enabled || !config.durability.enabled || !config.durability.warningsEnabled
                || client.player == null || client.world == null || client.player.isInCreativeMode()) {
            WARNINGS.clear();
            return;
        }
        var entries = DurabilityService.read(client.player);
        Set<String> active = entries.stream().map(DurabilityWarningController::identity).collect(Collectors.toSet());
        WARNINGS.retain(active);
        long now = System.nanoTime();
        long cooldown = config.durability.warningCooldownSeconds * 1_000_000_000L;
        var warnings = entries.stream()
                .sorted(Comparator.comparingDouble(entry -> entry.reading().remainingFraction()))
                .filter(entry -> WARNINGS.shouldWarn(
                        identity(entry),
                        entry.reading().remainingFraction(),
                        config.durability.warningThreshold,
                        now,
                        cooldown
                ))
                .toList();
        warnings.stream().findFirst().ifPresent(entry -> MessageService.sendOverlay(
                        Text.translatable(
                                "message.dotmod.durability.warning",
                                entry.stack().getName(),
                                entry.reading().remaining(),
                                entry.reading().maxDamage()
                        ),
                        MessageType.WARNING
                ));
    }

    private static String identity(DurabilityService.Entry entry) {
        ItemStack stack = entry.stack().copyWithCount(1);
        stack.setDamage(0);
        return entry.reading().slot().name() + ":" + ItemStack.hashCode(stack);
    }
}
