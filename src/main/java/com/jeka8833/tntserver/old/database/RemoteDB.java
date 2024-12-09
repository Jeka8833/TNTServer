package com.jeka8833.tntserver.old.database;

import com.jeka8833.tntserver.old.Main;
import com.jeka8833.tntserver.old.database.storage.Player;
import com.jeka8833.tntserver.old.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.old.database.storage.User;
import com.jeka8833.tntserver.old.gamechat.MuteDto;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public final class RemoteDB {
    private static final int MAX_RETRIES = 3;

    private static final ExecutorService EXECUTORS = Executors.newSingleThreadExecutor();

    private static @Nullable Connection connection;
    private static @Nullable PreparedStatement readUserPrivileges;
    private static @Nullable PreparedStatement botLogin;
    private static @Nullable PreparedStatement readUser;
    private static @Nullable PreparedStatement readUsers;
    private static @Nullable PreparedStatement writeUser;
    private static @Nullable PreparedStatement readMutePlayer;
    private static @Nullable PreparedStatement removeDiscordUser;

    public static void openConnection() {
        if (Main.INSTANCE.databaseURL == null ||
                Main.INSTANCE.databaseUser == null || Main.INSTANCE.databasePassword == null) {
            log.warn("Database URL, user or password is null");
            return;
        }

        log.info("Open connection to DB");

        try {
            connection = DriverManager.getConnection("jdbc:postgresql://" + Main.INSTANCE.databaseURL,
                    Main.INSTANCE.databaseUser, Main.INSTANCE.databasePassword);

            readUserPrivileges = connection.prepareStatement(
                    "SELECT \"roles\" FROM \"tntclient_user_roles_and_keys\" WHERE \"user\" = ?");
            botLogin = connection.prepareStatement(
                    "SELECT \"roles\" FROM \"tntclient_user_roles_and_keys\" WHERE \"user\" = ? AND \"static_key\" = ?");
            readUser = connection.prepareStatement(
                    "SELECT \"version\", \"blockModules\", \"donate\" FROM \"TC_Players\" WHERE \"user\" = ?");
            readUsers = connection.prepareStatement(
                    "SELECT \"user\", \"version\", \"blockModules\", \"donate\"" +
                            "FROM \"TC_Players\" WHERE \"user\" = ANY(?::uuid[])");
            writeUser = connection.prepareStatement(
                    "INSERT INTO \"TC_Players\" (\"user\", \"version\", \"timeLogin\", \"blockModules\")" +
                            "VALUES (?, ?, CURRENT_TIMESTAMP, ?) ON CONFLICT (\"user\")" +
                            "DO UPDATE SET \"version\" = EXCLUDED.\"version\", \"timeLogin\" = CURRENT_TIMESTAMP," +
                            "\"blockModules\" = EXCLUDED.\"blockModules\"");
            readMutePlayer = connection.prepareStatement(
                    "SELECT \"reason\", \"unmute_time\" FROM \"tntclient_muted_player\" WHERE \"player\" = ?");
            removeDiscordUser = connection.prepareStatement(
                    "DELETE FROM \"discordbot_discord_minecraft_user_connection\" WHERE \"minecraft\" = ?");

        } catch (SQLException e) {
            log.error("Fail connect to DB", e);
        }
    }

    public static void readUserPrivileges(@NotNull UUID uuid,
                                          @NotNull Consumer<@NotNull Optional<@NotNull Set<@NotNull String>>> consumer) {
        runTask(() -> {
            //noinspection DataFlowIssue
            readUserPrivileges.setObject(1, uuid);

            try (ResultSet rs = readUserPrivileges.executeQuery()) {
                if (rs.next()) {
                    String rolesString = rs.getString(1);
                    Set<String> roles = (rolesString == null) ? Collections.emptySet() :
                            new HashSet<>(Arrays.asList(rolesString.split(",")));

                    consumer.accept(Optional.of(roles));
                } else {
                    consumer.accept(Optional.of(Collections.emptySet()));
                }
            }

            return true;
        }, () -> consumer.accept(Optional.empty()));
    }

    public static void loginAndGetPrivileges(@NotNull UUID user, @NotNull UUID password,
                                             @NotNull Consumer<
                                                     @NotNull Optional<@NotNull Set<@NotNull String>>> consumer) {
        runTask(() -> {
            //noinspection DataFlowIssue
            botLogin.setObject(1, user);
            botLogin.setObject(2, password);

            try (ResultSet rs = botLogin.executeQuery()) {
                if (rs.next()) {
                    String rolesString = rs.getString(1);
                    Set<String> roles = (rolesString == null) ? Collections.emptySet() :
                            new HashSet<>(Arrays.asList(rolesString.split(",")));

                    if (rolesString != null && roles.contains("TNTCLIENT_SERVER")) {
                        consumer.accept(Optional.of(roles));
                    } else {
                        consumer.accept(Optional.of(Collections.emptySet()));
                    }
                } else {
                    consumer.accept(Optional.of(Collections.emptySet()));
                }
            }

            return true;
        }, () -> consumer.accept(Optional.empty()));
    }

    public static void writeUser(@NotNull User user) {
        runTask(() -> {
            //noinspection DataFlowIssue
            writeUser.setObject(1, user.uuid);

            if (user instanceof Player player && player.tntPlayerInfo != null) {
                writeUser.setString(2, player.tntPlayerInfo.version);
                writeUser.setLong(3, player.tntPlayerInfo.forceBlock);
            } else {
                writeUser.setString(2, "");
                writeUser.setLong(3, 0);
            }

            writeUser.executeUpdate();

            return true;
        }, null);
    }

    public static void readUser(@NotNull UUID uuid, @NotNull Consumer<@NotNull Optional<@NotNull User>> consumer) {
        runTask(() -> {
            //noinspection DataFlowIssue
            readUser.setObject(1, uuid);

            try (ResultSet rs = readUser.executeQuery()) {
                User user = PlayersDatabase.getOrCreate(uuid);

                if (rs.next() && user instanceof Player player) {
                    if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();

                    player.tntPlayerInfo.version = rs.getString(1);
                    player.tntPlayerInfo.forceBlock = rs.getLong(2);
                    player.tntPlayerInfo.donate = rs.getByte(3);
                }

                consumer.accept(Optional.of(user));
            }

            return true;
        }, () -> consumer.accept(Optional.empty()));
    }

    @SuppressWarnings("DataFlowIssue")
    public static void readUsers(@NotNull Set<@NotNull UUID> uuids,
                                 @NotNull Consumer<@NotNull ArrayList<@NotNull User>> consumer) {
        if (uuids.isEmpty()) return;

        Set<UUID> copyUUIDs = new HashSet<>(uuids);

        runTask(() -> {
            ArrayList<User> resultGroup = new ArrayList<>();

            try {
                Array array = connection.createArrayOf("uuid", copyUUIDs.toArray());
                readUsers.setArray(1, array);

                try (ResultSet rs = readUsers.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = rs.getObject(1, UUID.class);
                        if (uuid == null) continue;

                        User user = PlayersDatabase.getOrCreate(uuid);
                        if (user instanceof Player player) {
                            if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();

                            player.tntPlayerInfo.version = rs.getString(2);
                            player.tntPlayerInfo.forceBlock = rs.getLong(3);
                            player.tntPlayerInfo.donate = rs.getByte(4);
                        }

                        resultGroup.add(user);

                        copyUUIDs.remove(uuid);
                    }
                }

                return true;
            } finally {
                if (!resultGroup.isEmpty()) {
                    consumer.accept(resultGroup);
                }
            }
        }, null);
    }

    public static void readMute(@NotNull UUID uuid, @NotNull Consumer<@NotNull Optional<@NotNull MuteDto>> consumer) {
        runTask(() -> {
            //noinspection DataFlowIssue
            readMutePlayer.setObject(1, uuid);

            try (ResultSet rs = readMutePlayer.executeQuery()) {
                if (rs.next()) {
                    String reason = rs.getString(1);
                    Instant unmuteTime = rs.getObject(2, Timestamp.class).toInstant();

                    consumer.accept(Optional.of(new MuteDto(reason, unmuteTime)));
                } else {
                    consumer.accept(Optional.empty());
                }
            }

            return true;
        }, () -> consumer.accept(Optional.empty()));
    }

    public static void removeDiscordUser(@NotNull UUID uuid) {
        runTask(() -> {
            //noinspection DataFlowIssue
            removeDiscordUser.setObject(1, uuid);
            removeDiscordUser.executeUpdate();

            return true;
        }, null);
    }

    public static void saveAndClose() {
        EXECUTORS.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            EXECUTORS.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        closeConnection();
    }

    private static void checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                openConnection();
            } else if (!connection.isValid(5)) { // Timeout 5 second
                log.warn("Connection to DB is invalid");

                closeConnection();
                openConnection();
            }
        } catch (SQLException throwable) {
            log.warn("Fail check connection to DB", throwable);

            closeConnection();
            openConnection();
        }
    }

    private static void closeConnection() {
        log.info("Close connection to DB");

        close(readUserPrivileges);
        close(botLogin);
        close(readUser);
        close(readUsers);
        close(writeUser);
        close(readMutePlayer);
        close(removeDiscordUser);
        close(connection);
    }

    private static void close(AutoCloseable closeable) {
        if (closeable == null) return;

        try {
            closeable.close();
        } catch (Exception e) {
            log.warn("Fail close to DB:", e);
        }
    }

    private static void runTask(Callable<Boolean> task, @Nullable Runnable fail) {
        EXECUTORS.submit(() -> {
            for (int i = 0; i < MAX_RETRIES; i++) {
                checkConnection();

                try {
                    boolean result = task.call();
                    if (result) return;

                    if (fail != null) {
                        fail.run();
                    }
                } catch (Exception e) {
                    log.warn("Fail run DB task", e);
                }
            }

            if (fail != null) {
                fail.run();
            }

            log.warn("Max retry run DB task");
        });
    }
}
