package com.jeka8833.tntserver.packet.packets.odyssey;

import com.jeka8833.tntserver.BotsManager;
import com.jeka8833.tntserver.Main;
import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.callback.CallbackManager;
import com.jeka8833.tntserver.packet.packets.web.RolePacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DonatePacket implements Packet {
    private static final Logger logger = LogManager.getLogger(DonatePacket.class);

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
    public void serverProcess(WebSocket socket, @Nullable Player user) {
        if (!BotsManager.checkPrivilege(socket, "ODYSSEY_DONATE")) {
            socket.close();
            return;
        }

        if (playerUUID == null) throw new NullPointerException("User is null");

        List<Map.Entry<UUID, BotsManager.BotUser>> bots = BotsManager.getBots("SERVER_ROLES");
        if (!bots.isEmpty()) {
            try {
                for (Map.Entry<UUID, BotsManager.BotUser> entry : bots) {
                    CallbackManager.sendAndGetResult(entry.getValue().connection(),
                            new RolePacket(playerUUID), packet -> {
                                if (packet == null) {
                                    Main.serverSend(socket, new DonatePacket(playerUUID, 2, 0));
                                } else {
                                    DonateLevel donateLevel = DonateLevel.getDonateLevel(packet.getRoles());

                                    Main.serverSend(socket,
                                            new DonatePacket(playerUUID, 0, donateLevel.getLevel()));
                                }
                            });
                }

                return;
            } catch (Exception e) {
                logger.warn("Fail send to bot", e);
            }
        }

        Main.serverSend(socket, new DonatePacket(playerUUID, 1, 0));
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
