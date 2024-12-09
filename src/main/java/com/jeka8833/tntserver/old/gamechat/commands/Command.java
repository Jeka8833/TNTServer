package com.jeka8833.tntserver.old.gamechat.commands;

import com.jeka8833.tntserver.old.database.storage.User;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

public interface Command {
    @NotNull
    String @NotNull [] getNames();

    void execute(@NotNull User user, @NotNull WebSocket userSocket, @NotNull String text);
}
