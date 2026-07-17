package com.dinomiha.dotmod.feature.playercolor;

import java.util.Collection;
import java.util.List;

public final class PlayerLookup {
    private PlayerLookup() {
    }

    public static Result resolve(String name, Collection<PlayerIdentity> players) {
        if (name == null) {
            return new Result(Status.UNKNOWN, null);
        }
        List<PlayerIdentity> matches = players.stream()
                .filter(player -> player.name().equalsIgnoreCase(name))
                .toList();
        if (matches.isEmpty()) {
            return new Result(Status.UNKNOWN, null);
        }
        if (matches.stream().map(PlayerIdentity::uuid).distinct().count() != 1) {
            return new Result(Status.AMBIGUOUS, null);
        }
        return new Result(Status.FOUND, matches.getFirst());
    }

    public enum Status {
        FOUND,
        UNKNOWN,
        AMBIGUOUS
    }

    public record Result(Status status, PlayerIdentity player) {
    }
}
