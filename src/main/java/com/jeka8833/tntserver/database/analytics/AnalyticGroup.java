package com.jeka8833.tntserver.database.analytics;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public interface AnalyticGroup {
    void addManager(AnalyticManager analyticManager);

    void addToProcess(String clientVersion, UUID sessionID, AnalyticPacket packet);

    void process(@NotNull Path basePath) throws IOException, InterruptedException;

    void clear();
}
