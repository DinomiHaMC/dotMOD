package com.dinomiha.dotmod.hud.widget;

import com.dinomiha.dotmod.DotModClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudWidgetRegistry {
    private static final List<HudWidget> WIDGETS = List.of(
            new ArmorWidget(), new ColoredOnlineWidget(), new DurabilityWidget(),
            new MovementIndicatorWidget(), new FreelookIndicatorWidget()
    );
    private static final Map<String, HudWidget> BY_ID;
    private static boolean registered;

    static {
        LinkedHashMap<String, HudWidget> values = new LinkedHashMap<>();
        WIDGETS.forEach(widget -> values.put(widget.id(), widget));
        BY_ID = Map.copyOf(values);
    }

    private HudWidgetRegistry() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        VanillaHudWidgetRenderer.register();
        for (HudWidget widget : WIDGETS) {
            String path = widget.id().replace('.', '/');
            HudElementRegistry.attachElementBefore(
                    VanillaHudElements.CHAT,
                    Identifier.of(DotModClient.MOD_ID, path),
                    (context, tickCounter) -> HudWidgetRenderer.renderRuntime(context, widget)
            );
        }
    }

    public static HudWidget get(String id) {
        return BY_ID.get(id);
    }

    public static List<HudWidget> widgets() {
        return WIDGETS;
    }
}
