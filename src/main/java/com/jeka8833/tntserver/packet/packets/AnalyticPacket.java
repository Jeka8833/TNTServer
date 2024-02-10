package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.analytics.*;
import com.jeka8833.tntserver.database.analytics.jumpPakets.*;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnalyticPacket implements Packet {
    private static final Map<Integer, AnalyticPacketLink> PACKETS_MAP = generatePacketsMap();

    @Override
    public void write(PacketOutputStream stream) throws IOException {
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        if (Main.analyticManager.isDirectoryOverflow()) return;

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

                analyticPacketLink.group().addToProcess(randomID, packet);
            }
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
    }

    private static Map<Integer, AnalyticPacketLink> generatePacketsMap() {
        AnalyticGroup jumpGroup = new JumpAnalyticGroup();

        com.jeka8833.tntserver.database.analytics.AnalyticPacket callJump = new CallJump();
        com.jeka8833.tntserver.database.analytics.AnalyticPacket gameInfo = new GameInfo();
        com.jeka8833.tntserver.database.analytics.AnalyticPacket playerCamera = new PlayerCamera();
        com.jeka8833.tntserver.database.analytics.AnalyticPacket receivedJump = new ReceivedJump();

        Map<Integer, AnalyticPacketLink> map = new HashMap<>();

        map.put(callJump.getPacketID(), new AnalyticPacketLink(jumpGroup, callJump.getClass()));
        map.put(gameInfo.getPacketID(), new AnalyticPacketLink(jumpGroup, gameInfo.getClass()));
        map.put(playerCamera.getPacketID(), new AnalyticPacketLink(jumpGroup, playerCamera.getClass()));
        map.put(receivedJump.getPacketID(), new AnalyticPacketLink(jumpGroup, receivedJump.getClass()));

        Main.analyticManager.addGroup(jumpGroup);

        return map;
    }
}
