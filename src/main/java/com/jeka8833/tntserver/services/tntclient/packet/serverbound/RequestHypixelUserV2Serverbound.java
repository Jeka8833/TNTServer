package com.jeka8833.tntserver.services.tntclient.packet.serverbound;

import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public final class RequestHypixelUserV2Serverbound extends RequestHypixelUserServerbound {
    public RequestHypixelUserV2Serverbound(@NotNull Set<@NotNull UUID> players) {
        super(players);
    }

    public RequestHypixelUserV2Serverbound(@NotNull InputByteArray serializer, Integer protocolVersion) {
        super(serializer, protocolVersion);
    }
}
