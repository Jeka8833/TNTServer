package com.jeka8833.tntserver.packet.packets.web;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.callback.PacketCallback;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class RolePacket extends PacketCallback {

    private @Nullable UUID playerUUID;
    private @Nullable String roles;

    @SuppressWarnings("unused")
    public RolePacket() {
    }

    public RolePacket(@NotNull UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (playerUUID == null) throw new NullPointerException("Player is null");

        super.write(stream);

        stream.writeUUID(playerUUID);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        super.read(stream);

        roles = stream.readUTF();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (BotsManager.isAbsent(user, "SERVER_ROLES")) {
            socket.close();
            return;
        }

        super.serverProcess(socket, user);
    }

    @Nullable
    public String getRoles() {
        return roles;
    }
}
