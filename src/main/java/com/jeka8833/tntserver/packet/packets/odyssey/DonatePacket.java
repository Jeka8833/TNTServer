package com.jeka8833.tntserver.packet.packets.odyssey;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Bot;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.callback.CallbackManager;
import com.jeka8833.tntserver.packet.packets.web.RolePacket;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class DonatePacket implements Packet {
    private @Nullable UUID playerUUID;
    private int errorCode;
    private int donate;

    @SuppressWarnings("unused")
    public DonatePacket() {
    }

    public DonatePacket(@NotNull UUID playerUUID, int errorCode, int donate) {
        this.playerUUID = playerUUID;
        this.errorCode = errorCode;
        this.donate = donate;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (playerUUID == null) throw new NullPointerException("User is null");

        stream.writeUUID(playerUUID);
        stream.writeByte(errorCode);
        stream.writeByte(donate);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        playerUUID = stream.readUUID();
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable User user) {
        if (playerUUID == null) throw new NullPointerException("User is null");

        if (BotsManager.isAbsent(user, "ODYSSEY_DONATE")) {
            socket.close();
            return;
        }

        Bot roleServer = PlayersDatabase.getBotWithPrivilege("SERVER_ROLES");
        if (roleServer == null) {
            Main.serverSend(socket, new DonatePacket(playerUUID, 1, 0));
            return;
        }

        WebSocket roleServerSocket = roleServer.getSocket();
        if (roleServerSocket == null) {
            Main.serverSend(socket, new DonatePacket(playerUUID, 3, 0));
            return;
        }

        CallbackManager.sendAndGetResult(roleServerSocket, new RolePacket(playerUUID), packet -> {
            if (packet == null) {
                Main.serverSend(socket, new DonatePacket(playerUUID, 2, 0));
            } else {
                DonateLevel donateLevel = DonateLevel.getDonateLevel(packet.getRoles());

                Main.serverSend(socket, new DonatePacket(playerUUID, 0, donateLevel.getLevel()));
            }
        });
    }

    public enum DonateLevel {
        NONE(0),
        DONATE_SMALL(1, "DONATE_SMALL"),
        DONATE_BIG(2, "DONATE_BIG");

        private final int level;
        private final @Nullable String role;

        DonateLevel(int level) {
            this.level = level;
            this.role = null;
        }

        DonateLevel(int level, @NotNull String role) {
            this.level = level;
            this.role = role;
        }

        public int getLevel() {
            return level;
        }

        @NotNull
        @Contract(pure = true)
        public static DonateLevel getDonateLevel(@Nullable String roles) {
            if (roles == null) return NONE;

            String[] rolesArray = roles.split(",");
            for (String role : rolesArray) {
                for (DonateLevel donateLevel : values()) {
                    if (donateLevel.role != null && donateLevel.role.equals(role)) return donateLevel;
                }
            }

            return NONE;
        }
    }
}
