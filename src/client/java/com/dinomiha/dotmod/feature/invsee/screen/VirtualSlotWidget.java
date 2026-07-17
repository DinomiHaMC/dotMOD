package com.dinomiha.dotmod.feature.invsee.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class VirtualSlotWidget extends ClickableWidget {
    public static final int SIZE = 18;

    private final int referenceIndex;
    private final TextRenderer textRenderer;
    private final PressAction action;
    private ItemStack stack = ItemStack.EMPTY;
    private boolean selected;
    private boolean warning;

    public VirtualSlotWidget(int referenceIndex, int x, int y, TextRenderer textRenderer, PressAction action) {
        super(x, y, SIZE, SIZE, Text.translatable("screen.dotmod.ism.slot.empty"));
        this.referenceIndex = referenceIndex;
        this.textRenderer = textRenderer;
        this.action = action;
    }

    public int referenceIndex() {
        return referenceIndex;
    }

    public ItemStack stack() {
        return stack.copy();
    }

    public void setStack(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        setMessage(this.stack.isEmpty()
                ? Text.translatable("screen.dotmod.ism.slot.empty")
                : Text.translatable("screen.dotmod.ism.slot.item", this.stack.getName(), this.stack.getCount()));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setWarning(boolean warning) {
        this.warning = warning;
    }

    @Override
    protected boolean isValidClickButton(MouseInput input) {
        return input.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT
                || input.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        action.onPress(this, click.button(), click.hasShift());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (active && visible && (input.isEnter() || input.getKeycode() == InputUtil.GLFW_KEY_SPACE)) {
            action.onPress(this, InputUtil.GLFW_MOUSE_BUTTON_LEFT, input.hasShift());
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int border = selected ? 0xFF55FFFF : warning ? 0xFFFFAA00 : isFocused() || isHovered() ? 0xFFFFFFFF : 0xFF777777;
        context.fill(getX(), getY(), getX() + SIZE, getY() + SIZE, border);
        context.fill(getX() + 1, getY() + 1, getX() + SIZE - 1, getY() + SIZE - 1, 0xDD202020);
        if (!stack.isEmpty()) {
            context.drawItem(stack, getX() + 1, getY() + 1);
            context.drawStackOverlay(textRenderer, stack, getX() + 1, getY() + 1);
        }
        if (warning) {
            context.drawStrokedRectangle(getX() + 2, getY() + 2, SIZE - 4, SIZE - 4, 0xFFFFAA00);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(VirtualSlotWidget widget, int button, boolean shift);
    }
}
