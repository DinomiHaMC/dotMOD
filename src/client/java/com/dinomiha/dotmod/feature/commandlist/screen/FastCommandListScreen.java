package com.dinomiha.dotmod.feature.commandlist.screen;

import com.dinomiha.dotmod.feature.commandalias.CommandClientService;
import com.dinomiha.dotmod.feature.commandlist.CommandEntry;
import com.dinomiha.dotmod.feature.commandlist.DangerousCommandPolicy;
import com.dinomiha.dotmod.ui.component.DotButton;
import com.dinomiha.dotmod.ui.component.DotConfirmationDialog;
import com.dinomiha.dotmod.ui.component.DotTextField;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class FastCommandListScreen extends Screen {
    private static final int ROW_HEIGHT = 18;
    private static final int MAX_VISIBLE_ROWS = 12;

    private final Screen parent;
    private TextFieldWidget commandField;
    private ButtonWidget executeButton;
    private ButtonWidget pinButton;
    private List<Row> rows = List.of();
    private int listX;
    private int listY;
    private int listWidth;

    public FastCommandListScreen(Screen parent) {
        super(Text.translatable("screen.dotmod.commands.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(360, width - 24);
        listX = (width - panelWidth) / 2;
        listWidth = panelWidth;
        listY = Math.max(32, height / 2 - 150);
        commandField = addDrawableChild(DotTextField.create(
                textRenderer, listX, listY, panelWidth, 20,
                Text.translatable("screen.dotmod.commands.field"),
                Text.translatable("screen.dotmod.commands.placeholder"),
                "", CommandEntry.MAX_COMMAND_LENGTH,
                value -> value.codePoints().noneMatch(Character::isISOControl),
                value -> updateButtons()
        ));
        int buttonY = Math.min(height - 28, listY + 28 + MAX_VISIBLE_ROWS * ROW_HEIGHT);
        int buttonWidth = (panelWidth - 12) / 4;
        executeButton = addDrawableChild(DotButton.create(listX, buttonY, buttonWidth,
                Text.translatable("screen.dotmod.commands.execute"), button -> requestExecute()));
        pinButton = addDrawableChild(DotButton.create(listX + buttonWidth + 4, buttonY, buttonWidth,
                Text.translatable("screen.dotmod.commands.pin"), button -> togglePin()));
        addDrawableChild(DotButton.create(listX + (buttonWidth + 4) * 2, buttonY, buttonWidth,
                Text.translatable("screen.dotmod.commands.clear"), button -> {
                    CommandClientService.get().history().clearRecent();
                    refreshRows();
                }));
        addDrawableChild(DotButton.create(listX + (buttonWidth + 4) * 3, buttonY, buttonWidth,
                Text.translatable("gui.done"), button -> close()));
        refreshRows();
        setInitialFocus(commandField);
    }

    private void refreshRows() {
        ArrayList<Row> next = new ArrayList<>();
        CommandClientService.get().history().pinned().forEach(entry -> next.add(new Row(entry, true)));
        CommandClientService.get().history().recent().forEach(entry -> next.add(new Row(entry, false)));
        rows = List.copyOf(next.subList(0, Math.min(MAX_VISIBLE_ROWS, next.size())));
        updateButtons();
    }

    private void select(Row row) {
        commandField.setText(row.entry().command());
        commandField.setCursorToEnd(false);
        setFocused(commandField);
        updateButtons();
    }

    private void togglePin() {
        String command = commandField.getText();
        if (command.isBlank()) {
            return;
        }
        boolean pinned = CommandClientService.get().history().pinned().stream()
                .anyMatch(entry -> entry.command().equals(normalized(command)));
        if (pinned) {
            CommandClientService.get().history().unpin(command);
        } else {
            CommandClientService.get().history().pin(command);
        }
        refreshRows();
    }

    private void requestExecute() {
        String command = commandField.getText();
        if (command.isBlank() || client == null || client.getNetworkHandler() == null) {
            return;
        }
        String normalized;
        try {
            normalized = new CommandEntry(command).command();
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (DangerousCommandPolicy.isDangerous(normalized)) {
            DotConfirmationDialog.open(
                    client, this,
                    Text.translatable("screen.dotmod.commands.dangerous.title"),
                    Text.translatable("screen.dotmod.commands.dangerous.message", normalized),
                    () -> execute(normalized)
            );
        } else {
            execute(normalized);
        }
    }

    private void execute(String command) {
        if (client == null || client.getNetworkHandler() == null) {
            return;
        }
        client.setScreen(parent);
        client.getNetworkHandler().sendChatCommand(command.substring(1));
    }

    private void updateButtons() {
        if (executeButton == null || pinButton == null || commandField == null) {
            return;
        }
        executeButton.active = !commandField.getText().isBlank();
        String command = normalized(commandField.getText());
        boolean pinned = !command.isEmpty() && CommandClientService.get().history().pinned().stream()
                .anyMatch(entry -> entry.command().equals(command));
        pinButton.setMessage(Text.translatable(pinned
                ? "screen.dotmod.commands.unpin" : "screen.dotmod.commands.pin"));
        pinButton.active = !commandField.getText().isBlank();
    }

    private static String normalized(String command) {
        try {
            return command.isBlank() ? "" : new CommandEntry(command).command();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEnter()) {
            requestExecute();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() == 0 && click.x() >= listX && click.x() < listX + listWidth) {
            int row = ((int) click.y() - (listY + 24)) / ROW_HEIGHT;
            if (row >= 0 && row < rows.size()
                    && click.y() >= listY + 24 && click.y() < listY + 24 + rows.size() * ROW_HEIGHT) {
                select(rows.get(row));
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, listY - 18, 0xFFFFFFFF);
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            int y = listY + 24 + index * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
            context.fill(listX, y, listX + listWidth, y + ROW_HEIGHT - 1, hovered ? 0xAA353535 : 0xAA202020);
            context.drawTextWithShadow(textRenderer,
                    (row.pinned() ? "* " : "  ") + textRenderer.trimToWidth(row.entry().command(), listWidth - 12),
                    listX + 5, y + 5, row.pinned() ? 0xFFFFFFAA : 0xFFDDDDDD);
        }
        if (rows.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.dotmod.commands.empty"),
                    width / 2, listY + 34, 0xFF888888);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Row(CommandEntry entry, boolean pinned) {
    }
}
