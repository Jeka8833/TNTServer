package com.jeka8833.tntserver.services.tntclient.packet.clientbound.tntendpoint;

import com.jeka8833.tntserver.services.tntclient.packet.ClientBoundPacket;
import com.jeka8833.toprotocol.addons.serializer.UUIDSerialize;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ClientboundBotWebToken implements ClientBoundPacket {
    private final UUID player;
    private final boolean register;

    public ClientboundBotWebToken(UUID player, boolean register) {
        this.player = player;
        this.register = register;
    }

    public ClientboundBotWebToken(InputByteArray serializer, Integer protocolVersion) {
        this.player = UUIDSerialize.readUUID(serializer);
        this.register = serializer.readBoolean();
    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {
        UUIDSerialize.writeUUID(serializer, player);
        serializer.writeBoolean(register);
    }

    public UUID getPlayer() {
        return player;
    }

    public boolean isRegister() {
        return register;
    }
}
