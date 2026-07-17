package com.dinomiha.dotmod.feature.preset.screen;

import com.dinomiha.dotmod.feature.preset.PresetRecord;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class PresetRowWidget extends ClickableWidget {
    private final TextRenderer textRenderer;
    private final Consumer<PresetRecord> selectAction;
    private final BiConsumer<PresetRecord, Click> contextAction;
    private PresetRecord record;

    PresetRowWidget(TextRenderer textRenderer, Consumer<PresetRecord> selectAction, BiConsumer<PresetRecord, Click> contextAction) {
        super(0, 0, 1, 20, Text.empty());
        this.textRenderer = textRenderer;
        this.selectAction = selectAction;
        this.contextAction = contextAction;
        visible = false;
    }

    void setRecord(PresetRecord record) {
        this.record = record;
        visible = record != null;
        active = record != null;
        setMessage(record == null ? Text.empty() : Text.literal(record.preset().name()));
    }

    @Override
    protected boolean isValidClickButton(MouseInput input) {
        return input.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT || input.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (record == null) {
            return;
        }
        if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT) {
            contextAction.accept(record, click);
        } else {
            selectAction.accept(record);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (record != null && (input.isEnter() || input.getKeycode() == InputUtil.GLFW_KEY_SPACE)) {
            selectAction.accept(record);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int background = record != null && record.active() ? 0xA0337744 : isHovered() || isFocused() ? 0x80555588 : 0x60202020;
        context.fill(getX(), getY(), getRight(), getBottom(), background);
        if (record != null) {
            String label = (record.active() ? "* " : "") + record.preset().name();
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(label, Math.max(1, getWidth() - 6)), getX() + 3, getY() + 6, 0xFFFFFFFF);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
