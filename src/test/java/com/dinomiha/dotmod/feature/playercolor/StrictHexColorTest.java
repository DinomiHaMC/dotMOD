package com.dinomiha.dotmod.feature.playercolor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrictHexColorTest {
    @Test
    void acceptsOnlySixDigitRgbAndCanonicalizesIt() {
        assertEquals("#A1B2C3", StrictHexColor.parse("a1b2c3").orElseThrow());
        assertEquals("#00FF7A", StrictHexColor.parse("#00ff7a").orElseThrow());
        assertTrue(StrictHexColor.parse("#fff").isEmpty());
        assertTrue(StrictHexColor.parse(" 00FF00 ").isEmpty());
        assertTrue(StrictHexColor.parse("#GG0000").isEmpty());
        assertTrue(StrictHexColor.parse("#FF000000").isEmpty());
    }
}
