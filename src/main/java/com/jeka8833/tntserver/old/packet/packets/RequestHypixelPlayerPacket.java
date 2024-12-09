package com.jeka8833.tntserver.old.packet.packets;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.old.requester.HypixelCache;
import com.jeka8833.tntserver.old.user.UserBase;
import com.jeka8833.tntserver.old.user.player.Player;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class RequestHypixelPlayerPacket implements Packet {
    private @Nullable Set<UUID> userList;

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        if (userList == null || userList.isEmpty()) throw new NullPointerException("User list is empty");

        UUID[] array = userList.toArray(UUID[]::new);
        stream.writeByte(array.length);
        for (UUID user : array) {
            stream.writeUUID(user);
        }
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        int size = stream.readUnsignedByte();
        userList = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            userList.add(stream.readUUID());
        }
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (user instanceof Player player) {
            if (userList == null || userList.isEmpty()) throw new NullPointerException("User list is empty");

            HypixelCache.get(player.getUuid(), userList,
                    playersReady -> user.sendPacket(new ReceiveHypixelPlayerPacket(playersReady, false)),
                    () -> user.sendPacket(new ReceiveHypixelPlayerPacket(Collections.emptyMap(), true)));
        } else {
            user.disconnect();
        }
    }
}
