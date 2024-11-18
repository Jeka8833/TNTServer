package com.jeka8833.tntserver.packet.packets.webendpoints;

import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class DiscordTokenEndpointSidePacket implements Packet {
    private UUID player;
    private int code;

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(player);
        stream.writeInt(code);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        player = stream.readUUID();
        code = stream.readInt();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
    }
}
