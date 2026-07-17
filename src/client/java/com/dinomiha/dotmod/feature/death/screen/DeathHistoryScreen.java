package com.dinomiha.dotmod.feature.death.screen;

import com.dinomiha.dotmod.feature.death.DeathActions;
import com.dinomiha.dotmod.feature.death.DeathClientService;
import com.dinomiha.dotmod.feature.death.model.DeathRecord;
import com.dinomiha.dotmod.ui.component.DotButton;
import com.dinomiha.dotmod.ui.component.DotConfirmationDialog;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DeathHistoryScreen extends Screen {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private final Screen parent;
    private List<DeathRecord> records = List.of();
    private DeathRecord selected;
    private int pageStart;

    public DeathHistoryScreen(Screen parent) {
        super(Text.translatable("screen.dotmod.death_history.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        records = DeathClientService.get().list();
        if (selected != null) {
            selected = records.stream().filter(record -> record.id().equals(selected.id())).findFirst().orElse(null);
        }
        int rowWidth = Math.min(520, width - 24);
        int x = (width - rowWidth) / 2;
        int shown = Math.min(records.size(), Math.max(1, (height - 96) / 22));
        pageStart = Math.min(pageStart, Math.max(0, records.size() - 1));
        int pageSize = Math.min(shown, records.size() - pageStart);
        for (int index = 0; index < pageSize; index++) {
            DeathRecord record = records.get(pageStart + index);
            String label = DeathActions.shortId(record) + "  " + TIME.format(record.diedAt()) + "  "
                    + record.snapshot().blockX() + ", " + record.snapshot().blockY() + ", " + record.snapshot().blockZ();
            addDrawableChild(DotButton.create(x, 28 + index * 22, rowWidth, Text.literal(label), button -> {
                selected = record;
                clearAndInit();
            }));
        }
        ButtonWidget previous = addDrawableChild(DotButton.create(x, 4, 72,
                Text.translatable("screen.dotmod.death_history.previous"), button -> {
                    pageStart = Math.max(0, pageStart - shown);
                    clearAndInit();
                }));
        previous.active = pageStart > 0;
        ButtonWidget next = addDrawableChild(DotButton.create(x + rowWidth - 72, 4, 72,
                Text.translatable("screen.dotmod.death_history.next"), button -> {
                    pageStart += shown;
                    clearAndInit();
                }));
        next.active = pageStart + pageSize < records.size();
        int actionWidth = Math.min(520, width - 16);
        int buttonY = height - 70;
        int buttonWidth = Math.max(32, (actionWidth - 12) / 4);
        int buttonX = (width - actionWidth) / 2;
        action(buttonX, buttonY, buttonWidth, "screen.dotmod.death_history.show", () -> DeathActions.show(selected));
        action(buttonX += buttonWidth + 4, buttonY, buttonWidth, "screen.dotmod.death_history.inventory", () -> DeathActions.inventory(client, this, selected));
        action(buttonX += buttonWidth + 4, buttonY, buttonWidth, "screen.dotmod.death_history.view", () -> DeathActions.view(client, this, selected));
        action(buttonX += buttonWidth + 4, buttonY, buttonWidth, "screen.dotmod.death_history.copy", () -> DeathActions.copy(client, selected));
        buttonX = (width - actionWidth) / 2;
        buttonY += 22;
        action(buttonX, buttonY, buttonWidth, "screen.dotmod.death_history.open", () -> DeathActions.open(selected));
        action(buttonX += buttonWidth + 4, buttonY, buttonWidth, "screen.dotmod.death_history.delete", this::confirmDelete);
        ButtonWidget clear = addDrawableChild(DotButton.create(buttonX + buttonWidth + 4, buttonY, buttonWidth,
                Text.translatable("screen.dotmod.death_history.clear"), button -> confirmClear()));
        clear.active = !records.isEmpty();
        addDrawableChild(DotButton.create(width / 2 - 50, height - 26, 100, Text.translatable("gui.back"), button -> close()));
    }

    private void action(int x, int y, int width, String key, Runnable action) {
        ButtonWidget button = addDrawableChild(DotButton.create(x, y, width, Text.translatable(key), ignored -> action.run()));
        button.active = selected != null;
    }

    private void confirmDelete() {
        DotConfirmationDialog.open(client, this, Text.translatable("command.dotmod.death.delete.title"),
                Text.translatable("command.dotmod.death.delete.message", DeathActions.shortId(selected)), () -> {
                    DeathClientService.get().delete(selected.id());
                    selected = null;
                    clearAndInit();
                });
    }

    private void confirmClear() {
        DotConfirmationDialog.open(client, this, Text.translatable("command.dotmod.death.clear.title"),
                Text.translatable("command.dotmod.death.clear.message", records.size()), () -> {
                    DeathClientService.get().clear();
                    selected = null;
                    clearAndInit();
                });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF15171B);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        if (records.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.dotmod.death_history.empty"),
                    width / 2, height / 2, 0xFFB0B0B0);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
