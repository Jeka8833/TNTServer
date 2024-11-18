package com.jeka8833.tntserver.packet.packets.webendpoints;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.WebTokenUseSidePacket;
import lombok.NoArgsConstructor;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

@NoArgsConstructor
public class WebTokenEndpointSidePacket implements Packet {
    private UUID user;
    private UUID key;
    private boolean register;

    public WebTokenEndpointSidePacket(UUID user, boolean register) {
        this.user = user;
        this.register = register;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(user);
        stream.writeBoolean(register);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        user = stream.readUUID();
        key = stream.readUUID();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (PlayersDatabase.isPrivilegeAbsent(user, "SERVER_TOKEN")) {
            socket.close();

            return;
        }

        User foundUser = PlayersDatabase.getOrCreate(this.user);

        WebSocket foundUserSocket = foundUser.getSocket();
        if (foundUserSocket != null) {
            TNTServer.serverSend(foundUserSocket, new WebTokenUseSidePacket(this.user, this.key));
        }
    }
}
