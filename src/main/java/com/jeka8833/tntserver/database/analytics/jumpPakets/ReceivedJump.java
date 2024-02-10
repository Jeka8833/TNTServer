package com.jeka8833.tntserver.database.analytics.jumpPakets;

import com.jeka8833.tntserver.database.analytics.AnalyticPacket;
import com.jeka8833.tntserver.packet.PacketInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class ReceivedJump implements AnalyticPacket {
    private long time;
    private int velX;
    private int velY;
    private int velZ;

    @Override
    public int getPacketID() {
        return 3;
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        this.time = stream.readLong();
        this.velX = stream.readInt();
        this.velY = stream.readInt();
        this.velZ = stream.readInt();
    }

    @Override
    public int getFilePacketID() {
        return 3;
    }

    @Override
    public void fileWrite(@NotNull DataOutputStream writer) throws IOException {
        writer.write(getFilePacketID());

        writer.writeLong(time);
        writer.writeInt(velX);
        writer.writeInt(velY);
        writer.writeInt(velZ);
    }
}
