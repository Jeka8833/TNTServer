package com.jeka8833.tntserver.packet.packets.discordbot;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.storage.Bot;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class LinkCodePacket implements Packet {
    public static final int INTERNAL_ERROR = 0;
    public static final int CONNECTION_ERROR = 1;
    public static final int MESSAGE_GOOD_LINK = 2;
    public static final int MESSAGE_BAD_LINK = 3;
    public static final int MESSAGE_GOOD_UNLINK = 4;
    public static final int MESSAGE_BAD_UNLINK = 5;
    public static final int TRY_LINK = 6;
    public static final int TRY_UNLINK = 7;

    private UUID user;
    private int code;
    private int statusCode;

    @SuppressWarnings("unused")
    public LinkCodePacket() {
        this(null, Integer.MIN_VALUE, INTERNAL_ERROR);
    }

    public LinkCodePacket(UUID user, int code, int statusCode) {
        this.user = user;
        this.code = code;
        this.statusCode = statusCode;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(user);
        stream.writeInt(code);
        stream.writeInt(statusCode);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        user = stream.readUUID();
        code = stream.readInt();
        statusCode = stream.readInt();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (user instanceof Player player) {
            Bot serverTokenizer = PlayersDatabase.getBotWithPrivilege("SERVER_DISCORD_LINK");
            if (serverTokenizer == null) {
                TNTServer.serverSend(socket, new LinkCodePacket(player.uuid, code, CONNECTION_ERROR));
                return;
            }

            WebSocket serverTokenizerSocket = serverTokenizer.getSocket();
            if (serverTokenizerSocket == null) {
                TNTServer.serverSend(socket, new LinkCodePacket(player.uuid, code, CONNECTION_ERROR));
                return;
            }

            TNTServer.serverSend(serverTokenizerSocket, new LinkCodePacket(player.uuid, code, statusCode));
        } else if (user instanceof Bot bot && bot.hasPrivilege("SERVER_DISCORD_LINK")) {
            User toUser = PlayersDatabase.getUser(this.user);
            if (toUser == null) return;

            WebSocket toUserSocket = toUser.getSocket();
            if (toUserSocket == null) return;

            TNTServer.serverSend(toUserSocket, new LinkCodePacket(this.user, code, statusCode));
        } else {
            socket.close();
        }
    }
}
