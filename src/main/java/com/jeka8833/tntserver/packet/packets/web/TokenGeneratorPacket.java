package com.jeka8833.tntserver.packet.packets.web;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
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
    public void serverProcess(WebSocket socket, @Nullable Player user) {
        if (this.user == null || this.key == null) throw new NullPointerException("user or key is null");

        if (!BotsManager.checkPrivilege(socket, "SERVER_TOKEN")) {
            socket.close();
            return;
        }

        for (WebSocket playerSocket : Main.server.getConnections()) {
            if (this.user.equals(playerSocket.getAttachment())) {
                Main.serverSend(playerSocket, new TokenPacket(this.user, this.key));

                return;
            }
        }
    }
}
