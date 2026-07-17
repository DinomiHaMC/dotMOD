package com.dinomiha.dotmod.feature.preset.screen;

import com.dinomiha.dotmod.feature.preset.PresetException;
import com.dinomiha.dotmod.feature.preset.PresetNameValidator;
import com.dinomiha.dotmod.ui.component.DotButton;
import com.dinomiha.dotmod.ui.component.DotTextField;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public final class PresetNameScreen extends Screen {
    private final Screen parent;
    private final String initialName;
    private final Consumer<String> callback;
    private TextFieldWidget nameField;
    private Text error = Text.empty();
    private String draftName;

    public PresetNameScreen(Screen parent, Text title, String initialName, Consumer<String> callback) {
        super(title);
        this.parent = parent;
        this.initialName = initialName == null ? "" : initialName;
        this.draftName = this.initialName;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(260, width - 24);
        int x = (width - fieldWidth) / 2;
        nameField = addDrawableChild(DotTextField.create(
                textRenderer,
                x,
                height / 2 - 20,
                fieldWidth,
                20,
                Text.translatable("screen.dotmod.preset.name"),
                Text.translatable("screen.dotmod.preset.name.placeholder"),
                draftName,
                PresetNameValidator.MAX_LENGTH,
                value -> value.codePoints().noneMatch(Character::isISOControl),
                value -> {
                    draftName = value;
                    error = Text.empty();
                }
        ));
        addDrawableChild(DotButton.create(x, height / 2 + 8, (fieldWidth - 4) / 2, Text.translatable("gui.done"), button -> submit()));
        addDrawableChild(DotButton.create(x + (fieldWidth + 4) / 2, height / 2 + 8, (fieldWidth - 4) / 2, Text.translatable("gui.cancel"), button -> close()));
        setInitialFocus(nameField);
    }

    private void submit() {
        try {
            String name = PresetNameValidator.normalize(nameField.getText());
            callback.accept(name);
        } catch (PresetException exception) {
            error = Text.translatable("message.dotmod.preset.error." + exception.error().name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEnter()) {
            submit();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 48, 0xFFFFFFFF);
        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height / 2 + 34, 0xFFFF5555);
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
