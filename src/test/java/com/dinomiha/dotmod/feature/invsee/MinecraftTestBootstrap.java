package com.dinomiha.dotmod.feature.invsee;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;

final class MinecraftTestBootstrap {
    private static RegistryWrapper.WrapperLookup registries;

    private MinecraftTestBootstrap() {
    }

    static synchronized RegistryWrapper.WrapperLookup registries() {
        if (registries == null) {
            SharedConstants.createGameVersion();
            Bootstrap.initialize();
            registries = BuiltinRegistries.createWrapperLookup();
        }
        return registries;
    }

    static RegistryOps<JsonElement> jsonOps() {
        return registries().getOps(JsonOps.INSTANCE);
    }
}
