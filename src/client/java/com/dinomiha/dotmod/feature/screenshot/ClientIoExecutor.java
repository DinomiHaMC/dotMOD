package com.dinomiha.dotmod.feature.screenshot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ClientIoExecutor {
    public static final ExecutorService INSTANCE = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "dotmod-client-io");
        thread.setDaemon(true);
        return thread;
    });

    private ClientIoExecutor() {
    }
}
