package com.jeka8833.tntserver.gamechat.commands;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.database.storage.Bot;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.gamechat.CommandManager;
import com.jeka8833.tntserver.mojang.MojangAPI;
import com.jeka8833.tntserver.packet.packets.BlockModulesPacket;
import org.intellij.lang.annotations.MagicConstant;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ForceModuleCommand implements Command {
    private static final Map<String, Integer> MODULES = Map.of(
            "djfix", 6
    );

    private static final String PRIVILEGE = "FORCE_MODULE";


    private static final int OPERATION_ON = 0;
    private static final int OPERATION_RESET = 1;
    private static final int OPERATION_OFF = 2;
    private static final int OPERATION_STATUS = 3;

    private static void step0(@NotNull WebSocket userWebsocket, @NotNull String[] args) {
        if (args.length > 0 && args[0].equals("on")) {
            step1(userWebsocket, args, OPERATION_ON);
        } else if (args.length > 0 && args[0].equals("off")) {
            step1(userWebsocket, args, OPERATION_OFF);
        } else if (args.length > 0 && args[0].equals("reset")) {
            step1(userWebsocket, args, OPERATION_RESET);
        } else if (args.length > 0 && args[0].equals("status")) {
            step1(userWebsocket, args, OPERATION_STATUS);
        } else {
            CommandManager.sendError(userWebsocket, """
                    Force module help:
                    Usage: @fm <operation> <module> <player>
                    Operations:
                      - on: Enable a module for a player.
                      - off: Disable a module for a player.
                      - reset: Reset the module status for a player.
                      - status: Check the module status for a player.
                    
                    Available modules: djfix
                    
                    Example: @fm on djfix jeka8833""");
        }
    }

    private static void step1(@NotNull WebSocket userWebsocket, @NotNull String[] args,
                              @MagicConstant(intValues = {OPERATION_ON, OPERATION_RESET, OPERATION_OFF, OPERATION_STATUS})
                              int operation) {
        StringBuffer buffer = new StringBuffer();
        if (args.length > 1) {
            Integer module = MODULES.get(args[1]);
            if (module != null) {
                step2(userWebsocket, args, operation, module);
                return;
            } else {
                buffer.append("Incorrect module name.\n");
            }
        } else {
            buffer.append("Module name is required.\n");
        }
        buffer.append("Available modules:\n");
        MODULES.forEach((name, value) -> buffer.append("> ").append(name).append('\n'));
        CommandManager.sendError(userWebsocket, buffer.toString());
    }

    private static void step2(@NotNull WebSocket userWebsocket, @NotNull String[] args,
                              @MagicConstant(intValues = {OPERATION_ON, OPERATION_RESET, OPERATION_OFF, OPERATION_STATUS})
                              int operation, int module) {
        if (args.length > 2) {
            getPlayerByName(args[2], optionalPlayer -> {
                if (optionalPlayer.isPresent()) {
                    Player player = optionalPlayer.get();
                    if (operation == OPERATION_STATUS && player.tntPlayerInfo != null) {
                        CommandManager.sendGood(userWebsocket, args[2] + "'s status:\n" +
                                "Force block: " + ((player.tntPlayerInfo.forceBlock & (1L << module)) != 0) + "\n" +
                                "Force active: " + ((player.tntPlayerInfo.forceActive & (1L << module)) != 0) + "\n" +
                                "Current module state: " + ((player.tntPlayerInfo.activeModules & (1L << module)) != 0));
                    } else if (setStatus(operation, module, player)) {
                        CommandManager.sendGood(userWebsocket, "Success");
                    } else {
                        CommandManager.sendError(userWebsocket,
                                "The player is not on the server and is not in the database.");
                    }
                } else {
                    CommandManager.sendError(userWebsocket, "Incorrect player name or Mojang API is down.");
                }
            });
        } else {
            CommandManager.sendError(userWebsocket, "Player name is required");
        }
    }

    private static void getPlayerByName(@NotNull String name,
                                        @NotNull Consumer<@NotNull Optional<@NotNull Player>> listener) {
        MojangAPI.getUUID(name, mojangProfile -> {
            if (mojangProfile.getUuid().isPresent()) {
                User user = PlayersDatabase.getUser(mojangProfile.getUuid().get());
                if (user instanceof Player player) {
                    listener.accept(Optional.of(player));

                    return;
                }
            }

            listener.accept(Optional.empty());
        });
    }

    private static boolean setStatus(@MagicConstant(intValues = {OPERATION_ON, OPERATION_RESET, OPERATION_OFF})
                                     int operation, int module, @NotNull Player player) {
        TNTPlayerStorage tntPlayerStorage = player.tntPlayerInfo;
        if (tntPlayerStorage == null) return false;

        WebSocket webSocket = player.getSocket();
        if (webSocket == null) return false;

        if (operation == OPERATION_ON) {
            tntPlayerStorage.forceBlock &= ~(1L << module);
            tntPlayerStorage.forceActive |= (1L << module);

            TNTServer.serverSend(webSocket, new BlockModulesPacket(
                    tntPlayerStorage.forceBlock, tntPlayerStorage.forceActive));

            return true;
        } else if (operation == OPERATION_RESET) {
            tntPlayerStorage.forceActive &= ~(1L << module);
            tntPlayerStorage.forceBlock &= ~(1L << module);

            TNTServer.serverSend(webSocket, new BlockModulesPacket(
                    tntPlayerStorage.forceBlock, tntPlayerStorage.forceActive));

            return true;
        } else if (operation == OPERATION_OFF) {
            tntPlayerStorage.forceActive &= ~(1L << module);
            tntPlayerStorage.forceBlock |= (1L << module);

            TNTServer.serverSend(webSocket, new BlockModulesPacket(
                    tntPlayerStorage.forceBlock, tntPlayerStorage.forceActive));

            return true;
        }

        return false;
    }

    @NotNull
    @Override
    public String @NotNull [] getNames() {
        return new String[]{"fm", "forcemodule"};
    }

    @Override
    public void execute(@NotNull User user, @NotNull WebSocket userWebsocket, @NotNull String text) {
        if (user instanceof Player) {
            RemoteDB.readUserPrivileges(user.uuid, privilegesOptional -> {
                if (privilegesOptional.isEmpty()) {
                    CommandManager.sendError(userWebsocket, "Privilege server is not available");
                } else if (privilegesOptional.get().contains(PRIVILEGE)) {
                    step0(userWebsocket, text.strip().toLowerCase().split(" "));
                } else {
                    CommandManager.sendError(userWebsocket, "You don't have permission");
                }
            });
        } else if (user instanceof Bot bot) {
            if (bot.hasPrivilege(PRIVILEGE)) {
                step0(userWebsocket, text.strip().toLowerCase().split(" "));
            }
        }
    }
}
