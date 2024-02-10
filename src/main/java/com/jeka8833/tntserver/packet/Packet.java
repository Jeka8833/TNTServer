package com.jeka8833.tntserver.packet;

import com.jeka8833.tntserver.database.User;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface Packet {
    void write(PacketOutputStream stream) throws IOException;

    void read(PacketInputStream stream) throws IOException;

    void serverProcess(WebSocket socket, @Nullable User user);
}
