package com.jeka8833.tntserver.database.analytics.jumpPakets;

import com.jeka8833.tntserver.database.analytics.AnalyticPacket;
import com.jeka8833.tntserver.packet.PacketInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class CallJumpV2 implements AnalyticPacket {
    private long time;
    private boolean useFeather;
    private double posX;
    private double posY;
    private double posZ;
    private float lastReportedPitch;
    private float lastReportedYaw;

    @Override
    public int getPacketID() {
        return 4;
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        this.time = stream.readLong();
        this.useFeather = stream.readBoolean();
        this.posX = stream.readDouble();
        this.posY = stream.readDouble();
        this.posZ = stream.readDouble();
        this.lastReportedPitch = stream.readFloat();
        this.lastReportedYaw = stream.readFloat();
    }

    @Override
    public int getFilePacketID() {
        return 4;
    }

    @Override
    public void fileWrite(@NotNull DataOutputStream writer) throws IOException {
        writer.write(getFilePacketID());

        writer.writeLong(time);
        writer.writeBoolean(useFeather);
        writer.writeDouble(posX);
        writer.writeDouble(posY);
        writer.writeDouble(posZ);
        writer.writeFloat(lastReportedPitch);
        writer.writeFloat(lastReportedYaw);
    }
}
