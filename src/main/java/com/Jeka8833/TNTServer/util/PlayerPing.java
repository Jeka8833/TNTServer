package com.Jeka8833.TNTServer.util;

import org.jetbrains.annotations.Range;

public class PlayerPing {
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int userPing;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public int serverPing;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public int jumpPing;

    @Range(from = 0, to = Integer.MAX_VALUE)
    public int blockReactionTime;

    public PlayerPing() {
        this(0, 0, 0, 0);
    }

    public PlayerPing(int userPing, int serverPing, int jumpPing, int blockReactionTime) {
        this.userPing = userPing;
        this.serverPing = serverPing;
        this.jumpPing = jumpPing;
        this.blockReactionTime = blockReactionTime;
    }

    @Override
    public String toString() {
        return "PlayerPing{userPing=" + userPing +
                ", serverPing=" + serverPing +
                ", jumpPing=" + jumpPing +
                ", blockReactionTime=" + blockReactionTime +
                '}';
    }
}
