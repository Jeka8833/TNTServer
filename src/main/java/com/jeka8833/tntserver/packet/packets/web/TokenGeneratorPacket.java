package com.jeka8833.tntserver.packet.packets.web;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.TokenPacket;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class TokenGeneratorPacket implements Packet {
    private @Nullable UUID user;
    private UUID key;
    private boolean unregister;

    @SuppressWarnings("unused")
    public TokenGeneratorPacket() {
    }

    public TokenGeneratorPacket(@NotNull UUID user, boolean unregister) {
        this.user = user;
        this.unregister = unregister;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (this.user == null) throw new IllegalArgumentException("user or key is null");

        stream.writeUUID(user);
        stream.writeBoolean(unregister);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        user = stream.readUUID();
        key = stream.readUUID();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (this.user == null || this.key == null) throw new NullPointerException("user or key is null");

        if (BotsManager.isAbsent(user, "SERVER_TOKEN")) {
            socket.close();
            return;
        }

        User foundUser = PlayersDatabase.getOrCreate(this.user);

        WebSocket foundUserSocket = foundUser.getSocket();
        if (foundUserSocket != null) TNTServer.serverSend(foundUserSocket, new TokenPacket(this.user, this.key));
    }
}
