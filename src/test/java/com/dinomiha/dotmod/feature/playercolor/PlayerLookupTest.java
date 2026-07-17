package com.dinomiha.dotmod.feature.playercolor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerLookupTest {
    @Test
    void resolvesCaseInsensitivelyByUuidIdentity() {
        UUID uuid = UUID.randomUUID();
        PlayerLookup.Result result = PlayerLookup.resolve("alice", List.of(new PlayerIdentity(uuid, "Alice")));
        assertEquals(PlayerLookup.Status.FOUND, result.status());
        assertEquals(uuid, result.player().uuid());
    }

    @Test
    void distinguishesUnknownAndAmbiguousNames() {
        assertEquals(PlayerLookup.Status.UNKNOWN, PlayerLookup.resolve("Nobody", List.of()).status());
        PlayerLookup.Result ambiguous = PlayerLookup.resolve("Alex", List.of(
                new PlayerIdentity(UUID.randomUUID(), "Alex"),
                new PlayerIdentity(UUID.randomUUID(), "alex")
        ));
        assertEquals(PlayerLookup.Status.AMBIGUOUS, ambiguous.status());
    }

    @Test
    void duplicateEntriesForTheSameUuidAreNotAmbiguous() {
        UUID uuid = UUID.randomUUID();
        assertEquals(PlayerLookup.Status.FOUND, PlayerLookup.resolve("Sam", List.of(
                new PlayerIdentity(uuid, "Sam"), new PlayerIdentity(uuid, "sam")
        )).status());
    }
}
