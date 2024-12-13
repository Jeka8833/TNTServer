package com.jeka8833.tntserver.services.tntclient.packet.serverbound;

import com.jeka8833.tntserver.services.tntclient.packet.ServerBoundPacket;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

public final class HypixelRequesterStatsServerbound implements ServerBoundPacket {
    private final int count;

    public HypixelRequesterStatsServerbound(int count) {
        this.count = count;
    }

    public HypixelRequesterStatsServerbound(@NotNull InputByteArray serializer, Integer protocolVersion) {
        this.count = serializer.readInt();
    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {
        serializer.writeInt(count);
    }

    public int getCount() {
        return count;
    }
}
