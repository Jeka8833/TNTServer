package com.Jeka8833.TNTServer.dataBase;

import com.Jeka8833.TNTServer.TNTUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TNTClientDBManager {

    private static final Logger LOGGER = LogManager.getLogger(TNTClientDBManager.class);
    private static final DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Map<UUID, UserQuire> USER_REQUEST_LIST = new ConcurrentHashMap<>();
    private static final Map<UUID, TNTUser> uuid2User = new ConcurrentHashMap<>();

    public static void init() {
        var infinityThread = new Thread(() -> {
            while (true) {
                try {
                    forceWrite();
                    forceRead();

                    uuid2User.values().removeIf(TNTUser::isUserDead);

                    USER_REQUEST_LIST.values().removeIf(userQuire -> !userQuire.isNeed());

                    Thread.sleep(3000);
                } catch (Exception e) {
                    LOGGER.warn("DB Tick error:", e);
                }
            }
        });
        infinityThread.setDaemon(true);
        infinityThread.start();
    }

    private static void forceRead() {
        final List<UserQuire> needRead = USER_REQUEST_LIST.values().stream().filter(UserQuire::isRead).toList();
        if (!needRead.isEmpty()) {
            try {
                read(needRead);
            } catch (Exception e) {
                LOGGER.warn("Block read error, transition to single read method");
                for (UserQuire quire : needRead) {
                    try {
                        read(Collections.singletonList(quire));
                    } catch (Exception ex) {
                        LOGGER.warn("User Read Error: " + quire.user + ", error: ", ex);
                    }
                }
            }
        }
    }

    private static void read(@NotNull Collection<UserQuire> userList) throws Exception {
        var joiner = new StringJoiner("','",
                "SELECT * FROM \"TC_Players\" WHERE \"user\" IN ('", "')");

        for (UserQuire quire : userList) joiner.add(quire.user.toString());

        DatabaseManager.db.checkConnect();
        try (ResultSet resultSet = DatabaseManager.db.statement.executeQuery(joiner.toString())) { // Throw force exit
            while (resultSet.next()) {
                TNTUser user = getOrCreate(resultSet.getObject("user", UUID.class));
                user.version = resultSet.getString("version");

                final Date date = resultSet.getTimestamp("timeLogin");
                user.timeLogin = date == null ? System.currentTimeMillis() : date.getTime();

                user.forceBlock = resultSet.getLong("blockModules");
                user.donate = resultSet.getByte("donate");
            }
        }

        for (UserQuire quire : userList) quire.callRead();
    }

    public static void forceWrite() {
        final List<UserQuire> needWrite = USER_REQUEST_LIST.values().stream().filter(UserQuire::isWrite).toList();
        if (!needWrite.isEmpty()) {
            try {
                write(needWrite);
            } catch (Exception e) {
                LOGGER.warn("Block write error, transition to single write method");
                for (UserQuire quire : needWrite) {
                    try {
                        write(Collections.singletonList(quire));
                    } catch (Exception ex) {
                        LOGGER.warn("User Write Error: " + quire.user + ", error: ", ex);
                    }
                }
            }
        }
    }

    private static void write(@NotNull Collection<UserQuire> userList) throws Exception {
        var sqlRequest = new StringJoiner(",",
                "INSERT INTO \"TC_Players\" (\"user\", \"version\", " +
                        "\"timeLogin\", \"blockModules\", \"donate\") VALUES ",

                " ON CONFLICT (\"user\") DO UPDATE SET \"version\" = EXCLUDED.\"version\", " +
                        "\"timeLogin\" = EXCLUDED.\"timeLogin\", \"blockModules\" = EXCLUDED.\"blockModules\", " +
                        "\"donate\" = EXCLUDED.\"donate\"");
        for (UserQuire quire : userList) {
            TNTUser user = getUser(quire.user);
            if (user == null) continue;

            sqlRequest.add("('" + user.uuid + "','" + user.version + "','"
                    + FORMATTER.format(new Date(user.timeLogin)) + "'," + user.forceBlock + "," + user.donate + ")");
        }

        DatabaseManager.db.checkConnect();
        DatabaseManager.db.statement.executeUpdate(sqlRequest.toString()); // Maybe throw an exception

        for (UserQuire quire : userList) quire.callWrite();
    }

    /**
     * @param callback Will return null in callback if the database response has timed out or other circumstances.
     */
    private static void readUser(@NotNull UUID uuid, @Nullable UserCallback callback) {
        if (uuid.version() != 4) {
            if (callback != null) callback.update(null);
            return;
        }

        UserQuire quire = USER_REQUEST_LIST.computeIfAbsent(uuid, UserQuire::new);
        quire.addReadCallback(callback);
    }

    public static void readOrCashUser(@NotNull UUID uuid, final UserCallback callback) {
        TNTUser user = getUser(uuid);
        if (user == null) {
            readUser(uuid, callback);
        } else {
            callback.update(user);
        }
    }

    public static void readOrCashUsers(@NotNull List<UUID> users, @Nullable UsersCallback callbackList,
                                       boolean createConstructor) {
        if (callbackList == null) {
            for (UUID uuid : users) readOrCashUser(uuid, null);
        } else {
            AtomicInteger answerCount = new AtomicInteger(users.size());
            TNTUser[] returnUsers = new TNTUser[users.size()];
            for (int i = 0; i < returnUsers.length; i++) {
                final int finalI = i;
                UUID uuid = users.get(i);
                readOrCashUser(uuid, tntUser -> {
                    returnUsers[finalI] = createConstructor && tntUser == null ? new TNTUser(uuid) : tntUser;

                    if (answerCount.decrementAndGet() <= 0) callbackList.update(returnUsers);
                });
            }
        }
    }

    /**
     * @param callback Will return null in callback if the database response has timed out or other circumstances.
     */
    public static void writeUser(@NotNull UUID uuid, @Nullable UserCallback callback) {
        if (uuid.version() != 4) {
            if (callback != null) callback.update(null);
            return;
        }

        UserQuire quire = USER_REQUEST_LIST.computeIfAbsent(uuid, UserQuire::new);
        quire.addWriteCallback(callback);
    }

    @NotNull
    public static TNTUser getOrCreate(@NotNull UUID uuid) {
        TNTUser user = uuid2User.computeIfAbsent(uuid, TNTUser::new);
        user.heartBeat();

        return user;
    }

    @Nullable
    public static TNTUser getUser(@NotNull UUID uuid) {
        TNTUser user = uuid2User.get(uuid);
        if (user != null) user.heartBeat();

        return user;
    }

    private static class UserQuire {
        private final @NotNull UUID user;
        private final Queue<UserCallback> readCallbackList = new ConcurrentLinkedQueue<>();
        private final Queue<UserCallback> writeCallbackList = new ConcurrentLinkedQueue<>();
        private long readWriteTimeout = System.currentTimeMillis() + 20_000; // Wait 20 second

        public UserQuire(@NotNull UUID user) {
            this.user = user;
        }

        public boolean isNeed() {
            if (readCallbackList.isEmpty() && writeCallbackList.isEmpty()) return false;

            if (readWriteTimeout < System.currentTimeMillis()) {
                if (readCallbackList.isEmpty()) {
                    LOGGER.warn("Timeout read information, user: " + user + " user in query: " + isRead());
                }
                if (writeCallbackList.isEmpty()) {
                    LOGGER.warn("Timeout write information, user: " + user + " user in query: " + isWrite());
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
            UserCallback callback;
            while ((callback = readCallbackList.poll()) != null) {
                try {
                    callback.update(getUser(user));
                } catch (Exception e) {
                    LOGGER.warn("Read callback throw exception", e);
                }
            }
        }

        public void callWrite() {
            UserCallback callback;
            while ((callback = writeCallbackList.poll()) != null) {
                try {
                    callback.update(getUser(user));
                } catch (Exception e) {
                    LOGGER.warn("Read callback throw exception", e);
                }
            }
        }

        /**
         * Throw null in callback when timeout
         */
        public void addReadCallback(@Nullable UserCallback callback) {
            if (callback != null) {
                readCallbackList.offer(callback);
            } else if (readCallbackList.isEmpty()) {
                readCallbackList.offer(tntUser -> {
                });
            }
            readWriteTimeout = System.currentTimeMillis() + 20_000;
        }

        /**
         * Throw null in callback when timeout
         */
        public void addWriteCallback(@Nullable UserCallback callback) {
            if (callback != null) {
                writeCallbackList.offer(callback);
            } else if (writeCallbackList.isEmpty()) {
                writeCallbackList.offer(tntUser -> {
                });
            }

            readWriteTimeout = System.currentTimeMillis() + 20_000;
        }
    }

    public interface UserCallback {
        void update(@Nullable TNTUser tntUser);
    }

    public interface UsersCallback {
        void update(@NotNull TNTUser @Nullable [] tntUsers);
    }
}
