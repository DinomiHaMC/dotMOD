package com.dinomiha.dotmod.hud.widget;

import com.dinomiha.dotmod.util.NameColorManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ColoredOnlineWidget implements HudWidget {
    private static final int LIMIT = 8;

    @Override
    public String id() {
        return HudWidgetDefaults.COLORED_ONLINE;
    }

    @Override
    public boolean hasContent(MinecraftClient client, boolean preview) {
        return preview || !rows(client).isEmpty();
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float alpha, boolean preview) {
        List<Row> rows = preview
                ? List.of(
                        new Row(Text.translatable("screen.dotmod.hud_editor.preview.player_green"), 0x55FF55),
                        new Row(Text.translatable("screen.dotmod.hud_editor.preview.player_red"), 0xFF5555)
                )
                : rows(client);
        int height = Math.max(12, rows.size() * 10 + 4);
        context.fill(0, 0, 140, height, HudPlacementResolver.applyAlpha(0x90000000, alpha));
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            int color = HudPlacementResolver.applyAlpha(0xFF000000 | row.rgb, alpha);
            context.drawTextWithShadow(client.textRenderer,
                    client.textRenderer.trimToWidth(row.name.getString(), 134), 3, 3 + index * 10, color);
        }
    }

    private static List<Row> rows(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            return List.of();
        }
        return List.copyOf(client.getNetworkHandler().getListedPlayerListEntries()).stream()
                .map(entry -> NameColorManager.colorFor(entry.getProfile().id())
                        .map(color -> new Row(
                                client.inGameHud.getPlayerListHud().getPlayerName(entry), color
                        ))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(row -> row.name.getString(), String.CASE_INSENSITIVE_ORDER))
                .limit(LIMIT)
                .toList();
    }

    private record Row(Text name, int rgb) {
    }
}
