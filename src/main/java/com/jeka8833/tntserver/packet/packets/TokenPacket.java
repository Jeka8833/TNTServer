package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Bot;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.web.TokenGeneratorPacket;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.UUID;

public class TokenPacket implements Packet {
    private static final UUID NULL_UUID = new UUID(0, 0);

    private final UUID user;
    private final UUID key;

    private boolean unregister;

    @SuppressWarnings("unused")
    public TokenPacket() {
        this(null, null);
    }

    public TokenPacket(UUID user, UUID key) {
        this.user = user;
        this.key = key;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(user);
        stream.writeUUID(key);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        unregister = stream.readBoolean();
    }

    @Override
    public void serverProcess(WebSocket socket, User user) {
        if (user instanceof Player player) {
            Bot serverTokenizer = PlayersDatabase.getBotWithPrivilege("SERVER_TOKEN");
            if (serverTokenizer == null) {
                Main.serverSend(socket, new TokenPacket(NULL_UUID, NULL_UUID));
                return;
            }

            WebSocket serverTokenizerSocket = serverTokenizer.getSocket();
            if (serverTokenizerSocket == null) {
                Main.serverSend(socket, new TokenPacket(NULL_UUID, NULL_UUID));
                return;
            }

            Main.serverSend(serverTokenizerSocket, new TokenGeneratorPacket(player.uuid, unregister));
        } else {
            socket.close();
        }
    }
}
