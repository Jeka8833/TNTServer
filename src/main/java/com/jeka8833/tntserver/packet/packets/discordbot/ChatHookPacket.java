package com.jeka8833.tntserver.packet.packets.discordbot;

import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.database.Bot;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.gamechat.CommandManager;
import com.jeka8833.tntserver.gamechat.GameChatManager;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class ChatHookPacket implements Packet {
    private static final UUID EMPTY_UUID = new UUID(0, 0);

    private UUID sender;
    private UUID receiver;
    private String server;
    private String text;

    @SuppressWarnings("unused")
    public ChatHookPacket() {
    }

    public ChatHookPacket(@NotNull UUID sender, @Nullable UUID receiver,
                          @Nullable ServerType server, @NotNull String text) {
        this.sender = sender;
        this.receiver = receiver == null ? EMPTY_UUID : receiver;
        this.server = server == null ? "" : server.getActualServerBrand();
        this.text = text;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(sender);
        stream.writeUUID(receiver);
        stream.writeUTF(server);
        stream.writeUTF(text);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        sender = stream.readUUID();
        receiver = stream.readUUID();
        server = stream.readUTF();
        text = stream.readUTF();
    }

    @Nullable
    public UUID getReceiver() {
        if (receiver.equals(EMPTY_UUID)) return null;

        return receiver;
    }

    @NotNull
    public ServerType getServer() {
        return ServerType.getServer(server);
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (user instanceof Bot bot && bot.hasPrivilege("SERVER_CHAT")) {
            if (CommandManager.executeCommand(bot, text)) return;

            UUID receiver = getReceiver();
            if (receiver == null) {
                GameChatManager.sendGlobalMessage(sender, getServer(), text);
            } else {
                GameChatManager.sendDirectMessage(sender, receiver, text);
            }
        } else {
            socket.close();
        }
    }
}
