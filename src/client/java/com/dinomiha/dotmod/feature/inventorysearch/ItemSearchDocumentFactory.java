package com.dinomiha.dotmod.feature.inventorysearch;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ItemSearchDocumentFactory {
    private ItemSearchDocumentFactory() {
    }

    public static ItemSearchDocument create(ItemStack stack, MinecraftClient client, boolean includeTooltip) {
        if (stack == null || stack.isEmpty() || client == null) {
            throw new IllegalArgumentException("A non-empty client stack is required");
        }
        LoreComponent lore = stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
        List<String> loreLines = lore.lines().stream().limit(32).map(Text::getString).toList();
        List<String> enchantments = new ArrayList<>();
        addEnchantments(enchantments, stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT
        ));
        addEnchantments(enchantments, stack.getOrDefault(
                DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT
        ));
        List<String> tooltip = includeTooltip ? tooltip(stack, client) : List.of();
        Integer durability = null;
        if (stack.isDamageable() && stack.getMaxDamage() > 0) {
            long remaining = Math.max(0, stack.getMaxDamage() - stack.getDamage());
            durability = (int) (remaining * 100L / stack.getMaxDamage());
        }
        return ItemSearchDocument.of(
                stack.getName().getString(),
                Registries.ITEM.getId(stack.getItem()).toString(),
                loreLines,
                enchantments,
                tooltip,
                stack.getCount(),
                durability
        );
    }

    private static void addEnchantments(List<String> output, ItemEnchantmentsComponent component) {
        for (var entry : component.getEnchantmentEntries()) {
            if (output.size() >= 64) {
                break;
            }
            var enchantment = entry.getKey();
            int level = entry.getIntValue();
            output.add(enchantment.getKey()
                    .map(key -> key.getValue().toString())
                    .orElseGet(enchantment::getIdAsString));
            if (output.size() < 64) {
                output.add(Enchantment.getName(enchantment, level).getString());
            }
        }
    }

    private static List<String> tooltip(ItemStack stack, MinecraftClient client) {
        try {
            Item.TooltipContext context = client.world != null
                    ? Item.TooltipContext.create(client.world)
                    : client.getNetworkHandler() != null
                    ? Item.TooltipContext.create(client.getNetworkHandler().getRegistryManager())
                    : Item.TooltipContext.DEFAULT;
            return stack.getTooltip(context, client.player, TooltipType.BASIC).stream()
                    .limit(64)
                    .map(Text::getString)
                    .toList();
        } catch (RuntimeException ignored) {
            // A third-party tooltip failure must not hide an otherwise valid stack.
            return List.of();
        }
    }
}
