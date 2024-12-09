package com.jeka8833.tntserver.old.packet;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.user.UserBase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface Packet {
    void write(PacketOutputStream stream, int protocolVersion) throws IOException;

    void read(PacketInputStream stream, int protocolVersion) throws IOException;

    void serverProcess(@NotNull UserBase user, @NotNull TNTServer server);
}
