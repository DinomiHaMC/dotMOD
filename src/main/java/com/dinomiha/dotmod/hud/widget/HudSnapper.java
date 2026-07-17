package com.dinomiha.dotmod.hud.widget;

import java.util.List;

public final class HudSnapper {
    private HudSnapper() {
    }

    public static SnapResult snap(
            int proposedX,
            int proposedY,
            int width,
            int height,
            int screenWidth,
            int screenHeight,
            int threshold,
            List<HudPlacement> others
    ) {
        Axis x = snapAxis(proposedX, width, screenWidth, threshold, others, true);
        Axis y = snapAxis(proposedY, height, screenHeight, threshold, others, false);
        return new SnapResult(
                Math.max(0, Math.min(Math.max(0, screenWidth - width), x.position)),
                Math.max(0, Math.min(Math.max(0, screenHeight - height), y.position)),
                x.guide,
                y.guide
        );
    }

    private static Axis snapAxis(
            int position,
            int size,
            int screenSize,
            int threshold,
            List<HudPlacement> others,
            boolean horizontal
    ) {
        int bestPosition = position;
        int bestDistance = threshold + 1;
        Integer guide = null;
        int[] source = {position, position + size / 2, position + size};
        int[] screen = {0, screenSize / 2, screenSize};
        for (int index = 0; index < 3; index++) {
            int delta = screen[index] - source[index];
            if (Math.abs(delta) <= threshold && Math.abs(delta) < bestDistance) {
                bestPosition = position + delta;
                bestDistance = Math.abs(delta);
                guide = screen[index];
            }
        }
        for (HudPlacement other : others == null ? List.<HudPlacement>of() : others) {
            int start = horizontal ? other.x() : other.y();
            int otherSize = horizontal ? other.width() : other.height();
            int[] target = {start, start + otherSize / 2, start + otherSize};
            int[][] pairs = {{0, 0}, {1, 1}, {2, 2}, {0, 2}, {2, 0}};
            for (int[] pair : pairs) {
                int delta = target[pair[1]] - source[pair[0]];
                if (Math.abs(delta) <= threshold && Math.abs(delta) < bestDistance) {
                    bestPosition = position + delta;
                    bestDistance = Math.abs(delta);
                    guide = target[pair[1]];
                }
            }
        }
        return new Axis(bestPosition, guide);
    }

    public record SnapResult(int x, int y, Integer verticalGuide, Integer horizontalGuide) {
    }

    private record Axis(int position, Integer guide) {
    }
}
