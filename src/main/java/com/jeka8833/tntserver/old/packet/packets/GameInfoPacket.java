package com.jeka8833.tntserver.old.packet.packets;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.services.tntclient.user.UserBase;
import com.jeka8833.tntserver.services.tntclient.user.player.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class GameInfoPacket implements Packet {
    private String gameInfo;

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) {
        throw new NullPointerException("Fail read packet");
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        gameInfo = stream.readUTF();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (user instanceof Player player) {
            player.trySetMinigameLocation(gameInfo);
        } else {
            user.disconnect();
        }
    }
}