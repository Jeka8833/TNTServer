package com.jeka8833.tntserver.packet.serverbound;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import org.jetbrains.annotations.NotNull;

public record HypixelRequesterStatsServerbound(int count) implements PacketBase {
    public HypixelRequesterStatsServerbound(@NotNull PacketInputSerializer serializer, int protocolVersion) {
        this(serializer.readInt());
    }

    @Override
    public void write(@NotNull PacketOutputSerializer serializer, int protocolVersion) {
        serializer.writeInt(count);
    }
}
