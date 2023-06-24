package com.Jeka8833.TNTServer.database.storage;

import com.Jeka8833.TNTServer.packet.PacketInputStream;
import com.Jeka8833.TNTServer.packet.PacketOutputStream;
import com.Jeka8833.TNTServer.packet.StreamSerializer;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HypixelPlayerStorage implements StreamSerializer, HypixelPlayer {

    @SerializedName("stats")
    public Stats stats = new Stats();

    @SerializedName("networkExp")
    public long networkExp = -1;

    @Override
    public void readStream(@NotNull PacketInputStream stream) throws IOException {
        networkExp = stream.readLong();
        stats.TNTGames.coins = stream.readInt();
        stats.TNTGames.winstreak = stream.readInt();

        stats.TNTGames.wins_tntrun = stream.readInt();
        stats.TNTGames.deaths_tntrun = stream.readInt();
        stats.TNTGames.new_tntrun_double_jumps = stream.readInt();

        stats.TNTGames.wins_pvprun = stream.readInt();
        stats.TNTGames.deaths_pvprun = stream.readInt();
        stats.TNTGames.new_pvprun_double_jumps = stream.readInt();
        stats.TNTGames.kills_pvprun = stream.readInt();

        stats.TNTGames.wins_bowspleef = stream.readInt();
        stats.TNTGames.deaths_bowspleef = stream.readInt();
        stats.TNTGames.new_spleef_double_jumps = stream.readInt();

        stats.TNTGames.wins_tntag = stream.readInt();
        stats.TNTGames.deaths_tntag = stream.readInt();
        stats.TNTGames.kills_tntag = stream.readInt();

        stats.TNTGames.wins_capture = stream.readInt();
        stats.TNTGames.deaths_capture = stream.readInt();
        stats.TNTGames.kills_capture = stream.readInt();

        stats.duels.bowSpleefDuelWins = stream.readInt();
        stats.duels.bowSpleefDuelLosses = stream.readInt();
    }

    @Override
    public void writeStream(@NotNull PacketOutputStream stream) throws IOException {
        stream.writeLong(networkExp);
        stream.writeInt(stats.TNTGames.coins);
        stream.writeInt(stats.TNTGames.winstreak);

        stream.writeInt(stats.TNTGames.wins_tntrun);
        stream.writeInt(stats.TNTGames.deaths_tntrun);
        stream.writeInt(stats.TNTGames.new_tntrun_double_jumps);

        stream.writeInt(stats.TNTGames.wins_pvprun);
        stream.writeInt(stats.TNTGames.deaths_pvprun);
        stream.writeInt(stats.TNTGames.new_pvprun_double_jumps);
        stream.writeInt(stats.TNTGames.kills_pvprun);

        stream.writeInt(stats.TNTGames.wins_bowspleef);
        stream.writeInt(stats.TNTGames.deaths_bowspleef);
        stream.writeInt(stats.TNTGames.new_spleef_double_jumps);

        stream.writeInt(stats.TNTGames.wins_tntag);
        stream.writeInt(stats.TNTGames.deaths_tntag);
        stream.writeInt(stats.TNTGames.kills_tntag);

        stream.writeInt(stats.TNTGames.wins_capture);
        stream.writeInt(stats.TNTGames.deaths_capture);
        stream.writeInt(stats.TNTGames.kills_capture);

        stream.writeInt(stats.duels.bowSpleefDuelWins);
        stream.writeInt(stats.duels.bowSpleefDuelLosses);
    }

    public static class Stats {
        @SerializedName("TNTGames")
        public TNTGames TNTGames = new TNTGames();

        @SerializedName("Duels")
        public Duels duels = new Duels();
    }

    public static class TNTGames {

        // For TNTRun game
        @SerializedName("wins_tntrun")
        public int wins_tntrun = -1;

        @SerializedName("deaths_tntrun")
        public int deaths_tntrun = -1;

        @SerializedName("new_tntrun_double_jumps")
        public int new_tntrun_double_jumps = -1;

        // For PVPRun game
        @SerializedName("wins_pvprun")
        public int wins_pvprun = -1;

        @SerializedName("deaths_pvprun")
        public int deaths_pvprun = -1;

        @SerializedName("new_pvprun_double_jumps")
        public int new_pvprun_double_jumps = -1;

        @SerializedName("kills_pvprun")
        public int kills_pvprun = -1;

        // For BowSpleef game
        @SerializedName("wins_bowspleef")
        public int wins_bowspleef = -1;

        @SerializedName("deaths_bowspleef")
        public int deaths_bowspleef = -1;

        @SerializedName("new_spleef_double_jumps")
        public int new_spleef_double_jumps = -1;

        // For TNTTag game
        @SerializedName("wins_tntag")
        public int wins_tntag = -1;

        @SerializedName("deaths_tntag")
        public int deaths_tntag = -1;

        @SerializedName("kills_tntag")
        public int kills_tntag = -1;

        // For Wizard game
        @SerializedName("wins_capture")
        public int wins_capture = -1;

        @SerializedName("deaths_capture")
        public int deaths_capture = -1;

        @SerializedName("kills_capture")
        public int kills_capture = -1;

        // Other
        @SerializedName("winstreak")
        public int winstreak = -1;

        @SerializedName("coins")
        public int coins = -1;
    }

    public static class Duels {

        // For BowSpleef Duels game
        @SerializedName("bowspleef_duel_wins")
        public int bowSpleefDuelWins = -1;

        @SerializedName("bowspleef_duel_losses")
        public int bowSpleefDuelLosses = -1;
    }
}
