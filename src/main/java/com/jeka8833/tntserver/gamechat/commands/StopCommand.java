package com.jeka8833.tntserver.gamechat.commands;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.gamechat.CommandManager;
import com.jeka8833.tntserver.requester.HypixelCache;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

public class StopCommand implements Command {
    private static final String PRIVILEGE = "SERVER_STOP";

    @Override
    public @NotNull String @NotNull [] getNames() {
        return new String[]{"stop"};
    }

    @Override
    public void execute(@NotNull User user, @NotNull WebSocket userSocket, @NotNull String text) {
        RemoteDB.readUserPrivileges(user.uuid, privilegesOptional -> {
            if (privilegesOptional.isEmpty()) {
                CommandManager.sendError(userSocket, "Privilege server is not available");
            } else if (privilegesOptional.get().contains(PRIVILEGE)) {
                try {
                    HypixelCache.storeToFile(Main.INSTANCE.cacheFile);
                    TNTServer.server.stop(10_000);
                    RemoteDB.saveAndClose();
                    System.exit(0);
                } catch (Exception e) {
                    CommandManager.sendError(userSocket, e.toString());
                }
            } else {
                CommandManager.sendError(userSocket, "You don't have permission");
            }
        });
    }
}
