package com.dinomiha.dotmod.feature.preset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PresetNameValidatorTest {
    @Test
    void normalizesUnicodeAndBuildsCaseInsensitiveConflictKeys() {
        assertEquals("Kit 1", PresetNameValidator.normalize("  Ｋｉｔ 1  "));
        assertEquals(PresetNameValidator.conflictKey("Mining Kit"), PresetNameValidator.conflictKey("MINING KIT"));
    }

    @Test
    void rejectsBlankPathsControlsAndLongNames() {
        assertThrows(PresetException.class, () -> PresetNameValidator.normalize(" "));
        assertThrows(PresetException.class, () -> PresetNameValidator.normalize("../kit"));
        assertThrows(PresetException.class, () -> PresetNameValidator.normalize("bad\nname"));
        assertThrows(PresetException.class, () -> PresetNameValidator.normalize("x".repeat(65)));
    }
}
