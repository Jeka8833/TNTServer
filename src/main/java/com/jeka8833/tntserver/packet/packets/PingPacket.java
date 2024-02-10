package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;

public class PingPacket implements Packet {
    private long time;

    @SuppressWarnings("unused")
    public PingPacket() {
        this(0);
    }

    public PingPacket(final long time) {
        this.time = time;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeLong(time);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        time = stream.readLong();
    }

    @Override
    public void serverProcess(WebSocket socket, final User user) {
        Main.serverSend(socket, new PingPacket(time));
    }
}