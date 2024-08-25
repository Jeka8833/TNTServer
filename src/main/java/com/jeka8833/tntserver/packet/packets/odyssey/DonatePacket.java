package com.jeka8833.tntserver.packet.packets.odyssey;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.database.RemoteDB;
import com.jeka8833.tntserver.database.storage.User;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
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

        RemoteDB.readUserPrivileges(user.uuid, privilegesOptional -> {
            if (privilegesOptional.isEmpty()) {
                TNTServer.serverSend(socket, new DonatePacket(playerUUID, 1, 0));
                return;
            }

            DonateLevel donateLevel = DonateLevel.getDonateLevel(privilegesOptional.get());
            TNTServer.serverSend(socket, new DonatePacket(playerUUID, 0, donateLevel.getLevel()));
        });
    }

    public enum DonateLevel {
        NONE(),
        DONATE_SMALL(1, "DONATE_SMALL"),
        DONATE_BIG(2, "DONATE_BIG");

        private final int level;
        private final @Nullable String role;

        DonateLevel() {
            this.level = 0;
            this.role = null;
        }

        DonateLevel(int level, @NotNull String role) {
            this.level = level;
            this.role = role;
        }

        @NotNull
        @Contract(pure = true)
        public static DonateLevel getDonateLevel(@Nullable Collection<@NotNull String> roles) {
            if (roles == null) return NONE;

            for (DonateLevel donateLevel : values()) {
                if (donateLevel.role != null && roles.contains(donateLevel.role)) return donateLevel;
            }

            return NONE;
        }

        public int getLevel() {
            return level;
        }
    }
}
