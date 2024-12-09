package com.jeka8833.tntserver.old.database.analytics.jumpPakets;

import com.jeka8833.tntserver.old.database.analytics.AnalyticPacket;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class ReceivedJumpV2 implements AnalyticPacket {
    private long time;
    private int velX;
    private int velY;
    private int velZ;
    private double posX;
    private double posY;
    private double posZ;

    @Override
    public int getPacketID() {
        return 5;
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        this.time = stream.readLong();
        this.velX = stream.readInt();
        this.velY = stream.readInt();
        this.velZ = stream.readInt();
        this.posX = stream.readDouble();
        this.posY = stream.readDouble();
        this.posZ = stream.readDouble();
    }

    @Override
    public int getFilePacketID() {
        return 5;
    }

    @Override
    public void fileWrite(@NotNull DataOutputStream writer) throws IOException {
        writer.write(getFilePacketID());

        writer.writeLong(time);
        writer.writeInt(velX);
        writer.writeInt(velY);
        writer.writeInt(velZ);
        writer.writeDouble(posX);
        writer.writeDouble(posY);
        writer.writeDouble(posZ);
    }
}
