package com.jeka8833.tntserver.old.packet.packets.webendpoints;

import com.jeka8833.tntserver.old.TNTServer;
import com.jeka8833.tntserver.old.packet.Packet;
import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import com.jeka8833.tntserver.old.packet.packets.WebTokenUseSidePacket;
import com.jeka8833.tntserver.old.user.UserBase;
import com.jeka8833.tntserver.old.user.player.Player;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

@NoArgsConstructor
public class WebTokenEndpointSidePacket implements Packet {
    private UUID user;
    private UUID key;
    private boolean register;

    public WebTokenEndpointSidePacket(UUID user, boolean register) {
        this.user = user;
        this.register = register;
    }

    @Override
    public void write(PacketOutputStream stream, int protocolVersion) throws IOException {
        stream.writeUUID(user);
        stream.writeBoolean(register);
    }

    @Override
    public void read(PacketInputStream stream, int protocolVersion) throws IOException {
        user = stream.readUUID();
        key = stream.readUUID();
    }

    @Override
    public void serverProcess(@NotNull UserBase user, @NotNull TNTServer server) {
        if (!user.hasPrivilege("SERVER_TOKEN")) {
            user.disconnect();

            return;
        }

        Player player = server.getUserDatabase().getPlayer(this.user);
        if (player != null) {
            player.sendPacket(new WebTokenUseSidePacket(this.user, this.key));
        }
    }
}
