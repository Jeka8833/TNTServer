package com.jeka8833.tntserver.services.tntclient.packet.clientbound.tntendpoint;

import com.jeka8833.tntserver.services.tntclient.packet.ClientBoundPacket;
import com.jeka8833.tntserver.services.tntclient.user.player.GameServer;
import com.jeka8833.toprotocol.addons.serializer.OptionalSerialize;
import com.jeka8833.toprotocol.addons.serializer.UUIDSerialize;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ClientboundBotChat implements ClientBoundPacket {
    private final @Nullable UUID sender;
    private final @Nullable UUID receiver;
    private final @NotNull GameServer server;
    private final @NotNull String message;

    public ClientboundBotChat(
            @Nullable UUID sender, @Nullable UUID receiver, @NotNull GameServer server, @NotNull String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.server = server;
        this.message = message;
    }

    public ClientboundBotChat(InputByteArray serializer, Integer protocolVersion) {
        this.sender = OptionalSerialize.readOptionally(serializer, UUIDSerialize::readUUID);
        this.receiver = OptionalSerialize.readOptionally(serializer, UUIDSerialize::readUUID);
        this.server = GameServer.findByServerBrand(serializer.readString());
        this.message = serializer.readString();
    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {
        OptionalSerialize.writeOptionally(serializer, sender, UUIDSerialize::writeUUID);
        OptionalSerialize.writeOptionally(serializer, receiver, UUIDSerialize::writeUUID);

        serializer
                .writeString(server.getServerBrands())
                .writeString(message);
    }

    public @Nullable UUID getSender() {
        return sender;
    }

    public @Nullable UUID getReceiver() {
        return receiver;
    }

    public @NotNull GameServer getServer() {
        return server;
    }

    public @NotNull String getMessage() {
        return message;
    }
}
