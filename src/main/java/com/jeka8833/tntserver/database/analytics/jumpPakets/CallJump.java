package com.jeka8833.tntserver.database.analytics.jumpPakets;

import com.jeka8833.tntserver.database.analytics.AnalyticPacket;
import com.jeka8833.tntserver.packet.PacketInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class CallJump implements AnalyticPacket {
    private long time;
    private boolean useFeather;

    @Override
    public int getPacketID() {
        return 0;
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        this.time = stream.readLong();
        this.useFeather = stream.readBoolean();
    }

    @Override
    public int getFilePacketID() {
        return 0;
    }

    @Override
    public void fileWrite(@NotNull DataOutputStream writer) throws IOException {
        writer.write(getFilePacketID());

        writer.writeLong(time);
        writer.writeBoolean(useFeather);
    }
}
