package com.jeka8833.tntserver.services.tntclient.packet.serverbound;

import com.jeka8833.tntserver.services.tntclient.packet.ServerBoundPacket;
import com.jeka8833.toprotocol.addons.serializer.StringSerialize;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

public final class MinigameLocationServerbound implements ServerBoundPacket {
    private final String jsonLocation;

    public MinigameLocationServerbound(String jsonLocation) {
        this.jsonLocation = jsonLocation;
    }

    public MinigameLocationServerbound(@NotNull InputByteArray serializer, Integer protocolVersion) {
        this.jsonLocation = StringSerialize.readModifiedUTF8(serializer);
    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {
        StringSerialize.writeModifiedUTF8(serializer, jsonLocation);
    }

    public String getJsonLocation() {
        return jsonLocation;
    }
}
