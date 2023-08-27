package com.jeka8833.tntserver.packet.callback;

import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class PacketCallback implements Packet {
    private short uniqueID;

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeShort(uniqueID);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        uniqueID = stream.readShort();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        CallbackManager.callPacket(this);
    }

    public void setUniqueID(short uniqueID) {
        this.uniqueID = uniqueID;
    }

    public short getUniqueID() {
        return uniqueID;
    }
}
