package com.dinomiha.dotmod.feature.invsee;

@FunctionalInterface
public interface InvSeeSaveTarget {
    boolean save(VirtualInventorySnapshot snapshot);
}
