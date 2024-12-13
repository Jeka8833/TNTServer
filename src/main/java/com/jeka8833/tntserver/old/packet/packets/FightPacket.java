package com.jeka8833.tntserver.old.packet.packets;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.services.tntclient.user.UserBase;
import com.jeka8833.tntserver.services.tntclient.user.player.Player;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class FightPacket implements Packet {
    private final Collection<Player> activeConnection;
    private int playerFight = 0;

    @SuppressWarnings("unused")
    public FightPacket() {
        this(null);
    }

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        List<Player> users = activeConnection.stream()
                .filter(player -> player.getFight() != 0)
                .toList();

        stream.writeByte(users.size());
        for (Player user : users) {
            stream.writeUUID(user.getUuid());
            stream.writeInt(user.getFight());
        }
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        playerFight = stream.readInt();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (user instanceof Player player) {
            player.setFight(playerFight);

            player.sendPacket(new FightPacket(server.getUserDatabase().getAllPlayers()));
        } else {
            user.disconnect();
        }
    }
}
