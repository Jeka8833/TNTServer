package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.managers.TNTClientDBManager;
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
        TNTClientDBManager.readOrCashUser(PlayersDatabase.SETTING_USER, tntUser -> {
            if (tntUser == null || tntUser.tntPlayerInfo == null) return;

            globalActive = tntUser.tntPlayerInfo.forceActive;
            globalBlock = tntUser.tntPlayerInfo.forceBlock;
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

            TNTClientDBManager.readOrCashUser(PlayersDatabase.SETTING_USER, ignore -> {
                User settingUserStorage = PlayersDatabase.getOrCreate(PlayersDatabase.SETTING_USER);
                if (settingUserStorage instanceof Player player) {
                    if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();
                    player.tntPlayerInfo.forceActive = active;
                    player.tntPlayerInfo.forceBlock = block;

                    TNTClientDBManager.writeUser(PlayersDatabase.SETTING_USER, null);
                }
            });

            for (WebSocket socket1 : TNTServer.server.getConnections()) {
                UUID id = socket1.getAttachment();
                if (id != null && id.version() == 4) {
                    TNTClientDBManager.readUser(id, tntUser -> {
                        if (tntUser == null || tntUser.tntPlayerInfo == null) return;

                        TNTServer.serverSend(socket1, new BlockModulesPacket(
                                tntUser.tntPlayerInfo.forceBlock, tntUser.tntPlayerInfo.forceActive));
                    });
                }
            }
        } else {
            TNTClientDBManager.readUser(editedUser, ignore -> {
                User editedUserStorage = PlayersDatabase.getOrCreate(editedUser);
                if (editedUserStorage instanceof Player player) {
                    if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();
                    player.tntPlayerInfo.forceActive = active;
                    player.tntPlayerInfo.forceBlock = block;

                    TNTClientDBManager.writeUser(editedUser, null);

                    WebSocket editedUserSocket = player.getSocket();
                    if (editedUserSocket != null) {
                        TNTServer.serverSend(editedUserSocket, new BlockModulesPacket(block, active));
                    }
                }
            });
        }
    }
}
