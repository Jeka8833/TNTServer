package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.BotsManager;
import com.Jeka8833.TNTServer.Main;
import com.Jeka8833.TNTServer.TNTUser;
import com.Jeka8833.TNTServer.dataBase.TNTClientDBManager;
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
    public void serverProcess(WebSocket socket, TNTUser user) {
        if (!BotsManager.checkPrivilege(socket, "SERVER_CONTROL_MODULES")) {
            socket.close();
            return;
        }

        if (editedUser.equals(settingUser)) {
            globalActive = active;
            globalBlock = block;

            TNTClientDBManager.readOrCashUser(settingUser, ignore -> {
                TNTUser account = TNTClientDBManager.getOrCreate(settingUser);
                account.forceActive = active;
                account.forceBlock = block;

                TNTClientDBManager.writeUser(settingUser, null);
            });

            for (WebSocket socket1 : Main.server.getConnections()) {
                UUID id = socket1.getAttachment();
                if (id != null && id.version() == 4) {
                    TNTClientDBManager.readOrCashUser(id, tntUser1 -> {
                        if (tntUser1 == null) return;

                        Main.serverSend(socket1, new BlockModulesPacket(tntUser1.forceBlock, tntUser1.forceActive));
                    });
                }
            }
        } else {
            TNTClientDBManager.readOrCashUser(editedUser, ignore -> {
                TNTUser account = TNTClientDBManager.getOrCreate(editedUser);
                account.forceActive = active;
                account.forceBlock = block;

                TNTClientDBManager.writeUser(editedUser, null);

                Main.server.getConnections().stream()
                        .filter(socket1 -> account.uuid.equals(socket1.getAttachment()))
                        .forEach(socket1 -> Main.serverSend(socket1, new BlockModulesPacket(block, active)));
            });
        }
    }

    public static void readAndSetGlobalBlock() {
        TNTClientDBManager.readOrCashUser(settingUser, tntUser -> {
            if (tntUser == null) return;

            globalActive = tntUser.forceActive;
            globalBlock = tntUser.forceBlock;
        });
    }
}
