package com.dinomiha.dotmod.feature.commandlist;

import com.dinomiha.dotmod.storage.UnsupportedDataVersionException;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandHistoryDocumentTest {
    private final Gson gson = new Gson();

    @Test
    void validatesSchemaOneDocument() {
        CommandHistoryDocument document = new CommandHistoryDocument();
        document.recent = List.of(new CommandEntry("/home"));
        document.pinned = List.of(new CommandEntry("/spawn"));

        document.validate();
    }

    @Test
    void rejectsOldAndFutureSchemas() {
        CommandHistoryDocument old = gson.fromJson("{\"schemaVersion\":0,\"recent\":[],\"pinned\":[]}", CommandHistoryDocument.class);
        CommandHistoryDocument future = gson.fromJson("{\"schemaVersion\":2,\"recent\":[],\"pinned\":[]}", CommandHistoryDocument.class);

        assertThrows(IllegalArgumentException.class, old::validate);
        assertThrows(UnsupportedDataVersionException.class, future::validate);
    }

    @Test
    void rejectsMissingMalformedDuplicateAndSensitiveEntries() {
        CommandHistoryDocument missing = gson.fromJson("{\"schemaVersion\":1,\"recent\":null,\"pinned\":[]}", CommandHistoryDocument.class);
        CommandHistoryDocument malformed = gson.fromJson("{\"schemaVersion\":1,\"recent\":[null],\"pinned\":[]}", CommandHistoryDocument.class);
        CommandHistoryDocument duplicate = new CommandHistoryDocument();
        duplicate.recent = List.of(new CommandEntry("home"));
        duplicate.pinned = List.of(new CommandEntry("home"));
        CommandHistoryDocument sensitive = new CommandHistoryDocument();
        sensitive.recent = List.of(new CommandEntry("LOGIN secret"));

        assertThrows(IllegalArgumentException.class, missing::validate);
        assertThrows(IllegalArgumentException.class, malformed::validate);
        assertThrows(IllegalArgumentException.class, duplicate::validate);
        assertThrows(IllegalArgumentException.class, sensitive::validate);
    }

    @Test
    void rejectsUnboundedPersistedLists() {
        CommandHistoryDocument document = new CommandHistoryDocument();
        document.pinned = IntStream.rangeClosed(0, CommandHistoryDocument.MAX_ENTRIES_PER_LIST)
                .mapToObj(index -> new CommandEntry("/say " + index))
                .toList();

        assertThrows(IllegalArgumentException.class, document::validate);
    }
}
