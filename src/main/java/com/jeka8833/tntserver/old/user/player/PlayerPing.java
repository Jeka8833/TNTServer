package com.jeka8833.tntserver.old.user.player;

public record PlayerPing(
        short playerPing,
        short serverPing,
        short jumpPing,
        short blockReactionTime) {

    public static final PlayerPing EMPTY = new PlayerPing((short) 0, (short) 0, (short) 0, (short) 0);
}
