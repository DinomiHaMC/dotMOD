package com.dinomiha.dotmod.util;

import java.util.ArrayList;
import java.util.List;

public final class SlotListParser {
    private SlotListParser() {
    }

    public static String format(List<Integer> slots) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(slots.get(i));
        }
        return builder.toString();
    }

    public static List<Integer> parse(String text, List<Integer> fallback) {
        List<Integer> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return new ArrayList<>(fallback);
        }
        String[] parts = text.split("[,;\\s]+");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            try {
                int slot = Integer.parseInt(part);
                if (slot >= 0 && slot <= 35) {
                    result.add(slot);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return result.isEmpty() ? new ArrayList<>(fallback) : result;
    }
}
