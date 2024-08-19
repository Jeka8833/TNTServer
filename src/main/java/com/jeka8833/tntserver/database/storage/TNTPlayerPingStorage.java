package com.jeka8833.tntserver.database.storage;

import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;

import java.io.IOException;

public class TNTPlayerPingStorage {
    public final int playerPing;
    public final int serverPing;
    public final int jumpPing;
    public final int blockReactionTime;

    public TNTPlayerPingStorage() {
        this.playerPing = 0;
        this.serverPing = 0;
        this.jumpPing = 0;
        this.blockReactionTime = 0;
    }

    public TNTPlayerPingStorage(PacketInputStream stream) throws IOException {
        this.playerPing = stream.readUnsignedShort();
        this.serverPing = stream.readUnsignedShort();
        stream.skipNBytes(8);   // Download and upload speed
        this.jumpPing = stream.readUnsignedShort();
        this.blockReactionTime = stream.readUnsignedShort();
    }

    public void writeStream(PacketOutputStream stream) throws IOException {
        stream.writeShort(playerPing);
        stream.writeShort(serverPing);
        stream.writeInt(0);
        stream.writeInt(0);
        stream.writeShort(jumpPing);
        stream.writeShort(blockReactionTime);
    }
}
