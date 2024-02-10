package com.jeka8833.tntserver.database.analytics;

import com.jeka8833.tntserver.packet.PacketInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public interface AnalyticPacket {
    int getPacketID();

    void read(PacketInputStream stream) throws IOException;

    int getFilePacketID();

    void fileWrite(@NotNull DataOutputStream writer) throws IOException;
}
