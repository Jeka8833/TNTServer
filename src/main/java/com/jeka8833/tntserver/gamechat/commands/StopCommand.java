package com.jeka8833.tntserver.gamechat.commands;

import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.gamechat.CommandManager;
import com.jeka8833.tntserver.gamechat.GameChatManager;
import com.jeka8833.tntserver.packet.packets.discordbot.ChatHookPacket;
import com.jeka8833.tntserver.requester.HypixelCache;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopCommand implements Command {
    private static final String PRIVILEGE = "SERVER_STOP";

    private static final Logger LOGGER = LoggerFactory.getLogger(StopCommand.class);

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
                    GameChatManager.sendGlobalMessage(ChatHookPacket.EMPTY_UUID, ServerType.UNKNOWN,
                            "§7(TNTClient Server) Server is shutting down...");
                    HypixelCache.storeToFile(Main.INSTANCE.cacheFile);
                    LOGGER.info("[Shutdown] Cache file stored");
                    TNTServer.server.stop(10_000);
                    LOGGER.info("[Shutdown] Server stopped");
                    RemoteDB.saveAndClose();
                    LOGGER.info("[Shutdown] Database closed");
                    System.exit(0);
                } catch (Exception e) {
                    LOGGER.error("Failed to stop server", e);

                    CommandManager.sendError(userSocket, e.toString());
                }
            } else {
                CommandManager.sendError(userSocket, "You don't have permission");
            }
        });
    }
}
