package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;

public class BlockModulesPacket implements Packet {

    private final long block;
    private final long active;

    public BlockModulesPacket() {
        this(0, 0);
    }

    public BlockModulesPacket(final long block, long active) {
        this.block = block;
        this.active = active;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeLong(block);
        stream.writeLong(active);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        throw new NullPointerException("Fail read packet");
    }

    @Override
    public void serverProcess(WebSocket socket, TNTUser user) {
        throw new NullPointerException("Fail process packet");
    }
}
