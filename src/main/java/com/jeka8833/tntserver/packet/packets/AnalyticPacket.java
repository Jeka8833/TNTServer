package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.analytics.AnalyticGroup;
import com.jeka8833.tntserver.database.analytics.AnalyticPacketLink;
import com.jeka8833.tntserver.database.analytics.jumpPakets.*;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.util.VersionUtil;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class AnalyticPacket implements Packet {
    private static final Map<Integer, AnalyticPacketLink> PACKETS_MAP = generatePacketsMap();

    private final List<AnalyticTempStorage> TEMP_STORAGE_LIST = new ArrayList<>();

    private static Map<Integer, AnalyticPacketLink> generatePacketsMap() {
        AnalyticGroup jumpGroup = new JumpAnalyticGroup();

        com.jeka8833.tntserver.database.analytics.AnalyticPacket callJump = new CallJump();
        com.jeka8833.tntserver.database.analytics.AnalyticPacket callJumpV2 = new CallJumpV2();
        com.jeka8833.tntserver.database.analytics.AnalyticPacket gameInfo = new GameInfo();
        com.jeka8833.tntserver.database.analytics.AnalyticPacket playerCamera = new PlayerCamera();
        com.jeka8833.tntserver.database.analytics.AnalyticPacket receivedJump = new ReceivedJump();
        com.jeka8833.tntserver.database.analytics.AnalyticPacket receivedJumpV2 = new ReceivedJumpV2();

        Map<Integer, AnalyticPacketLink> map = new HashMap<>();

        map.put(callJump.getPacketID(), new AnalyticPacketLink(jumpGroup, callJump.getClass()));
        map.put(callJumpV2.getPacketID(), new AnalyticPacketLink(jumpGroup, callJumpV2.getClass()));
        map.put(gameInfo.getPacketID(), new AnalyticPacketLink(jumpGroup, gameInfo.getClass()));
        map.put(playerCamera.getPacketID(), new AnalyticPacketLink(jumpGroup, playerCamera.getClass()));
        map.put(receivedJump.getPacketID(), new AnalyticPacketLink(jumpGroup, receivedJump.getClass()));
        map.put(receivedJumpV2.getPacketID(), new AnalyticPacketLink(jumpGroup, receivedJumpV2.getClass()));

        if (TNTServer.analyticManager != null) {
            TNTServer.analyticManager.addGroup(jumpGroup);
        }

        return map;
    }

    @Override
    public void write(PacketOutputStream stream) {
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        if (TNTServer.analyticManager == null || TNTServer.analyticManager.isDirectoryOverflow()) return;

        UUID randomID = stream.readUUID();

        int packetCount = stream.read();
        try {
            for (int i = 0; i < packetCount; i++) {
                int packetId = stream.readShort();

                AnalyticPacketLink analyticPacketLink = PACKETS_MAP.get(packetId);
                if (analyticPacketLink == null)
                    throw new IOException("The received packet has an unknown ID: " + packetId);

                com.jeka8833.tntserver.database.analytics.AnalyticPacket packet =
                        analyticPacketLink.packetClass().getDeclaredConstructor().newInstance();
                packet.read(stream);

                TEMP_STORAGE_LIST.add(new AnalyticTempStorage(analyticPacketLink.group(), randomID, packet));
            }
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (user instanceof Player player) {
            if (player.tntPlayerInfo == null || player.tntPlayerInfo.version == null) return;

            if (VersionUtil.compareVersions(player.tntPlayerInfo.version, "v1.3.16") < 0) {
                return;
            }

            for (AnalyticTempStorage storage : TEMP_STORAGE_LIST) {
                storage.group.addToProcess(player.tntPlayerInfo.version, storage.sessionID, storage.packet);
            }
        }
    }

    private record AnalyticTempStorage(AnalyticGroup group, UUID sessionID,
                                       com.jeka8833.tntserver.database.analytics.AnalyticPacket packet) {
    }
}
