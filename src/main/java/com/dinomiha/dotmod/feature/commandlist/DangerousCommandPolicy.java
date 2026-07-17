package com.dinomiha.dotmod.feature.commandlist;

import java.util.Set;

public final class DangerousCommandPolicy {
    private static final Set<String> DANGEROUS_ROOTS = Set.of(
            "op",
            "deop",
            "ban",
            "ban-ip",
            "kick",
            "stop",
            "whitelist",
            "pardon",
            "pardon-ip"
    );

    private DangerousCommandPolicy() {
    }

    public static boolean isDangerous(String command) {
        return DANGEROUS_ROOTS.contains(SensitiveCommandFilter.commandRoot(command));
    }
}
