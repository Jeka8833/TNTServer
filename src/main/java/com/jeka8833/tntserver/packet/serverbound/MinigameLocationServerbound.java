package com.jeka8833.tntserver.packet.serverbound;

import com.jeka8833.toprotocol.core.packet.PacketBase;
import com.jeka8833.toprotocol.core.serializer.PacketInputSerializer;
import com.jeka8833.toprotocol.core.serializer.PacketOutputSerializer;
import org.jetbrains.annotations.NotNull;

public record MinigameLocationServerbound(String jsonLocation) implements PacketBase {
    public MinigameLocationServerbound(@NotNull PacketInputSerializer serializer, int protocolVersion) {

    }

    @Override
    public void write(@NotNull PacketOutputSerializer serializer, int protocolVersion) {
        PacketBase.super.write(serializer, protocolVersion);
    }
}
