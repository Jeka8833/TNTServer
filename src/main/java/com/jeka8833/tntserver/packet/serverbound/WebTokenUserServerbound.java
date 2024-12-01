package com.jeka8833.tntserver.packet.serverbound;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import org.jetbrains.annotations.NotNull;

public record WebTokenUserServerbound(boolean unregister) implements PacketBase {
    public WebTokenUserServerbound(@NotNull PacketInputSerializer serializer, int protocolVersion) {
        this(serializer.readBoolean());
    }

    @Override
    public void write(@NotNull PacketOutputSerializer serializer, int protocolVersion) {
        serializer.writeBoolean(unregister);
    }
}
