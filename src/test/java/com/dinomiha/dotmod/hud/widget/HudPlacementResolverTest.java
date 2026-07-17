package com.dinomiha.dotmod.hud.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudPlacementResolverTest {
    @Test
    void resolvesScaledBottomRightPlacementAndClampsIt() {
        HudWidgetSettings settings = new HudWidgetSettings(
                true, HudAnchor.BOTTOM_RIGHT, -40, -20, 1.5F, 0.5F
        );

        HudPlacement placement = HudPlacementResolver.resolve(settings, 20, 10, 320, 240);

        assertEquals(280, placement.x());
        assertEquals(220, placement.y());
        assertEquals(30, placement.width());
        assertEquals(15, placement.height());
        assertEquals(0.5F, placement.alpha());
    }

    @Test
    void clampsAllEdgesAndOversizedWidgetsDeterministically() {
        HudWidgetSettings settings = new HudWidgetSettings(
                true, HudAnchor.TOP_LEFT, -100, 999, 4.0F, 1.0F
        );

        HudPlacement placement = HudPlacementResolver.resolve(settings, 100, 100, 320, 240);

        assertEquals(0, placement.x());
        assertEquals(0, placement.y());
        assertTrue(placement.width() <= 320);
        assertEquals(240, placement.height());
    }

    @Test
    void customWidgetScaleIsReducedTransientlyToFitViewport() {
        HudWidgetSettings settings = new HudWidgetSettings(
                true, HudAnchor.TOP_LEFT, 0, 0, 4.0F, 1.0F
        );

        HudPlacement placement = HudPlacementResolver.resolve(settings, 140, 84, 320, 240);

        assertTrue(placement.x() + placement.width() <= 320);
        assertTrue(placement.y() + placement.height() <= 240);
        assertTrue(placement.scale() < 4.0F);
    }

    @Test
    void anchorChangePreservesAbsolutePosition() {
        HudWidgetSettings settings = new HudWidgetSettings(
                true, HudAnchor.TOP_LEFT, 20, 30, 1.0F, 1.0F
        );

        HudPlacementResolver.preservePositionForAnchor(
                settings, HudAnchor.BOTTOM_RIGHT, 20, 30, 320, 240
        );

        assertEquals(-300, settings.offsetX);
        assertEquals(-210, settings.offsetY);
    }

    @Test
    void alphaApplicationPreservesRgb() {
        assertEquals(0x40123456, HudPlacementResolver.applyAlpha(0x80123456, 0.5F));
    }
}
