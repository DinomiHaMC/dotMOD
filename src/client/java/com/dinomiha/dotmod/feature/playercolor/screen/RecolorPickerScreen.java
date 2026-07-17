package com.dinomiha.dotmod.feature.playercolor.screen;

import com.dinomiha.dotmod.config.PlayerColorService;
import com.dinomiha.dotmod.feature.playercolor.PlayerIdentity;
import com.dinomiha.dotmod.feature.playercolor.StrictHexColor;
import com.dinomiha.dotmod.message.MessageService;
import com.dinomiha.dotmod.message.MessageType;
import com.dinomiha.dotmod.ui.component.DotButton;
import com.dinomiha.dotmod.ui.component.DotColorPicker;
import com.dinomiha.dotmod.ui.component.DotTextField;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public final class RecolorPickerScreen extends Screen {
    private final Screen parent;
    private final PlayerIdentity player;
    private TextFieldWidget field;
    private DotColorPicker picker;
    private String draft;
    private Text error = Text.empty();

    public RecolorPickerScreen(Screen parent, PlayerIdentity player) {
        super(Text.translatable("screen.dotmod.recolor.title", player.name()));
        this.parent = parent;
        this.player = player;
        this.draft = PlayerColorService.get().color(player.uuid()).orElse("#FFFFFF");
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(220, width - 24);
        int x = (width - panelWidth) / 2;
        int y = height / 2 - 70;
        field = addDrawableChild(DotTextField.create(
                textRenderer, x, y, panelWidth, 20,
                Text.translatable("screen.dotmod.recolor.hex"), Text.literal("#RRGGBB"), draft, 7,
                value -> value.matches("#?[0-9a-fA-F]{0,6}"),
                value -> {
                    draft = value;
                    error = Text.empty();
                    StrictHexColor.parse(value).ifPresent(picker::setColor);
                }
        ));
        picker = addDrawableChild(new DotColorPicker(x, y + 26, panelWidth, 64, draft, value -> {
            draft = value;
            field.setText(value);
        }));
        addDrawableChild(DotButton.create(x, y + 96, (panelWidth - 4) / 2,
                Text.translatable("screen.dotmod.recolor.apply"), button -> apply()));
        addDrawableChild(DotButton.create(x + (panelWidth + 4) / 2, y + 96, (panelWidth - 4) / 2,
                Text.translatable("gui.cancel"), button -> close()));
        setInitialFocus(field);
    }

    private void apply() {
        String color = StrictHexColor.parse(draft).orElse(null);
        if (color == null) {
            error = Text.translatable("message.dotmod.recolor.invalid_hex");
            return;
        }
        boolean saved = PlayerColorService.get().set(player.uuid(), color, player.name());
        MessageService.sendChat(Text.translatable(
                saved ? "message.dotmod.player_colors.set" : "message.dotmod.player_colors.save_failed",
                player.name(), color
        ), saved ? MessageType.SUCCESS : MessageType.ERROR);
        close();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEnter()) {
            apply();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 92, 0xFFFFFF);
        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height / 2 + 54, 0xFF5555);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
