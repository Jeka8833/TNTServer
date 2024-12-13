package com.jeka8833.tntserver.services.tntclient.packet.serverbound;

import com.jeka8833.tntserver.services.tntclient.packet.ServerBoundPacket;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

public final class WebTokenUserServerbound implements ServerBoundPacket {
    private final boolean unregister;

    public WebTokenUserServerbound(boolean unregister) {
        this.unregister = unregister;
    }

    public WebTokenUserServerbound(@NotNull InputByteArray serializer, Integer protocolVersion) {
        this(serializer.readBoolean());
    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {
        serializer.writeBoolean(unregister);
    }

    public boolean isUnregister() {
        return unregister;
    }
}
