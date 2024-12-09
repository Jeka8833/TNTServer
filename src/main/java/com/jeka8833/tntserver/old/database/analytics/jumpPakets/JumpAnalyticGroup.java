package com.jeka8833.tntserver.old.database.analytics.jumpPakets;


import com.jeka8833.tntserver.old.database.analytics.AnalyticGroup;
import com.jeka8833.tntserver.old.database.analytics.AnalyticManager;
import com.jeka8833.tntserver.old.database.analytics.AnalyticPacket;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JumpAnalyticGroup implements AnalyticGroup {
    private static final byte[] HEADER = {'T', 'C', '_', 'J', 'I'};
    private static final String FOLDER_NAME = "jumpInfoV4";

    private final Map<UUID, Queue<AnalyticPacket>> packets = new ConcurrentHashMap<>();
    private AnalyticManager analyticManager;

    @Override
    public void addManager(AnalyticManager analyticManager) {
        this.analyticManager = analyticManager;
    }

    @Override
    public void addToProcess(String version, UUID sessionID, AnalyticPacket packet) {
        if (analyticManager == null || analyticManager.isDirectoryOverflow()) return;

        packets.computeIfAbsent(sessionID, uuid -> new ConcurrentLinkedQueue<>())
                .add(packet);
    }

    @Override
    public void process(@NotNull Path basePath) throws IOException {
        Path folderPath = basePath.resolve(FOLDER_NAME);
        if (!Files.isDirectory(folderPath)) {
            Files.createDirectories(folderPath);
        }

        for (UUID sessionID : packets.keySet()) {
            Path filePath = folderPath.resolve(sessionID + ".data");
            boolean fileExists = Files.isRegularFile(filePath);

            try (DataOutputStream writer = new DataOutputStream(Files.newOutputStream(filePath,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                if (!fileExists) {
                    writer.write(HEADER);
                    writer.writeLong(System.currentTimeMillis());
                }

                Queue<AnalyticPacket> queue = packets.remove(sessionID);

                AnalyticPacket packet;
                while ((packet = queue.poll()) != null) {
                    packet.fileWrite(writer);
                }

                writer.flush();
            }
        }
    }

    @Override
    public void clear() {
        packets.clear();
    }
}
