package com.jeka8833.tntserver.gamechat.commands;

import com.jeka8833.tntserver.database.User;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

public interface Command {
    @NotNull
    String @NotNull [] getNames();

    void execute(@NotNull User user, @NotNull WebSocket userSocket, @NotNull String text);
}
