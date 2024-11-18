package com.jeka8833.tntserver.gamechat;

import com.jeka8833.tntserver.ServerType;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.gamechat.commands.Command;
import com.jeka8833.tntserver.gamechat.commands.ForceModuleCommand;
import com.jeka8833.tntserver.gamechat.commands.StopCommand;
import com.jeka8833.tntserver.packet.packets.ChatPacket;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class CommandManager {
    private static final Map<String, Command> COMMANDS = getCommands(new ForceModuleCommand(), new StopCommand());

    private static final UUID EMPTY_UUID = new UUID(0, 0);

    public static boolean executeCommand(User user, String text) {
        try {
            if (text.startsWith("@")) {
                WebSocket userWebsocket = user.getSocket();
                if (userWebsocket == null) return false;

                GameChatManager.sendToHook(user.uuid, null, ServerType.UNKNOWN, text);

                int commandEndIndex = text.indexOf(" ");
                if (commandEndIndex == -1) commandEndIndex = text.length();

                String commandName = text.substring(1, commandEndIndex).toLowerCase();
                Command command = COMMANDS.get(commandName);
                if (command != null) {
                    command.execute(user, userWebsocket, text.substring(commandEndIndex).strip());
                } else {
                    sendError(userWebsocket, "Unknown command");
                }

                return true;
            }
        } catch (Exception e) {
            log.error("Error while executing command", e);
        }

        return false;
    }

    private static Map<String, Command> getCommands(Command... commands) {
        Map<String, Command> commandsList = new HashMap<>();
        for (Command c : commands) {
            for (String name : c.getNames()) {
                commandsList.put(name, c);
            }
        }
        return commandsList;
    }

    public static void sendGood(@NotNull WebSocket socket, @NotNull String text) {
        TNTServer.serverSend(socket, new ChatPacket(EMPTY_UUID, "<Server> §a" + text));
    }

    public static void sendNeutral(@NotNull WebSocket socket, @NotNull String text) {
        TNTServer.serverSend(socket, new ChatPacket(EMPTY_UUID, "<Server> §7" + text));
    }

    public static void sendError(@NotNull WebSocket socket, @NotNull String text) {
        TNTServer.serverSend(socket, new ChatPacket(EMPTY_UUID, "<Server> §c" + text));
    }
}
