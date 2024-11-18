package com.jeka8833.tntserver.packet.packets.webendpoints;

import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.Bot;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.gamechat.CommandManager;
import com.jeka8833.tntserver.gamechat.GameChatManager;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.toprotocol.core.serializer.ArrayOutputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import com.jeka8833.toprotocol.core.serializer.StreamInputSerializer;
import lombok.NoArgsConstructor;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

@NoArgsConstructor
public class ChatHookPacket implements Packet {
    private @Nullable UUID sender;
    private @Nullable UUID receiver;
    private ServerType server;
    private String message;
    private boolean isSystemMessage;

    public ChatHookPacket(@Nullable UUID sender, @Nullable UUID receiver,
                          @NotNull ServerType server, @NotNull String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.server = server;
        this.message = message;
        this.isSystemMessage = false;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        ArrayOutputSerializer packetOutputSerializer = new ArrayOutputSerializer(Integer.MAX_VALUE);

        packetOutputSerializer.writeOptionally(sender, PacketOutputSerializer::writeUUID);
        packetOutputSerializer.writeOptionally(receiver, PacketOutputSerializer::writeUUID);
        packetOutputSerializer.writeString(server.getActualServerBrand());
        packetOutputSerializer.writeString(message);
        //packetOutputSerializer.writeBoolean(isSystemMessage); // Asymmetric packet

        stream.write(packetOutputSerializer.array());
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        StreamInputSerializer streamInputSerializer = new StreamInputSerializer(stream);

        sender = streamInputSerializer.readOptionally(PacketInputSerializer::readUUID);
        receiver = streamInputSerializer.readOptionally(PacketInputSerializer::readUUID);
        server = ServerType.getServer(streamInputSerializer.readString());
        message = streamInputSerializer.readString();
        isSystemMessage = streamInputSerializer.readBoolean();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (user instanceof Bot bot && bot.hasPrivilege("SERVER_CHAT")) {
            if (sender != null) {
                if (CommandManager.executeCommand(PlayersDatabase.getOrCreate(sender), message)) return;
            }

            if (receiver != null) {
                GameChatManager.sendDirectMessage(sender, receiver, message, isSystemMessage);
            } else {
                GameChatManager.sendGlobalMessage(sender, server, message, isSystemMessage);
            }
        } else {
            socket.close();
        }
    }
}
