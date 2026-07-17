package com.dinomiha.dotmod.hud.widget;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudSnapperTest {
    @Test
    void snapsToScreenEdgesAndCenters() {
        HudSnapper.SnapResult edge = HudSnapper.snap(3, 4, 20, 10, 320, 240, 4, List.of());
        assertEquals(0, edge.x());
        assertEquals(0, edge.y());

        HudSnapper.SnapResult center = HudSnapper.snap(149, 114, 20, 10, 320, 240, 2, List.of());
        assertEquals(150, center.x());
        assertEquals(115, center.y());
    }

    @Test
    void snapsAdjacentWidgetEdgesAndClampsAfterward() {
        HudPlacement target = new HudPlacement(50, 50, 20, 20, 1, 1, true);
        HudSnapper.SnapResult result = HudSnapper.snap(72, 49, 10, 10, 100, 100, 3, List.of(target));

        assertEquals(70, result.x());
        assertEquals(50, result.y());
    }
}
