package com.jeka8833.tntserver.database.analytics.jumpPakets;

import com.jeka8833.tntserver.database.analytics.AnalyticPacket;
import com.jeka8833.tntserver.packet.PacketInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class PlayerCamera implements AnalyticPacket {
    private long time;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;
    private boolean onGround;

    @Override
    public int getPacketID() {
        return 2;
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        this.time = stream.readLong();
        this.x = stream.readDouble();
        this.y = stream.readDouble();
        this.z = stream.readDouble();
        this.pitch = stream.readFloat();
        this.yaw = stream.readFloat();
        this.onGround = stream.readBoolean();
    }

    @Override
    public int getFilePacketID() {
        return 2;
    }

    @Override
    public void fileWrite(@NotNull DataOutputStream writer) throws IOException {
        writer.write(getFilePacketID());

        writer.writeLong(time);
        writer.writeDouble(x);
        writer.writeDouble(y);
        writer.writeDouble(z);
        writer.writeFloat(pitch);
        writer.writeFloat(yaw);
        writer.writeBoolean(onGround);
    }
}
