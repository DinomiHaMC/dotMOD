package com.dinomiha.dotmod.feature.preset.screen;

import com.dinomiha.dotmod.feature.preset.helper.PresetRequirement;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public final class PresetRequirementRowWidget extends ClickableWidget {
    private final TextRenderer textRenderer;
    private final PressAction action;
    private PresetRequirement requirement;
    private boolean selected;
    private boolean craftable;

    public PresetRequirementRowWidget(int x, int y, int width, TextRenderer textRenderer, PressAction action) {
        super(x, y, width, 22, Text.empty());
        this.textRenderer = textRenderer;
        this.action = action;
    }

    public void setRequirement(PresetRequirement requirement, boolean selected, boolean craftable) {
        this.requirement = requirement;
        this.selected = selected;
        this.craftable = craftable;
        this.active = requirement != null;
        setMessage(requirement == null ? Text.empty() : Text.translatable(
                "screen.dotmod.preset.helper.row.narration",
                requirement.stack().getName(), requirement.required(), requirement.available(), requirement.missing()
        ));
    }

    public PresetRequirement requirement() {
        return requirement;
    }

    @Override
    protected boolean isValidClickButton(MouseInput input) {
        return input.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (requirement != null) {
            action.onPress(requirement);
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int border = selected ? 0xFF55FFFF : isHovered() ? 0xFFFFFFFF : 0xFF555555;
        int background = requirement != null && requirement.complete() ? 0xD0183820 : 0xD0382020;
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
        context.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, background);
        if (requirement == null) {
            return;
        }
        context.drawItem(requirement.stack(), getX() + 3, getY() + 3);
        int textWidth = Math.max(20, getWidth() - 92);
        context.drawTextWithShadow(
                textRenderer,
                textRenderer.trimToWidth(requirement.stack().getName().getString(), textWidth),
                getX() + 23,
                getY() + 7,
                0xFFFFFF
        );
        Text status = Text.literal(requirement.available() + "/" + requirement.required()
                + (craftable ? " +" : ""));
        context.drawTextWithShadow(
                textRenderer,
                status,
                getX() + getWidth() - textRenderer.getWidth(status) - 4,
                getY() + 7,
                requirement.complete() ? 0x55FF55 : craftable ? 0x55FFFF : 0xFFAA55
        );
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(PresetRequirement requirement);
    }
}
