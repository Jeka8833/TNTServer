package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.storage.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.UUID;

public class BlockModulesPacket implements Packet {
    private static long globalBlock = 0;
    private static long globalActive = 0;

    private UUID editedUser;

    private long block = 0;
    private long active = 0;

    @SuppressWarnings("unused")
    public BlockModulesPacket() {
    }

    public BlockModulesPacket(final long block, long active) {
        this.block = block;
        this.active = active;
    }

    public static void readAndSetGlobalBlock() {
        RemoteDB.readUser(PlayersDatabase.SETTING_USER, userOptional -> {
            if (userOptional.isEmpty() ||
                    !(userOptional.get() instanceof Player player) || player.tntPlayerInfo == null) {
                return;
            }

            globalActive = player.tntPlayerInfo.forceActive;
            globalBlock = player.tntPlayerInfo.forceBlock;
        });
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeLong(block | globalBlock);
        stream.writeLong(active | globalActive);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        editedUser = stream.readUUID();
        block = stream.readLong();
        active = stream.readLong();
    }

    @Override
    public void serverProcess(WebSocket socket, User user) {
        if (BotsManager.isAbsent(user, "SERVER_CONTROL_MODULES")) {
            socket.close();
            return;
        }

        if (editedUser.equals(PlayersDatabase.SETTING_USER)) {
            globalActive = active;
            globalBlock = block;

            RemoteDB.readUser(PlayersDatabase.SETTING_USER, ignore -> {
                User settingUserStorage = PlayersDatabase.getOrCreate(PlayersDatabase.SETTING_USER);
                if (settingUserStorage instanceof Player player) {
                    if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();

                    player.tntPlayerInfo.forceActive = active;
                    player.tntPlayerInfo.forceBlock = block;
                }

                RemoteDB.writeUser(settingUserStorage);
            });

            for (WebSocket socket1 : TNTServer.server.getConnections()) {
                UUID id = socket1.getAttachment();
                if (id != null && id.version() == 4) {
                    RemoteDB.readUser(id, userOptional -> {
                        if (userOptional.isEmpty() ||
                                !(userOptional.get() instanceof Player player) || player.tntPlayerInfo == null) {
                            return;
                        }

                        TNTServer.serverSend(socket1, new BlockModulesPacket(
                                player.tntPlayerInfo.forceBlock, player.tntPlayerInfo.forceActive));
                    });
                }
            }
        } else {
            RemoteDB.readUser(editedUser, ignore -> {
                User editedUserStorage = PlayersDatabase.getOrCreate(editedUser);
                if (editedUserStorage instanceof Player player) {
                    if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();
                    player.tntPlayerInfo.forceActive = active;
                    player.tntPlayerInfo.forceBlock = block;

                    RemoteDB.writeUser(editedUserStorage);

                    WebSocket editedUserSocket = player.getSocket();
                    if (editedUserSocket != null) {
                        TNTServer.serverSend(editedUserSocket, new BlockModulesPacket(block, active));
                    }
                }
            });
        }
    }
}
