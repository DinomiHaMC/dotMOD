package com.dinomiha.dotmod.hud.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public final class ArmorWidget implements HudWidget {
    private static final List<EquipmentSlot> SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    );

    @Override
    public String id() {
        return HudWidgetDefaults.ARMOR;
    }

    @Override
    public boolean hasContent(MinecraftClient client, boolean preview) {
        return preview || client.player != null && SLOTS.stream()
                .anyMatch(slot -> !client.player.getEquippedStack(slot).isEmpty());
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float alpha, boolean preview) {
        List<ItemStack> stacks = preview
                ? List.of(
                        new ItemStack(Items.DIAMOND_HELMET),
                        new ItemStack(Items.DIAMOND_CHESTPLATE),
                        new ItemStack(Items.DIAMOND_LEGGINGS),
                        new ItemStack(Items.DIAMOND_BOOTS)
                )
                : SLOTS.stream().map(slot -> client.player.getEquippedStack(slot).copy()).toList();
        context.fill(0, 0, 72, 18, HudPlacementResolver.applyAlpha(0x80000000, alpha));
        for (int index = 0; index < stacks.size(); index++) {
            if (!stacks.get(index).isEmpty()) {
                context.drawItem(stacks.get(index), index * 18 + 1, 1);
            }
        }
    }
}
