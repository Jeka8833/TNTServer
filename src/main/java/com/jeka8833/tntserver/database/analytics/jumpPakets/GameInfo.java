package com.jeka8833.tntserver.database.analytics.jumpPakets;

import com.jeka8833.tntserver.database.analytics.AnalyticPacket;
import com.jeka8833.tntserver.packet.PacketInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class GameInfo implements AnalyticPacket {
    private long time;
    private String serverBrand;
    private String gameType;
    private String map;
    private String mode;

    @Override
    public int getPacketID() {
        return 1;
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        this.time = stream.readLong();
        this.serverBrand = stream.readUTF();
        this.gameType = stream.readUTF();
        this.map = stream.readUTF();
        this.mode = stream.readUTF();
    }

    @Override
    public int getFilePacketID() {
        return 1;
    }

    @Override
    public void fileWrite(@NotNull DataOutputStream writer) throws IOException {
        writer.write(getFilePacketID());

        writer.writeLong(time);
        writer.writeUTF(serverBrand);
        writer.writeUTF(gameType);
        writer.writeUTF(map);
        writer.writeUTF(mode);
    }
}
