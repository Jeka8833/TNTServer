package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.user.UserBase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public record ReceiveTNTClientPlayerPacket(Player[] users) implements Packet {
    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        stream.writeByte(users.length);
        for (Player user : users) {
            stream.writeUUID(user.uuid);

            boolean inTNTDatabase = user.tntPlayerInfo != null;
            stream.writeBoolean(inTNTDatabase);
            if (inTNTDatabase) {
                boolean isOffline = user.tntPlayerInfo.status == TNTPlayerStorage.STATUS_OFFLINE ||
                        user.tntPlayerInfo.status == TNTPlayerStorage.STATUS_INVISIBLE;

                long isDJFix = user.tntPlayerInfo.activeModules & (1L << 6L | 1L << 29L | 1L << 30L);

                stream.writeByte(user.tntPlayerInfo.donate);

                stream.writeByte(isOffline ? TNTPlayerStorage.STATUS_OFFLINE : user.tntPlayerInfo.status);

                stream.writeLong(isDJFix);

                stream.writeUTF(Objects.requireNonNullElse(user.tntPlayerInfo.version, ""));

                stream.writeUTF(Objects.requireNonNullElse(
                        isOffline ? null : user.tntPlayerInfo.gameInfo, ""));
            }
        }
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) {
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        user.disconnect();
    }
}
