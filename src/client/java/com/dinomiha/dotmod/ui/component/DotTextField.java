package com.dinomiha.dotmod.ui.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class DotTextField {
    private DotTextField() {
    }

    public static TextFieldWidget create(
            TextRenderer textRenderer,
            int x,
            int y,
            int width,
            int height,
            Text narration,
            Text placeholder,
            String initialValue,
            int maxLength,
            Predicate<String> validator,
            Consumer<String> changedListener
    ) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, height, narration);
        field.setMaxLength(maxLength);
        field.setTextPredicate(validator);
        field.setPlaceholder(placeholder);
        field.setText(initialValue == null ? "" : initialValue);
        field.setChangedListener(changedListener);
        return field;
    }
}
