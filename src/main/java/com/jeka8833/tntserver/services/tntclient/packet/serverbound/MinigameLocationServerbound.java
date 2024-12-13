package com.jeka8833.tntserver.services.tntclient.packet.serverbound;

import com.jeka8833.tntserver.services.tntclient.packet.ServerBoundPacket;
import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MinigameLocationServerbound implements ServerBoundPacket {
    private final String jsonLocation;

    public MinigameLocationServerbound(String jsonLocation) {
        this.jsonLocation = jsonLocation;
    }

    public MinigameLocationServerbound(@NotNull InputByteArray serializer, Integer protocolVersion) {

    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {

    }

    public String getJsonLocation() {
        return jsonLocation;
    }
}
