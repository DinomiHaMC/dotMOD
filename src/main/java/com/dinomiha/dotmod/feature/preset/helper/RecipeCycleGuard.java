package com.dinomiha.dotmod.feature.preset.helper;

import java.util.HashSet;
import java.util.Set;

/** Path-local guard; repeated nodes in separate dependency branches remain valid. */
public final class RecipeCycleGuard {
    private final Set<ExactItemKey> path = new HashSet<>();

    public boolean enter(ExactItemKey key) {
        return path.add(key);
    }

    public void leave(ExactItemKey key) {
        path.remove(key);
    }
}
