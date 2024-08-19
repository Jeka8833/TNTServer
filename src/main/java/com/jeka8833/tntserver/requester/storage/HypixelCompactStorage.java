package com.jeka8833.tntserver.requester.storage;

import com.jeka8833.tntserver.packet.PacketInputStream;
import com.jeka8833.tntserver.packet.PacketOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

public record HypixelCompactStorage(long networkExp,

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
                                    int bowSpleefDuelsWinstreak) {

    public static final HypixelCompactStorage EMPTY_INSTANCE = new HypixelCompactStorage(
            -1L, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1,
            -1);

    public static HypixelCompactStorage compress(@NotNull HypixelJSONStructure structure) {
        if (structure.isEmpty()) return EMPTY_INSTANCE;

        Optional<HypixelJSONStructure.Player> player = structure.player;
        Optional<HypixelJSONStructure.TNTGames> tntGames = player.flatMap(p -> p.stats).flatMap(stats -> stats.tntGames);
        Optional<HypixelJSONStructure.Duels> duels = player.flatMap(p -> p.stats).flatMap(stats -> stats.duels);

        return new HypixelCompactStorage(
                player.flatMap(p -> p.networkExp).orElse(-1L),

                tntGames.flatMap(g -> g.tntRunWins).orElse(-1),
                tntGames.flatMap(g -> g.tntRunLosses).orElse(-1),

                tntGames.flatMap(g -> g.pvpRunWins).orElse(-1),
                tntGames.flatMap(g -> g.pvpRunLosses).orElse(-1),
                tntGames.flatMap(g -> g.pvpRunKills).orElse(-1),

                tntGames.flatMap(g -> g.bowSpleefWins).orElse(-1),
                tntGames.flatMap(g -> g.bowSpleefLosses).orElse(-1),

                tntGames.flatMap(g -> g.tntTagWins).orElse(-1),
                tntGames.flatMap(g -> g.tntTagLosses).orElse(-1),
                tntGames.flatMap(g -> g.tntTagKills).orElse(-1),

                tntGames.flatMap(g -> g.wizardsWins).orElse(-1),
                tntGames.flatMap(g -> g.wizardsLosses).orElse(-1),
                tntGames.flatMap(g -> g.wizardsKills).orElse(-1),

                tntGames.flatMap(g -> g.winStreak).orElse(-1),
                tntGames.flatMap(g -> g.coins).orElse(-1),

                duels.flatMap(d -> d.bowSpleefWins).orElse(-1),
                duels.flatMap(d -> d.bowSpleefLosses).orElse(-1),
                duels.flatMap(d -> d.bowSpleefWinStreak).orElse(-1)
        );
    }

    @NotNull
    @Contract("_ -> new")
    public static HypixelCompactStorage readStream(@NotNull PacketInputStream stream) throws IOException {
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

        return new HypixelCompactStorage(networkExp, wins_tntrun, deaths_tntrun, wins_pvprun, deaths_pvprun,
                kills_pvprun, wins_bowspleef, deaths_bowspleef, wins_tntag, deaths_tntag, kills_tntag, wins_capture,
                deaths_capture, kills_capture, winstreak, coins, bowSpleefDuelWins, bowSpleefDuelLosses,
                bowSpleefDuelWinstreak);
    }

    @NotNull
    @Contract("_ -> new")
    public static HypixelCompactStorage readStreamV2(@NotNull PacketInputStream stream) throws IOException {
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

        return new HypixelCompactStorage(networkExp, wins_tntrun, deaths_tntrun, wins_pvprun, deaths_pvprun,
                kills_pvprun, wins_bowspleef, deaths_bowspleef, wins_tntag, deaths_tntag, kills_tntag, wins_capture,
                deaths_capture, kills_capture, winstreak, coins, bowSpleefDuelWins, bowSpleefDuelLosses,
                bowSpleefDuelWinstreak);
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
}
