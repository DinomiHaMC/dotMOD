package com.dinomiha.dotmod.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalizationTest {
    @Test
    void englishAndRussianContainTheSameKeys() {
        JsonObject english = language("en_us");
        JsonObject russian = language("ru_ru");
        assertEquals(english.keySet(), russian.keySet());
        assertNotNull(english.get("key.category.dotmod.controls"));
    }

    private static JsonObject language(String code) {
        String path = "/assets/dotmod/lang/" + code + ".json";
        var stream = LocalizationTest.class.getResourceAsStream(path);
        assertNotNull(stream, "Missing language file " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("Could not read " + path, exception);
        }
    }
}
