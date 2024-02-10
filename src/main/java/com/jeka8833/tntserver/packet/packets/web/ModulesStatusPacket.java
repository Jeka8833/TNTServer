package com.jeka8833.tntserver.packet.packets.web;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.managers.TNTClientDBManager;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.UUID;

public class ModulesStatusPacket implements Packet {
    private UUID requestedPlayer;

    private short callBackID = 0;
    private boolean playerFound = false;
    private long currentActives = 0;
    private long forceBlock = 0;
    private long forceActive = 0;

    @SuppressWarnings("unused")
    public ModulesStatusPacket() {
    }

    public ModulesStatusPacket(short callBackID) {
        this.callBackID = callBackID;
    }

    public ModulesStatusPacket(short callBackID, long currentActives, long forceBlock, long forceActive) {
        playerFound = true;

        this.callBackID = callBackID;
        this.currentActives = currentActives;
        this.forceBlock = forceBlock;
        this.forceActive = forceActive;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeShort(callBackID);
        stream.writeBoolean(playerFound);

        if (playerFound) {
            stream.writeLong(currentActives);
            stream.writeLong(forceBlock);
            stream.writeLong(forceActive);
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        callBackID = stream.readShort();
        requestedPlayer = stream.readUUID();
    }

    @Override
    public void serverProcess(WebSocket socket, User user) {
        if (BotsManager.isAbsent(user, "SERVER_CONTROL_MODULES")) {
            socket.close();
            return;
        }

        TNTClientDBManager.readOrCashUser(requestedPlayer, tntUser -> {
            if (tntUser == null || tntUser.tntPlayerInfo == null) {
                Main.serverSend(socket, new ModulesStatusPacket(callBackID));
            } else {
                Main.serverSend(socket, new ModulesStatusPacket(
                        callBackID, tntUser.tntPlayerInfo.activeModules,
                        tntUser.tntPlayerInfo.forceBlock, tntUser.tntPlayerInfo.forceActive));
            }
        });
    }
}
