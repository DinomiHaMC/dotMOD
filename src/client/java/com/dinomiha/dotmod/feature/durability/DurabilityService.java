package com.dinomiha.dotmod.feature.durability;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DurabilityService {
    private static final List<SlotSource> SOURCES = List.of(
            new SlotSource(DurabilitySlot.MAIN_HAND, null),
            new SlotSource(DurabilitySlot.OFF_HAND, null),
            new SlotSource(DurabilitySlot.HEAD, EquipmentSlot.HEAD),
            new SlotSource(DurabilitySlot.CHEST, EquipmentSlot.CHEST),
            new SlotSource(DurabilitySlot.LEGS, EquipmentSlot.LEGS),
            new SlotSource(DurabilitySlot.FEET, EquipmentSlot.FEET)
    );

    private DurabilityService() {
    }

    public static List<Entry> read(ClientPlayerEntity player) {
        if (player == null) {
            return List.of();
        }
        List<Entry> result = new ArrayList<>();
        for (SlotSource source : SOURCES) {
            ItemStack stack = switch (source.slot) {
                case MAIN_HAND -> player.getMainHandStack();
                case OFF_HAND -> player.getOffHandStack();
                default -> player.getEquippedStack(source.equipmentSlot);
            };
            if (!stack.isEmpty() && stack.isDamageable() && stack.getMaxDamage() > 0) {
                result.add(new Entry(
                        DurabilityReading.of(source.slot, stack.getMaxDamage(), stack.getDamage()),
                        stack.copy()
                ));
            }
        }
        return List.copyOf(result);
    }

    public record Entry(DurabilityReading reading, ItemStack stack) {
        public Entry {
            stack = stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    private record SlotSource(DurabilitySlot slot, EquipmentSlot equipmentSlot) {
    }
}
