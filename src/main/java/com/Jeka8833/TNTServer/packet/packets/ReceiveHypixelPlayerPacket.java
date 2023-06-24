package com.Jeka8833.TNTServer.packet.packets;

import com.Jeka8833.TNTServer.database.Player;
import com.Jeka8833.TNTServer.database.PlayersDatabase;
import com.Jeka8833.TNTServer.database.storage.HypixelPlayer;
import com.Jeka8833.TNTServer.database.storage.HypixelPlayerStorage;
import com.Jeka8833.TNTServer.packet.Packet;
import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public class ReceiveHypixelPlayerPacket implements Packet {

    public static final int RECEIVE_FAIL = 0;
    public static final int RECEIVE_GOOD = 1;
    public static final int RECEIVE_GOOD_NOTHING = 2;

    private @Nullable Collection<ReceivePlayer> userList;
    private boolean lastPacket = false;

    public ReceiveHypixelPlayerPacket() {
    }

    public ReceiveHypixelPlayerPacket(@NotNull Collection<ReceivePlayer> userList, boolean lastPacket) {
        this.userList = userList;
        this.lastPacket = lastPacket;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (userList == null) throw new NullPointerException("User list is empty");

        stream.writeBoolean(lastPacket);
        stream.writeByte(userList.size());
        for (ReceivePlayer player : userList) {
            stream.writeByte(player.status);
            stream.writeUUID(player.player);
            if (player.storage instanceof HypixelPlayerStorage) {
                ((HypixelPlayerStorage) player.storage).writeStream(stream);
            }
        }
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        lastPacket = stream.readBoolean();
        int size = stream.readUnsignedByte();
        for (int i = 0; i < size; i++) {
            int status = stream.readUnsignedByte();
            UUID playerUUID = stream.readUUID();
            if ((status & RECEIVE_GOOD) == RECEIVE_GOOD) {
                Player player = PlayersDatabase.getOrCreate(playerUUID);

                var storage = new HypixelPlayerStorage();
                storage.readStream(stream);
                player.hypixelPlayerInfo = storage;
            }
        }
    }

    @Override
    public void serverProcess(WebSocket socket, @Nullable Player user) {

    }

    public static class ReceivePlayer {
        public final UUID player;
        public final int status;
        public final HypixelPlayer storage;

        public ReceivePlayer(UUID player, HypixelPlayer storage) {
            this(player, storage, storage instanceof HypixelPlayerStorage ? RECEIVE_GOOD : RECEIVE_FAIL);
        }

        public ReceivePlayer(UUID player, HypixelPlayer storage, int status) {
            this.player = player;
            this.storage = storage;
            this.status = status;
        }
    }
}
