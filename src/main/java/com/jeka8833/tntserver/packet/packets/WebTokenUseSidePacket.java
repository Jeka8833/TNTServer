package com.jeka8833.tntserver.packet.packets;

import com.jeka8833.tntserver.TNTServer;
import com.jeka8833.tntserver.packet.Packet;
import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import com.jeka8833.tntserver.packet.packets.webendpoints.WebTokenEndpointSidePacket;
import com.jeka8833.tntserver.user.Bot;
import com.jeka8833.tntserver.user.UserBase;
import com.jeka8833.tntserver.user.player.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class WebTokenUseSidePacket implements Packet {
    private static final UUID NULL_UUID = new UUID(0, 0);

    private final UUID user;
    private final UUID key;

    private boolean unregister;

    @SuppressWarnings("unused")
    public WebTokenUseSidePacket() {
        this(null, null);
    }

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        stream.writeUUID(user);
        stream.writeUUID(key);
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        unregister = stream.readBoolean();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (user instanceof Player player) {
            List<Bot> bots = server.getUserDatabase().findAllBotsByPrivilege("SERVER_TOKEN");
            if (bots.isEmpty()) {
                user.sendPacket(new WebTokenUseSidePacket(NULL_UUID, NULL_UUID));

                return;
            }

            if (bots.size() > 1) {
                log.warn("Multiple bots with SERVER_TOKEN privilege!! UUIDs: {}", bots);
            }

            bots.getFirst().sendPacket(new WebTokenEndpointSidePacket(player.getUuid(), !unregister));
        } else {
            user.disconnect();
        }
    }
}
