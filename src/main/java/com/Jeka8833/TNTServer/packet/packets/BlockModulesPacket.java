package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.BotsManager;
import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.database.Player;
import com.Jeka8833.TNTServer.database.PlayersDatabase;
import com.Jeka8833.TNTServer.database.managers.TNTClientDBManager;
import com.Jeka8833.TNTServer.database.storage.TNTPlayerStorage;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.UUID;

public class BlockModulesPacket implements Packet {

    private static final UUID settingUser = UUID.fromString("00000000-0000-4000-0000-000000000000");

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
    public void serverProcess(WebSocket socket, Player user) {
        if (!BotsManager.checkPrivilege(socket, "SERVER_CONTROL_MODULES")) {
            socket.close();
            return;
        }

        if (editedUser.equals(settingUser)) {
            globalActive = active;
            globalBlock = block;

            TNTClientDBManager.readOrCashUser(settingUser, ignore -> {
                Player player = PlayersDatabase.getOrCreate(settingUser);

                player.tntPlayerInfo = new TNTPlayerStorage();
                player.tntPlayerInfo.forceActive = active;
                player.tntPlayerInfo.forceBlock = block;

                TNTClientDBManager.writeUser(settingUser, null);
            });

            for (WebSocket socket1 : Main.server.getConnections()) {
                UUID id = socket1.getAttachment();
                if (id != null && id.version() == 4) {
                    TNTClientDBManager.readOrCashUser(id, tntUser -> {
                        if (tntUser == null || tntUser.tntPlayerInfo == null) return;

                        Main.serverSend(socket1, new BlockModulesPacket(
                                tntUser.tntPlayerInfo.forceBlock, tntUser.tntPlayerInfo.forceActive));
                    });
                }
            }
        } else {
            TNTClientDBManager.readOrCashUser(editedUser, ignore -> {
                Player player = PlayersDatabase.getOrCreate(editedUser);

                player.tntPlayerInfo = new TNTPlayerStorage();
                player.tntPlayerInfo.forceActive = active;
                player.tntPlayerInfo.forceBlock = block;

                TNTClientDBManager.writeUser(editedUser, null);

                Main.server.getConnections().stream()
                        .filter(socket1 -> player.uuid.equals(socket1.getAttachment()))
                        .forEach(socket1 -> Main.serverSend(socket1, new BlockModulesPacket(block, active)));
            });
        }
    }

    public static void readAndSetGlobalBlock() {
        TNTClientDBManager.readOrCashUser(settingUser, tntUser -> {
            if (tntUser == null || tntUser.tntPlayerInfo == null) return;

            globalActive = tntUser.tntPlayerInfo.forceActive;
            globalBlock = tntUser.tntPlayerInfo.forceBlock;
        });
    }
}
