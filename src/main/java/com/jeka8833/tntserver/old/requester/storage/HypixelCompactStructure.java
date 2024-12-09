package com.jeka8833.tntserver.old.requester.storage;

import com.jeka8833.tntserver.old.packet.PacketInputStream;
import com.jeka8833.tntserver.old.packet.PacketOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;

public record HypixelCompactStructure(
        long networkExp,

        int tntRunWins,
        int tntRunLosses,

        int pvpRunWins,
        int pvpRunLosses,
        int pvpRunKills,

        int bowSpleefWins,
        int bowSpleefLosses,

        int tntTagWins,
        int tntTagLosses,
        int tntTagKills,

        int wizardsWins,
        int wizardsLosses,
        int wizardsKills,

        int tntGamesWinstreak,
        int tntGamesCoins,

        int bowSpleefDuelsWins,
        int bowSpleefDuelsLosses,
        int bowSpleefDuelsWinstreak
) implements Serializable, Comparable<HypixelCompactStructure> {
    @Serial
    private static final long serialVersionUID = 5349876747379349796L;

    public static final HypixelCompactStructure EMPTY_INSTANCE = new HypixelCompactStructure(-1L,
            -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1,
            -1);

    @NotNull
    @Contract("_ -> new")
    public static HypixelCompactStructure readStream(@NotNull PacketInputStream stream) throws IOException {
        long networkExp = stream.readLong();
        int coins = stream.readInt();
        int winstreak = stream.readInt();

        int wins_tntrun = stream.readInt();
        int deaths_tntrun = stream.readInt();
        stream.skipNBytes(4);       // new_tntrun_double_jumps

        int wins_pvprun = stream.readInt();
        int deaths_pvprun = stream.readInt();
        stream.skipNBytes(4);       // new_pvprun_double_jumps
        int kills_pvprun = stream.readInt();

        int wins_bowspleef = stream.readInt();
        int deaths_bowspleef = stream.readInt();
        stream.skipNBytes(4);       // new_spleef_double_jumps

        int wins_tntag = stream.readInt();
        int deaths_tntag = stream.readInt();
        int kills_tntag = stream.readInt();

        int wins_capture = stream.readInt();
        int deaths_capture = stream.readInt();
        int kills_capture = stream.readInt();

        int bowSpleefDuelWins = stream.readInt();
        int bowSpleefDuelLosses = stream.readInt();
        int bowSpleefDuelWinstreak = -1;

        return new HypixelCompactStructure(networkExp, wins_tntrun, deaths_tntrun,
                wins_pvprun, deaths_pvprun, kills_pvprun, wins_bowspleef, deaths_bowspleef, wins_tntag, deaths_tntag,
                kills_tntag, wins_capture, deaths_capture, kills_capture, winstreak, coins, bowSpleefDuelWins,
                bowSpleefDuelLosses, bowSpleefDuelWinstreak);
    }

    @NotNull
    @Contract("_ -> new")
    public static HypixelCompactStructure readStreamV2(@NotNull PacketInputStream stream) throws IOException {
        long networkExp = stream.readLong();
        int coins = stream.readInt();
        int winstreak = stream.readInt();

        int wins_tntrun = stream.readInt();
        int deaths_tntrun = stream.readInt();

        int wins_pvprun = stream.readInt();
        int deaths_pvprun = stream.readInt();
        int kills_pvprun = stream.readInt();

        int wins_bowspleef = stream.readInt();
        int deaths_bowspleef = stream.readInt();

        int wins_tntag = stream.readInt();
        int deaths_tntag = stream.readInt();
        int kills_tntag = stream.readInt();

        int wins_capture = stream.readInt();
        int deaths_capture = stream.readInt();
        int kills_capture = stream.readInt();

        int bowSpleefDuelWins = stream.readInt();
        int bowSpleefDuelLosses = stream.readInt();
        int bowSpleefDuelWinstreak = stream.readInt();

        return new HypixelCompactStructure(networkExp, wins_tntrun, deaths_tntrun,
                wins_pvprun, deaths_pvprun, kills_pvprun, wins_bowspleef, deaths_bowspleef, wins_tntag, deaths_tntag,
                kills_tntag, wins_capture, deaths_capture, kills_capture, winstreak, coins, bowSpleefDuelWins,
                bowSpleefDuelLosses, bowSpleefDuelWinstreak);
    }

    public void writeStream(@NotNull PacketOutputStream stream) throws IOException {
        stream.writeLong(networkExp());
        stream.writeInt(tntGamesCoins());
        stream.writeInt(tntGamesWinstreak());

        stream.writeInt(tntRunWins());
        stream.writeInt(tntRunLosses());
        stream.writeInt(7);     // new_tntrun_double_jumps

        stream.writeInt(pvpRunWins());
        stream.writeInt(pvpRunLosses());
        stream.writeInt(7);     // new_pvprun_double_jumps
        stream.writeInt(pvpRunKills());

        stream.writeInt(bowSpleefWins());
        stream.writeInt(bowSpleefLosses());
        stream.writeInt(7);     // new_spleef_double_jumps

        stream.writeInt(tntTagWins());
        stream.writeInt(tntTagLosses());
        stream.writeInt(tntTagKills());

        stream.writeInt(wizardsWins());
        stream.writeInt(wizardsLosses());
        stream.writeInt(wizardsKills());

        stream.writeInt(bowSpleefDuelsWins());
        stream.writeInt(bowSpleefDuelsLosses());
    }

    public void writeStreamV2(@NotNull PacketOutputStream stream) throws IOException {
        stream.writeLong(networkExp());
        stream.writeInt(tntGamesCoins());
        stream.writeInt(tntGamesWinstreak());

        stream.writeInt(tntRunWins());
        stream.writeInt(tntRunLosses());

        stream.writeInt(pvpRunWins());
        stream.writeInt(pvpRunLosses());
        stream.writeInt(pvpRunKills());

        stream.writeInt(bowSpleefWins());
        stream.writeInt(bowSpleefLosses());

        stream.writeInt(tntTagWins());
        stream.writeInt(tntTagLosses());
        stream.writeInt(tntTagKills());

        stream.writeInt(wizardsWins());
        stream.writeInt(wizardsLosses());
        stream.writeInt(wizardsKills());

        stream.writeInt(bowSpleefDuelsWins());
        stream.writeInt(bowSpleefDuelsLosses());
        stream.writeInt(bowSpleefDuelsWinstreak());
    }

    @Override
    public int compareTo(@NotNull HypixelCompactStructure o) {
        return Comparator.comparingLong((HypixelCompactStructure value) -> value.networkExp)
                .thenComparingInt(value -> value.tntRunWins)
                .thenComparingInt(value -> value.tntRunLosses)
                .thenComparingInt(value -> value.pvpRunWins)
                .thenComparingInt(value -> value.pvpRunLosses)
                .thenComparingInt(value -> value.pvpRunKills)
                .thenComparingInt(value -> value.bowSpleefWins)
                .thenComparingInt(value -> value.bowSpleefLosses)
                .thenComparingInt(value -> value.tntTagWins)
                .thenComparingInt(value -> value.tntTagLosses)
                .thenComparingInt(value -> value.tntTagKills)
                .thenComparingInt(value -> value.wizardsWins)
                .thenComparingInt(value -> value.wizardsLosses)
                .thenComparingInt(value -> value.wizardsKills)
                .thenComparingInt(value -> value.bowSpleefDuelsWins)
                .thenComparingInt(value -> value.bowSpleefDuelsLosses)
                .compare(o, this);
    }
}
