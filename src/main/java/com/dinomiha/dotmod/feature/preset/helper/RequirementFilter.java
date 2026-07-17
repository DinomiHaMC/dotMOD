package com.dinomiha.dotmod.feature.preset.helper;

public enum RequirementFilter {
    ALL,
    MISSING,
    COMPLETE,
    CRAFTABLE;

    public RequirementFilter next() {
        RequirementFilter[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
