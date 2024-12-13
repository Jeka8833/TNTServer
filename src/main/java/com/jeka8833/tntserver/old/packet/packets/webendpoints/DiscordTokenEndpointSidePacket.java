package com.jeka8833.tntserver.old.packet.packets.webendpoints;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.services.tntclient.user.UserBase;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class DiscordTokenEndpointSidePacket implements Packet {
    private UUID player;
    private int code;

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        stream.writeUUID(player);
        stream.writeInt(code);
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        player = stream.readUUID();
        code = stream.readInt();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
    }
}
