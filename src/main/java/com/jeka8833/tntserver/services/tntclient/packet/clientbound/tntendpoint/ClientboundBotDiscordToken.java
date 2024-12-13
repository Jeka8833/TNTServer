package com.jeka8833.tntserver.services.tntclient.packet.clientbound.tntendpoint;

import com.jeka8833.tntserver.services.tntclient.packet.ClientBoundPacket;
import com.jeka8833.toprotocol.addons.serializer.UUIDSerialize;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ClientboundBotDiscordToken implements ClientBoundPacket {
    private final UUID player;
    private final int code;

    public ClientboundBotDiscordToken(UUID player, int code) {
        this.player = player;
        this.code = code;
    }

    public ClientboundBotDiscordToken(InputByteArray serializer, Integer protocolVersion) {
        this.player = UUIDSerialize.readUUID(serializer);
        this.code = serializer.readInt();
    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {
        UUIDSerialize.writeUUID(serializer, player);
        serializer.writeInt(code);
    }

    public UUID getPlayer() {
        return player;
    }

    public int getCode() {
        return code;
    }
}
