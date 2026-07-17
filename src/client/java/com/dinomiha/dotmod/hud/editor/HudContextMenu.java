package com.dinomiha.dotmod.hud.editor;

import com.dinomiha.dotmod.hud.widget.HudWidgetSettings;
import com.dinomiha.dotmod.ui.component.DotContextMenu;
import net.minecraft.text.Text;

import java.util.List;
import java.util.ArrayList;

public final class HudContextMenu {
    private HudContextMenu() {
    }

    public static List<DotContextMenu.Action> actions(
            HudEditorController controller,
            String id,
            int width,
            int height,
            Runnable save
    ) {
        HudWidgetSettings settings = controller.settings(id);
        List<DotContextMenu.Action> actions = new ArrayList<>();
        actions.add(action(Text.translatable(settings.visible
                        ? "screen.dotmod.hud_editor.context.hide"
                        : "screen.dotmod.hud_editor.context.show"), () -> controller.toggleVisible(id), save));
        actions.add(action(Text.translatable("screen.dotmod.hud_editor.context.scale", settings.scale),
                () -> controller.cycleScale(id), save));
        if (com.dinomiha.dotmod.hud.widget.HudWidgetDefaults.require(id).custom()) {
            actions.add(action(Text.translatable("screen.dotmod.hud_editor.context.alpha", Math.round(settings.alpha * 100)),
                    () -> controller.cycleAlpha(id), save));
        }
        actions.add(action(Text.translatable("screen.dotmod.hud_editor.context.anchor", Text.translatable(
                                "config.dotmod.hud.anchor." + settings.anchor.next().name().toLowerCase(java.util.Locale.ROOT))),
                () -> controller.cycleAnchor(id, width, height), save));
        actions.add(action(Text.translatable("screen.dotmod.hud_editor.context.reset"), () -> controller.reset(id), save));
        return List.copyOf(actions);
    }

    private static DotContextMenu.Action action(Text label, Runnable operation, Runnable save) {
        return new DotContextMenu.Action(label, null, true, () -> {
            operation.run();
            save.run();
        });
    }
}
