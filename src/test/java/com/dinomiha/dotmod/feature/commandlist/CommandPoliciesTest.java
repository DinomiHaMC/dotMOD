package com.dinomiha.dotmod.feature.commandlist;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPoliciesTest {
    @Test
    void detectsEverySensitiveRootCaseInsensitively() {
        for (String root : List.of("login", "register", "changepassword", "password", "auth", "2fa", "otp")) {
            assertTrue(SensitiveCommandFilter.isSensitive(" \t/// \n" + root.toUpperCase() + " secret"), root);
        }
        assertFalse(SensitiveCommandFilter.isSensitive("/say login"));
        assertFalse(SensitiveCommandFilter.isSensitive("/logins secret"));
        assertFalse(SensitiveCommandFilter.isSensitive(null));
    }

    @Test
    void detectsEveryDangerousRootWithoutMatchingArgumentsOrPrefixes() {
        for (String root : List.of("op", "deop", "ban", "ban-ip", "kick", "stop", "whitelist", "pardon", "pardon-ip")) {
            assertTrue(DangerousCommandPolicy.isDangerous(" // " + root.toUpperCase() + " target"), root);
        }
        assertFalse(DangerousCommandPolicy.isDangerous("/say ban"));
        assertFalse(DangerousCommandPolicy.isDangerous("/banner test"));
    }
}
