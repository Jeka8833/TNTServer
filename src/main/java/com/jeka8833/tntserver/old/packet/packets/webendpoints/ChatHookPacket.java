package com.jeka8833.tntserver.old.packet.packets.webendpoints;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.database.PlayersDatabase;
import com.jeka8833.tntserver.old.gamechat.CommandManager;
import com.jeka8833.tntserver.old.gamechat.GameChatManager;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.services.tntclient.user.Bot;
import com.jeka8833.tntserver.services.tntclient.user.UserBase;
import com.jeka8833.tntserver.services.tntclient.user.player.GameServer;
import com.jeka8833.toprotocol.core.serializer.ArrayOutputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import com.jeka8833.toprotocol.core.serializer.StreamInputSerializer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

@NoArgsConstructor
public class ChatHookPacket implements Packet {
    private @Nullable UUID sender;
    private @Nullable UUID receiver;
    private GameServer server;
    private String message;
    private boolean isSystemMessage;

    public ChatHookPacket(@Nullable UUID sender, @Nullable UUID receiver,
                          @NotNull GameServer server, @NotNull String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.server = server;
        this.message = message;
        this.isSystemMessage = false;
    }

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        ArrayOutputSerializer packetOutputSerializer = new ArrayOutputSerializer(Integer.MAX_VALUE);

        packetOutputSerializer.writeOptionally(sender, PacketOutputSerializer::writeUUID);
        packetOutputSerializer.writeOptionally(receiver, PacketOutputSerializer::writeUUID);
        packetOutputSerializer.writeString(server.getServerBrands());
        packetOutputSerializer.writeString(message);
        //packetOutputSerializer.writeBoolean(isSystemMessage); // Asymmetric packet

        stream.write(packetOutputSerializer.array());
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        StreamInputSerializer streamInputSerializer = new StreamInputSerializer(stream);

        sender = streamInputSerializer.readOptionally(PacketInputSerializer::readUUID);
        receiver = streamInputSerializer.readOptionally(PacketInputSerializer::readUUID);
        server = GameServer.findByServerBrand(streamInputSerializer.readString());
        message = streamInputSerializer.readString();
        isSystemMessage = streamInputSerializer.readBoolean();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (user instanceof Bot bot && bot.hasPrivilege("SERVER_CHAT")) {
            if (sender != null) {
                if (CommandManager.executeCommand(PlayersDatabase.getOrCreate(sender), message)) return;
            }

            if (receiver != null) {
                GameChatManager.sendDirectMessage(sender, receiver, message, isSystemMessage);
            } else {
                GameChatManager.sendGlobalMessage(sender, this.server, message, isSystemMessage);
            }
        } else {
            user.disconnect();
        }
    }
}
