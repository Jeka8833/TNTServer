package com.Jeka8833.TNTServer.packet;

import com.Jeka8833.TNTServer.TNTUser;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface Packet {

    void write(PacketOutputStream stream) throws IOException;

    void read(PacketInputStream stream) throws IOException;

    void serverProcess(WebSocket socket, @Nullable TNTUser user);
}
