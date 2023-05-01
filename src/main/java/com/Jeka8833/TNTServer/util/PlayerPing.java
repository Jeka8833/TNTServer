package com.Jeka8833.TNTServer.util;

import org.jetbrains.annotations.Range;

public class PlayerPing {
    @Range(from = 0, to = Integer.MAX_VALUE)
    public final int userPing;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public final int serverPing;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public final int serverDownloadSpeed;
    @Range(from = 0, to = Integer.MAX_VALUE)
    public final int serverUploadSpeed;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public final int jumpPing;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public final int blockReactionTime;

    public PlayerPing() {
        this(0, 0, 0, 0, 0, 0);
    }

    public PlayerPing(int userPing, int serverPing, int serverDownloadSpeed, int serverUploadSpeed, int jumpPing, int blockReactionTime) {
        this.userPing = userPing;
        this.serverPing = serverPing;
        this.serverDownloadSpeed = serverDownloadSpeed;
        this.serverUploadSpeed = serverUploadSpeed;
        this.jumpPing = jumpPing;
        this.blockReactionTime = blockReactionTime;
    }

    @Override
    public String toString() {
        return "PlayerPing{" +
                "userPing=" + userPing +
                ", serverPing=" + serverPing +
                ", serverDownloadSpeed=" + serverDownloadSpeed +
                ", serverUploadSpeed=" + serverUploadSpeed +
                ", jumpPing=" + jumpPing +
                ", blockReactionTime=" + blockReactionTime +
                '}';
    }
}
