package com.jeka8833.tntserver.database.managers;

import com.jeka8833.tntserver.database.Player;
import com.jeka8833.tntserver.database.PlayersDatabase;
import com.jeka8833.tntserver.database.User;
import com.jeka8833.tntserver.database.storage.TNTPlayerStorage;
import com.jeka8833.tntserver.packet.callback.CallbackManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TNTClientDBManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TNTClientDBManager.class);
    private static final Map<UUID, UserQuire> USER_REQUEST_LIST = new ConcurrentHashMap<>();
    private static PreparedStatement preparedWrite;

    public static void init() {
        try {
            preparedWrite = DatabaseManager.db.connection.prepareStatement(
                    "INSERT INTO \"TC_Players\" (\"user\", \"version\", \"timeLogin\", \"blockModules\") " +
                            "VALUES (?,?,CURRENT_TIMESTAMP,?) ON CONFLICT (\"user\") DO UPDATE SET " +
                            "\"version\" = EXCLUDED.\"version\", \"timeLogin\" = EXCLUDED.\"timeLogin\"," +
                            " \"blockModules\" = EXCLUDED.\"blockModules\"");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        var infinityThread = watchDogThread();
        infinityThread.start();
    }

    private static @NotNull Thread watchDogThread() {
        var infinityThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    forceWrite();
                    forceRead();

                    PlayersDatabase.clearInactivePeople();
                    USER_REQUEST_LIST.values().removeIf(userQuire -> !userQuire.isNeed());
                    CallbackManager.checkTimeout();

                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    return;
                } catch (Exception e) {
                    LOGGER.warn("DB Tick error:", e);
                }
            }
        });
        infinityThread.setDaemon(true);
        return infinityThread;
    }

    private static void forceRead() {
        List<UserQuire> needRead = USER_REQUEST_LIST.values().stream().filter(UserQuire::isRead).toList();

        if (!needRead.isEmpty()) {
            try {
                read(needRead);
            } catch (Exception e) {
                for (UserQuire quire : needRead) {
                    try {
                        read(Collections.singletonList(quire));
                    } catch (Exception ex) {
                        LOGGER.warn("Read DB error for {}", quire.user, ex);
                    }
                }
            }
        }
    }

    private static void read(@NotNull Iterable<UserQuire> userList) throws Exception {
        var joiner = new StringJoiner("','",
                "SELECT\"user\",\"version\",\"blockModules\",\"donate\"FROM\"TC_Players\"WHERE\"user\"IN('",
                "')");

        for (UserQuire quire : userList) joiner.add(quire.user.toString());

        DatabaseManager.db.checkConnect();
        try (ResultSet resultSet = DatabaseManager.db.statement.executeQuery(joiner.toString())) { // Throw force exit
            while (resultSet.next()) {
                User user = PlayersDatabase.getOrCreate(resultSet.getObject("user", UUID.class));
                if (user instanceof Player player) {
                    if (player.tntPlayerInfo == null) player.tntPlayerInfo = new TNTPlayerStorage();
                    player.tntPlayerInfo.version = resultSet.getString("version");
                    player.tntPlayerInfo.forceBlock = resultSet.getLong("blockModules");
                    player.tntPlayerInfo.donate = resultSet.getByte("donate");
                }
            }
        }

        for (UserQuire quire : userList) quire.callRead();
    }

    public static void forceWrite() {
        List<UserQuire> needWrite = USER_REQUEST_LIST.values().stream().filter(UserQuire::isWrite).toList();

        if (!needWrite.isEmpty()) {
            try {
                write(needWrite);
            } catch (Exception e) {
                for (UserQuire quire : needWrite) {
                    try {
                        write(Collections.singletonList(quire));
                    } catch (Exception ex) {
                        LOGGER.warn("Write DB error for {}", quire.user, ex);
                    }
                }
            }
        }
    }

    private static void write(@NotNull Iterable<UserQuire> userList) throws Exception {
        for (int i = 0; i < 2; i++) {
            for (UserQuire quire : userList) {
                User user = PlayersDatabase.getUser(quire.user);
                if (user instanceof Player player) {
                    if (player.tntPlayerInfo == null) continue;

                    preparedWrite.setObject(1, player.uuid);
                    preparedWrite.setString(2, player.tntPlayerInfo.version);
                    preparedWrite.setLong(3, player.tntPlayerInfo.forceBlock);

                    preparedWrite.addBatch();
                }
            }

            DatabaseManager.db.checkConnect();
            try {
                int[] results = preparedWrite.executeBatch();
                for (int result : results) {
                    if (result == Statement.EXECUTE_FAILED) throw new NullPointerException("Execution failed.");
                }

                for (UserQuire quire : userList) quire.callWrite();

                return;
            } catch (PSQLException e) {
                DatabaseManager.db.close();
                DatabaseManager.db.connect();

                preparedWrite = DatabaseManager.db.connection.prepareStatement(
                        "INSERT INTO \"TC_Players\" (\"user\", \"version\", \"timeLogin\", \"blockModules\") " +
                                "VALUES (?,?,CURRENT_TIMESTAMP,?) ON CONFLICT (\"user\") DO UPDATE SET " +
                                "\"version\" = EXCLUDED.\"version\", \"timeLogin\" = EXCLUDED.\"timeLogin\"," +
                                " \"blockModules\" = EXCLUDED.\"blockModules\"");
            }
        }
    }

    /**
     * @param callback Will return null in callback if the database response has timed out or other circumstances.
     */
    public static void readUser(@NotNull UUID uuid, @Nullable Consumer<@Nullable Player> callback) {
        if (uuid.version() != 4) {
            if (callback != null) callback.accept(null);
            return;
        }

        UserQuire quire = USER_REQUEST_LIST.computeIfAbsent(uuid, UserQuire::new);
        quire.addReadCallback(callback);
    }

    public static void readOrCashUser(@NotNull UUID uuid, Consumer<@Nullable Player> callback) {
        User user = PlayersDatabase.getUser(uuid);
        if (user instanceof Player player) {
            callback.accept(player);
        } else {
            readUser(uuid, callback);
        }
    }

    public static void readOrCashUsers(@NotNull List<UUID> users,
                                       @Nullable Consumer<@NotNull Player @Nullable []> callbackList,
                                       boolean createConstructor) {
        if (callbackList == null) {
            for (UUID uuid : users) readOrCashUser(uuid, null);
        } else {
            AtomicInteger answerCount = new AtomicInteger(users.size());
            Player[] returnUsers = new Player[users.size()];
            for (int i = 0; i < returnUsers.length; i++) {
                final int finalI = i;
                UUID uuid = users.get(i);
                readOrCashUser(uuid, tntUser -> {
                    returnUsers[finalI] = createConstructor && tntUser == null ? new Player(uuid) : tntUser;

                    if (answerCount.decrementAndGet() <= 0) callbackList.accept(returnUsers);
                });
            }
        }
    }

    /**
     * @param callback Will return null in callback if the database response has timed out or other circumstances.
     */
    public static void writeUser(@NotNull UUID uuid, @Nullable Consumer<@Nullable Player> callback) {
        if (uuid.version() != 4) {
            if (callback != null) callback.accept(null);
            return;
        }

        UserQuire quire = USER_REQUEST_LIST.computeIfAbsent(uuid, UserQuire::new);
        quire.addWriteCallback(callback);
    }

    private static class UserQuire {
        private static final long TIME_TO_CANCEL_ALL_OPERATIONS = TimeUnit.SECONDS.toNanos(20);

        private final @NotNull UUID user;
        private final Queue<Consumer<@Nullable Player>> readCallbackList = new ConcurrentLinkedQueue<>();
        private final Queue<Consumer<@Nullable Player>> writeCallbackList = new ConcurrentLinkedQueue<>();
        private long startTime = System.nanoTime();

        public UserQuire(@NotNull UUID user) {
            this.user = user;
        }

        public boolean isNeed() {
            if (readCallbackList.isEmpty() && writeCallbackList.isEmpty()) return false;

            if (System.nanoTime() - startTime > TIME_TO_CANCEL_ALL_OPERATIONS) {
                if (readCallbackList.isEmpty()) {
                    LOGGER.warn("Timeout read information, user: {} user in query: {}", user, isRead());
                }
                if (writeCallbackList.isEmpty()) {
                    LOGGER.warn("Timeout write information, user: {} user in query: {}", user, isWrite());
                }

                callRead();
                callWrite();
                return false;
            }
            return true;
        }

        /**
         * Write always in priority
         */
        public boolean isWrite() {
            return !writeCallbackList.isEmpty();
        }

        /**
         * Read is not priority
         */
        public boolean isRead() {
            return !readCallbackList.isEmpty() && !isWrite();
        }

        public void callRead() {
            call(readCallbackList);
        }

        public void callWrite() {
            call(writeCallbackList);
        }

        private void call(@NotNull Queue<@NotNull Consumer<@Nullable Player>> queue) {
            Consumer<@Nullable Player> callback;
            while ((callback = queue.poll()) != null) {
                try {
                    User userStorage = PlayersDatabase.getUser(user);
                    if (userStorage instanceof Player player) {
                        callback.accept(player);
                    } else {
                        callback.accept(null);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Call callback throw exception", e);
                }
            }
        }

        /**
         * Throw null in callback when timeout
         */
        public void addReadCallback(@Nullable Consumer<@Nullable Player> callback) {
            if (callback != null) {
                readCallbackList.offer(callback);
            } else if (readCallbackList.isEmpty()) {
                readCallbackList.offer(tntUser -> {
                });
            }

            startTime = System.nanoTime();
        }

        /**
         * Throw null in callback when timeout
         */
        public void addWriteCallback(@Nullable Consumer<@Nullable Player> callback) {
            if (callback != null) {
                writeCallbackList.offer(callback);
            } else if (writeCallbackList.isEmpty()) {
                writeCallbackList.offer(tntUser -> {
                });
            }

            startTime = System.nanoTime();
        }
    }
}
