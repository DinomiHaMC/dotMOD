package com.dinomiha.dotmod.feature.invsee;

import java.util.EnumSet;
import java.util.Set;

public enum InvSeeMode {
    VIEW("screen.dotmod.ism.mode.view", "screen.dotmod.ism.mode.view.short", InvSeeCapability.TOOLTIP, InvSeeCapability.COPY_INFO),
    EDIT(
            "screen.dotmod.ism.mode.edit",
            "screen.dotmod.ism.mode.edit.short",
            InvSeeCapability.TOOLTIP,
            InvSeeCapability.COPY_INFO,
            InvSeeCapability.MUTATE,
            InvSeeCapability.SET_AMOUNT,
            InvSeeCapability.SAVE,
            InvSeeCapability.ROLLBACK
    ),
    CREATIVE(
            "screen.dotmod.ism.mode.creative",
            "screen.dotmod.ism.mode.creative.short",
            InvSeeCapability.TOOLTIP,
            InvSeeCapability.COPY_INFO,
            InvSeeCapability.MUTATE,
            InvSeeCapability.SET_AMOUNT,
            InvSeeCapability.SAVE,
            InvSeeCapability.ROLLBACK,
            InvSeeCapability.CATALOG
    );

    private final String translationKey;
    private final String shortTranslationKey;
    private final Set<InvSeeCapability> capabilities;

    InvSeeMode(String translationKey, String shortTranslationKey, InvSeeCapability... capabilities) {
        this.translationKey = translationKey;
        this.shortTranslationKey = shortTranslationKey;
        this.capabilities = Set.copyOf(EnumSet.of(capabilities[0], capabilities));
    }

    public boolean allows(InvSeeCapability capability) {
        return capabilities.contains(capability);
    }

    public String translationKey() {
        return translationKey;
    }

    public String shortTranslationKey() {
        return shortTranslationKey;
    }
}
