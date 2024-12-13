package com.jeka8833.tntserver.services.tntclient.packet.serverbound;

import com.jeka8833.tntserver.old.requester.storage.HypixelCompactStructure;
import com.jeka8833.tntserver.services.tntclient.packet.ServerBoundPacket;
import com.jeka8833.toprotocol.addons.serializer.UUIDSerialize;
import com.jeka8833.toprotocol.core.serializer.InputByteArray;
import com.jeka8833.toprotocol.core.serializer.OutputByteArray;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ReceiveHypixelUserV2Serverbound implements ServerBoundPacket {
    private final @NotNull Map<@NotNull UUID, @NotNull HypixelCompactStructure> players;
    private final boolean lastPacket;

    public ReceiveHypixelUserV2Serverbound(@NotNull Map<@NotNull UUID, @NotNull HypixelCompactStructure> players,
                                           boolean lastPacket) {
        this.players = players;
        this.lastPacket = lastPacket;
    }

    public ReceiveHypixelUserV2Serverbound(InputByteArray serializer, Integer protocolVersion) {
        this.lastPacket = serializer.readBoolean();

        int size = serializer.readByte() & 0xFF;
        this.players = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = UUIDSerialize.readUUID(serializer);

            this.players.put(uuid, readUser(serializer, protocolVersion));
        }
    }

    @Override
    public void write(@NotNull OutputByteArray serializer, Integer protocolVersion) {
        serializer.writeBoolean(lastPacket);

        serializer.writeByte((byte) players.size());
        for (Map.Entry<UUID, HypixelCompactStructure> entry : players.entrySet()) {
            UUIDSerialize.writeUUID(serializer, entry.getKey());

            writeUser(serializer, protocolVersion, entry.getValue());
        }
    }

    @NotNull
    private static HypixelCompactStructure readUser(@NotNull InputByteArray serializer, int protocolVersion) {
        long networkExp = serializer.readLong();
        int coins = serializer.readInt();
        int winstreak = serializer.readInt();

        int wins_tntrun = serializer.readInt();
        int deaths_tntrun = serializer.readInt();

        int wins_pvprun = serializer.readInt();
        int deaths_pvprun = serializer.readInt();
        int kills_pvprun = serializer.readInt();

        int wins_bowspleef = serializer.readInt();
        int deaths_bowspleef = serializer.readInt();

        int wins_tntag = serializer.readInt();
        int deaths_tntag = serializer.readInt();
        int kills_tntag = serializer.readInt();

        int wins_capture = serializer.readInt();
        int deaths_capture = serializer.readInt();
        int kills_capture = serializer.readInt();

        int bowSpleefDuelWins = serializer.readInt();
        int bowSpleefDuelLosses = serializer.readInt();
        int bowSpleefDuelWinstreak = serializer.readInt();

        return new HypixelCompactStructure(networkExp, wins_tntrun, deaths_tntrun,
                wins_pvprun, deaths_pvprun, kills_pvprun, wins_bowspleef, deaths_bowspleef, wins_tntag, deaths_tntag,
                kills_tntag, wins_capture, deaths_capture, kills_capture, winstreak, coins, bowSpleefDuelWins,
                bowSpleefDuelLosses, bowSpleefDuelWinstreak);
    }

    private static void writeUser(@NotNull OutputByteArray serializer, int protocolVersion,
                                  @NotNull HypixelCompactStructure structure) {
        serializer.writeLong(structure.networkExp());
        serializer.writeInt(structure.tntGamesCoins());
        serializer.writeInt(structure.tntGamesWinstreak());

        serializer.writeInt(structure.tntRunWins());
        serializer.writeInt(structure.tntRunLosses());

        serializer.writeInt(structure.pvpRunWins());
        serializer.writeInt(structure.pvpRunLosses());
        serializer.writeInt(structure.pvpRunKills());

        serializer.writeInt(structure.bowSpleefWins());
        serializer.writeInt(structure.bowSpleefLosses());

        serializer.writeInt(structure.tntTagWins());
        serializer.writeInt(structure.tntTagLosses());
        serializer.writeInt(structure.tntTagKills());

        serializer.writeInt(structure.wizardsWins());
        serializer.writeInt(structure.wizardsLosses());
        serializer.writeInt(structure.wizardsKills());

        serializer.writeInt(structure.bowSpleefDuelsWins());
        serializer.writeInt(structure.bowSpleefDuelsLosses());
        serializer.writeInt(structure.bowSpleefDuelsWinstreak());
    }

    public @NotNull Map<@NotNull UUID, @NotNull HypixelCompactStructure> getPlayers() {
        return players;
    }

    public boolean isLastPacket() {
        return lastPacket;
    }
}
