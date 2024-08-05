package com.jeka8833.tntserver.packet;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface StreamWriter {
    void writeStream(@NotNull PacketOutputStream stream) throws IOException;
}
