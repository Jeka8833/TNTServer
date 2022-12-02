package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;

public class PingPacket implements Packet {

    private long time;

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
    public void serverProcess(WebSocket socket, final TNTUser user) {
        Main.serverSend(socket, new PingPacket(time));
    }
}
