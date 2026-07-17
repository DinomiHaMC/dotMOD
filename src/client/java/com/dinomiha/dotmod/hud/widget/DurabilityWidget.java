package com.dinomiha.dotmod.hud.widget;

import com.dinomiha.dotmod.config.ConfigService;
import com.dinomiha.dotmod.feature.durability.DurabilityColorInterpolator;
import com.dinomiha.dotmod.feature.durability.DurabilityService;
import com.dinomiha.dotmod.feature.durability.DurabilitySlot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class DurabilityWidget implements HudWidget {
    @Override
    public String id() {
        return HudWidgetDefaults.DURABILITY;
    }

    @Override
    public boolean hasContent(MinecraftClient client, boolean preview) {
        return preview || client.player != null && !DurabilityService.read(client.player).isEmpty();
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float alpha, boolean preview) {
        List<DurabilityService.Entry> entries = preview ? previewEntries() : DurabilityService.read(client.player);
        context.fill(0, 0, 126, 40, HudPlacementResolver.applyAlpha(0x90000000, alpha));
        var config = ConfigService.get().config().durability;
        int low = com.dinomiha.dotmod.util.ColorUtil.parseRgb(config.lowColor, 0xFF5555);
        int middle = com.dinomiha.dotmod.util.ColorUtil.parseRgb(config.middleColor, 0xFFFF55);
        int high = com.dinomiha.dotmod.util.ColorUtil.parseRgb(config.highColor, 0x55FF55);
        for (int index = 0; index < Math.min(6, entries.size()); index++) {
            DurabilityService.Entry entry = entries.get(index);
            int x = index % 3 * 42;
            int y = index / 3 * 20;
            context.drawItem(entry.stack(), x + 1, y + 1);
            int color = DurabilityColorInterpolator.interpolate(
                    entry.reading().remainingFraction(), low, middle, high
            );
            int bar = (int) Math.round(20 * entry.reading().remainingFraction());
            context.fill(x + 18, y + 5, x + 39, y + 8, HudPlacementResolver.applyAlpha(0xFF202020, alpha));
            context.fill(x + 18, y + 5, x + 18 + bar, y + 8,
                    HudPlacementResolver.applyAlpha(0xFF000000 | color, alpha));
            String percent = Math.round(entry.reading().remainingFraction() * 100) + "%";
            context.drawTextWithShadow(client.textRenderer, percent, x + 18, y + 10,
                    HudPlacementResolver.applyAlpha(0xFF000000 | color, alpha));
        }
    }

    private static List<DurabilityService.Entry> previewEntries() {
        List<DurabilityService.Entry> entries = new ArrayList<>();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(sword.getMaxDamage() / 4);
        entries.add(new DurabilityService.Entry(
                com.dinomiha.dotmod.feature.durability.DurabilityReading.of(
                        DurabilitySlot.MAIN_HAND, sword.getMaxDamage(), sword.getDamage()
                ), sword
        ));
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.setDamage(pickaxe.getMaxDamage() * 3 / 4);
        entries.add(new DurabilityService.Entry(
                com.dinomiha.dotmod.feature.durability.DurabilityReading.of(
                        DurabilitySlot.OFF_HAND, pickaxe.getMaxDamage(), pickaxe.getDamage()
                ), pickaxe
        ));
        return entries;
    }
}
