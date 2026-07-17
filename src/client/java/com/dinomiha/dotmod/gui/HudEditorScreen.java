package com.dinomiha.dotmod.gui;

import com.dinomiha.dotmod.config.DotModConfig;
import com.dinomiha.dotmod.hud.HudElement;
import com.dinomiha.dotmod.hud.HudLayout;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class HudEditorScreen extends Screen {
    private static final int GUIDE_COLOR = 0xDD55FFFF;
    private static final int[][] SNAP_ANCHOR_PAIRS = {
            {0, 0},
            {1, 1},
            {2, 2},
            {0, 2},
            {2, 0}
    };

    private final Screen parent;
    private HudElement dragging;
    private int dragStartMouseX;
    private int dragStartMouseY;
    private int dragStartDx;
    private int dragStartDy;
    private Guide verticalGuide;
    private Guide horizontalGuide;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("dotMOD HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> DotModConfig.resetHud())
                .dimensions(8, 8, 80, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(8, 32, 80, 20)
                .build());
    }

    @Override
    public void close() {
        DotModConfig.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0x88000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        renderGrid(context);
        renderGuide(context, verticalGuide);
        renderGuide(context, horizontalGuide);
        for (HudElement element : HudElement.values()) {
            HudLayout.Rect rect = HudLayout.rect(element, width, height);
            boolean hovered = mouseX >= rect.x() && mouseX <= rect.x() + rect.width() && mouseY >= rect.y() && mouseY <= rect.y() + rect.height();
            int color = hovered || element == dragging ? 0xAA55FF55 : 0xAAFFFFFF;
            context.drawStrokedRectangle(rect.x(), rect.y(), rect.width(), rect.height(), color);
            context.drawTextWithShadow(textRenderer, element.displayName(), rect.x() + 3, rect.y() + 3, 0xFFFFFF);
        }
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Drag elements. Positions are saved as dx/dy offsets."), width / 2, 24, 0xA0A0A0);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0) {
            verticalGuide = null;
            horizontalGuide = null;
            HudElement[] elements = HudElement.values();
            for (int index = elements.length - 1; index >= 0; index--) {
                HudElement element = elements[index];
                HudLayout.Rect rect = HudLayout.rect(element, width, height);
                if (mouseX >= rect.x() && mouseX <= rect.x() + rect.width() && mouseY >= rect.y() && mouseY <= rect.y() + rect.height()) {
                    DotModConfig.HudOffset offset = DotModConfig.get().hudOffset(element);
                    dragging = element;
                    dragStartMouseX = (int) mouseX;
                    dragStartMouseY = (int) mouseY;
                    dragStartDx = offset.dx;
                    dragStartDy = offset.dy;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (dragging != null && click.button() == 0) {
            DotModConfig config = DotModConfig.get();
            DotModConfig.HudOffset offset = config.hudOffset(dragging);
            int dx = dragStartDx + (int) mouseX - dragStartMouseX;
            int dy = dragStartDy + (int) mouseY - dragStartMouseY;
            if (config.hudSnapToGrid) {
                int grid = Math.max(1, config.hudGridSize);
                dx = Math.round(dx / (float) grid) * grid;
                dy = Math.round(dy / (float) grid) * grid;
            }
            verticalGuide = null;
            horizontalGuide = null;
            if (config.hudMagneticSnapping) {
                HudLayout.Rect currentRect = HudLayout.rect(dragging, width, height);
                int baseX = currentRect.x() - offset.dx;
                int baseY = currentRect.y() - offset.dy;
                HudLayout.Rect proposedRect = new HudLayout.Rect(baseX + dx, baseY + dy, currentRect.width(), currentRect.height());

                AxisSnap xSnap = snapX(proposedRect, dx, config.hudMagneticSnapDistance);
                dx = xSnap.offset();
                verticalGuide = xSnap.guide();
                proposedRect = new HudLayout.Rect(baseX + dx, proposedRect.y(), proposedRect.width(), proposedRect.height());

                AxisSnap ySnap = snapY(proposedRect, dy, config.hudMagneticSnapDistance);
                dy = ySnap.offset();
                horizontalGuide = ySnap.guide();
            }
            offset.dx = dx;
            offset.dy = dy;
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null && click.button() == 0) {
            dragging = null;
            verticalGuide = null;
            horizontalGuide = null;
            DotModConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    private void renderGrid(DrawContext context) {
        DotModConfig config = DotModConfig.get();
        if (!config.hudSnapToGrid) {
            return;
        }
        int grid = Math.max(2, config.hudGridSize * 8);
        for (int x = 0; x < width; x += grid) {
            context.fill(x, 0, x + 1, height, 0x22000000);
        }
        for (int y = 0; y < height; y += grid) {
            context.fill(0, y, width, y + 1, 0x22000000);
        }
    }

    private AxisSnap snapX(HudLayout.Rect dragged, int proposedDx, int threshold) {
        int bestDx = proposedDx;
        int bestDistance = threshold + 1;
        Guide bestGuide = null;

        int zeroDistance = Math.abs(proposedDx);
        if (zeroDistance <= threshold) {
            bestDx = 0;
            bestDistance = zeroDistance;
            bestGuide = new Guide(true, dragged.x() - proposedDx, 0, height);
        }

        int[] draggedAnchors = xAnchors(dragged);
        for (HudElement element : HudElement.values()) {
            if (element == dragging) {
                continue;
            }
            HudLayout.Rect target = HudLayout.rect(element, width, height);
            int[] targetAnchors = xAnchors(target);
            for (int[] pair : SNAP_ANCHOR_PAIRS) {
                int delta = targetAnchors[pair[1]] - draggedAnchors[pair[0]];
                int distance = Math.abs(delta);
                if (distance <= threshold && distance < bestDistance) {
                    bestDx = proposedDx + delta;
                    bestDistance = distance;
                    int top = Math.min(dragged.y(), target.y());
                    int bottom = Math.max(dragged.y() + dragged.height(), target.y() + target.height());
                    bestGuide = new Guide(true, targetAnchors[pair[1]], top, bottom);
                }
            }
        }
        return new AxisSnap(bestDx, bestGuide);
    }

    private AxisSnap snapY(HudLayout.Rect dragged, int proposedDy, int threshold) {
        int bestDy = proposedDy;
        int bestDistance = threshold + 1;
        Guide bestGuide = null;

        int zeroDistance = Math.abs(proposedDy);
        if (zeroDistance <= threshold) {
            bestDy = 0;
            bestDistance = zeroDistance;
            bestGuide = new Guide(false, dragged.y() - proposedDy, 0, width);
        }

        int[] draggedAnchors = yAnchors(dragged);
        for (HudElement element : HudElement.values()) {
            if (element == dragging) {
                continue;
            }
            HudLayout.Rect target = HudLayout.rect(element, width, height);
            int[] targetAnchors = yAnchors(target);
            for (int[] pair : SNAP_ANCHOR_PAIRS) {
                int delta = targetAnchors[pair[1]] - draggedAnchors[pair[0]];
                int distance = Math.abs(delta);
                if (distance <= threshold && distance < bestDistance) {
                    bestDy = proposedDy + delta;
                    bestDistance = distance;
                    int left = Math.min(dragged.x(), target.x());
                    int right = Math.max(dragged.x() + dragged.width(), target.x() + target.width());
                    bestGuide = new Guide(false, targetAnchors[pair[1]], left, right);
                }
            }
        }
        return new AxisSnap(bestDy, bestGuide);
    }

    private static int[] xAnchors(HudLayout.Rect rect) {
        return new int[]{rect.x(), rect.x() + rect.width() / 2, rect.x() + rect.width()};
    }

    private static int[] yAnchors(HudLayout.Rect rect) {
        return new int[]{rect.y(), rect.y() + rect.height() / 2, rect.y() + rect.height()};
    }

    private static void renderGuide(DrawContext context, Guide guide) {
        if (guide == null) {
            return;
        }
        int start = Math.min(guide.start(), guide.end());
        int end = Math.max(guide.start(), guide.end());
        if (guide.vertical()) {
            context.fill(guide.position(), start, guide.position() + 1, end + 1, GUIDE_COLOR);
        } else {
            context.fill(start, guide.position(), end + 1, guide.position() + 1, GUIDE_COLOR);
        }
    }

    private record AxisSnap(int offset, Guide guide) {
    }

    private record Guide(boolean vertical, int position, int start, int end) {
    }
}
