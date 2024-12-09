package com.jeka8833.tntserver.old.packet.packets;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.database.RemoteDB;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.old.packet.packets.webendpoints.DiscordTokenEndpointSidePacket;
import com.jeka8833.tntserver.old.user.Bot;
import com.jeka8833.tntserver.old.user.UserBase;
import com.jeka8833.tntserver.old.user.player.Player;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
public class DiscordTokenUserSidePacket implements Packet {
    public static final int INTERNAL_ERROR = 0;
    public static final int CONNECTION_ERROR = 1;
    public static final int MESSAGE_GOOD_LINK = 2;
    public static final int MESSAGE_BAD_LINK = 3;
    public static final int MESSAGE_GOOD_UNLINK = 4;
    public static final int MESSAGE_BAD_UNLINK = 5;
    public static final int TRY_LINK = 6;
    public static final int TRY_UNLINK = 7;

    private UUID user;
    private int code;
    private int statusCode;

    @SuppressWarnings("unused")
    public DiscordTokenUserSidePacket() {
        this(null, Integer.MIN_VALUE, INTERNAL_ERROR);
    }

    public DiscordTokenUserSidePacket(UUID user, int code, int statusCode) {
        this.user = user;
        this.code = code;
        this.statusCode = statusCode;
    }

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        stream.writeUUID(user);
        stream.writeInt(code);
        stream.writeInt(statusCode);
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        user = stream.readUUID();
        code = stream.readInt();
        statusCode = stream.readInt();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (user instanceof Player player) {
            if (statusCode == TRY_LINK) {
                List<Bot> bots = server.getUserDatabase().findAllBotsByPrivilege("SERVER_DISCORD_LINK");
                if (bots.isEmpty()) {
                    player.sendPacket(new DiscordTokenUserSidePacket(player.getUuid(), code, CONNECTION_ERROR));

                    return;
                }

                if (bots.size() > 1) {
                    log.warn("More than one bot with SERVER_DISCORD_LINK privilege. UUIDs: {}", bots);
                }

                Bot bot = bots.getFirst();

                bot.sendPacket(new DiscordTokenEndpointSidePacket(player.getUuid(), code));
            } else if (statusCode == TRY_UNLINK) {
                RemoteDB.removeDiscordUser(player.getUuid());

                player.sendPacket(new DiscordTokenUserSidePacket(player.getUuid(), code, MESSAGE_GOOD_UNLINK));
            } else {
                player.sendPacket(new DiscordTokenUserSidePacket(player.getUuid(), code, INTERNAL_ERROR));
            }
        } else {
            user.disconnect();
        }
    }
}
