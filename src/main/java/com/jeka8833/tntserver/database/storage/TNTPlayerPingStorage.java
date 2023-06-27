package com.jeka8833.tntserver.database.storage;

import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.StreamSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;

public class TNTPlayerPingStorage implements Cloneable, StreamSerializer {
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int playerPing;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public int serverPing;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public int serverDownloadSpeed;
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int serverUploadSpeed;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public int jumpPing;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public int blockReactionTime;

    public TNTPlayerPingStorage() {
        this(0, 0, 0, 0, 0, 0);
    }

    public TNTPlayerPingStorage(int playerPing, int serverPing, int serverDownloadSpeed,
                                int serverUploadSpeed, int jumpPing, int blockReactionTime) {
        this.playerPing = playerPing;
        this.serverPing = serverPing;
        this.serverDownloadSpeed = serverDownloadSpeed;
        this.serverUploadSpeed = serverUploadSpeed;
        this.jumpPing = jumpPing;
        this.blockReactionTime = blockReactionTime;
    }

    @Override
    public void readStream(@NotNull PacketInputStream stream) throws IOException {
        playerPing = stream.readUnsignedShort();
        serverPing = stream.readUnsignedShort();
        serverDownloadSpeed = stream.readInt();
        serverUploadSpeed = stream.readInt();
        jumpPing = stream.readUnsignedShort();
        blockReactionTime = stream.readUnsignedShort();
    }

    @Override
    public void writeStream(@NotNull PacketOutputStream stream) throws IOException {
        stream.writeShort(playerPing);
        stream.writeShort(serverPing);
        stream.writeInt(serverDownloadSpeed);
        stream.writeInt(serverUploadSpeed);
        stream.writeShort(jumpPing);
        stream.writeShort(blockReactionTime);
    }

    @Override
    public String toString() {
        return "PlayerPing{" +
                "userPing=" + playerPing +
                ", serverPing=" + serverPing +
                ", serverDownloadSpeed=" + serverDownloadSpeed +
                ", serverUploadSpeed=" + serverUploadSpeed +
                ", jumpPing=" + jumpPing +
                ", blockReactionTime=" + blockReactionTime +
                '}';
    }

    @Override
    public TNTPlayerPingStorage clone() {
        try {
            return (TNTPlayerPingStorage) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
