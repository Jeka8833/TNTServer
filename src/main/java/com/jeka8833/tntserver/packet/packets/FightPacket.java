package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class FightPacket implements Packet {

    private int playerFight = 0;

    private final Collection<WebSocket> activeConnection;

    @SuppressWarnings("unused")
    public FightPacket() {
        this(null);
    }

    public FightPacket(Collection<WebSocket> activeConnection) {
        this.activeConnection = activeConnection;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        final List<Player> users = activeConnection.stream()
                .map(webSocket -> {
                    UUID user = webSocket.getAttachment();
                    if (user == null) return null;
                    return PlayersDatabase.getUser(user);
                })
                .filter(tntUser -> tntUser != null && tntUser.tntPlayerInfo != null && tntUser.tntPlayerInfo.fight != 0)
                .toList();

        stream.writeByte(users.size());
        for (Player user : users) {
            stream.writeUUID(user.uuid);
            stream.writeInt(user.tntPlayerInfo == null ? 0 : user.tntPlayerInfo.fight);
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        playerFight = stream.readInt();
    }

    @Override
    public void serverProcess(WebSocket socket, Player user) {
        if (user == null && !BotsManager.checkPrivilege(socket, "FIGHT_LIST")) {
            socket.close();
            return;
        }

        if (user != null && user.tntPlayerInfo != null) user.tntPlayerInfo.fight = playerFight;
        Main.serverSend(socket, new FightPacket(Main.server.getConnections()));
    }
}
