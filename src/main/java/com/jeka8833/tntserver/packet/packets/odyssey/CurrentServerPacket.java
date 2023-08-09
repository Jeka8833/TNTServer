package com.jeka8833.tntserver.packet.packets.odyssey;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class CurrentServerPacket implements Packet {

    private boolean isOdysseyServer;
    private @Nullable UUID playerUUID;

    public CurrentServerPacket() {
    }

    public CurrentServerPacket(@NotNull UUID playerUUID, boolean isOdysseyServer) {
        this.playerUUID = playerUUID;
        this.isOdysseyServer = isOdysseyServer;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (playerUUID == null) throw new NullPointerException("User is null");

        stream.writeUUID(playerUUID);
        stream.writeBoolean(isOdysseyServer);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        playerUUID = stream.readUUID();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable Player user) {
        if (!BotsManager.checkPrivilege(socket, "ODYSSEY_CHECK")) {
            socket.close();
            return;
        }

        Player player = PlayersDatabase.getUser(playerUUID);
        if (player == null) {
            Main.serverSend(socket, new CurrentServerPacket(playerUUID, false));
        } else {
            Main.serverSend(socket,
                    new CurrentServerPacket(player.uuid, player.serverType == ServerType.TNT_COMMUNITY));
        }
    }
}
