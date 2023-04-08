package com.Jeka8833.TNTServer.dataBase;

import com.Jeka8833.TNTServer.TNTUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TNTClientDBManager {

    private static final Logger LOGGER = LogManager.getLogger(TNTClientDBManager.class);
    private static final DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Map<UUID, UserQuire> USERS = new ConcurrentHashMap<>();

    public static void init() {
        // scheduleWithFixedDelay crashes
        var infinityThread = new Thread(() -> {
            while (true) {
                try {
                    forceWrite();
                    forceRead();

                    TNTUser.uuid2User.values().removeIf(TNTUser::isUserDead);

                    USERS.values().removeIf(userQuire -> !userQuire.isNeed());

                    Thread.sleep(5000);
                } catch (Exception e) {
                    LOGGER.warn("DB Tick error:", e);
                }
            }
        });
        infinityThread.setDaemon(true);
        infinityThread.start();
    }

    private static void forceRead() {
        final List<UserQuire> needRead = USERS.values().stream().filter(UserQuire::isRead).toList();
        if (!needRead.isEmpty()) {
            try {
                read(needRead);
            } catch (Exception e) {
                LOGGER.warn("Block read error, transition to single read method");
                for (UserQuire quire : needRead) {
                    try {
                        read(Collections.singletonList(quire));
                    } catch (SQLException ex) {
                        LOGGER.warn("User Read Error: " + quire.user + ", error: ", ex);
                    }
                }
            }
        }
    }

    public static void forceWrite() {
        final List<UserQuire> needWrite = USERS.values().stream().filter(UserQuire::isWrite).toList();
        if (!needWrite.isEmpty()) {
            try {
                write(needWrite);
            } catch (Exception e) {
                LOGGER.warn("Block write error, transition to single write method");
                for (UserQuire quire : needWrite) {
                    try {
                        write(Collections.singletonList(quire));
                    } catch (SQLException ex) {
                        LOGGER.warn("User Write Error: " + quire.user + ", error: ", ex);
                    }
                }
            }
        }
    }

    @NotNull
    public static Set<Map.Entry<UUID, TNTUser>> getLoadedUsers() {
        return TNTUser.uuid2User.entrySet();
    }

    @Nullable
    public static TNTUser getUser(@NotNull UUID uuid) {
        return TNTUser.uuid2User.get(uuid);
    }

    public static void readOrCashUser(@NotNull UUID uuid, final UserCallback callback) {
        TNTUser user = getUser(uuid);
        if (user == null) {
            readUser(uuid, callback);
        } else {
            callback.update(user);
        }
    }

    public static void readOrCashUser(final List<UUID> users, final UsersCallback callbackList, final boolean fillDefault) {
        final List<UUID> needRequest = new ArrayList<>();
        final List<TNTUser> returnUsers = new ArrayList<>();
        for (UUID uuid : users) {
            TNTUser user = getUser(uuid);
            if (user == null) {
                needRequest.add(uuid);
            } else {
                returnUsers.add(user);
            }
        }
        if (needRequest.isEmpty()) {
            callbackList.update(returnUsers);
        } else {
            readUsers(needRequest, tntUsers -> {
                returnUsers.addAll(tntUsers);
                callbackList.update(returnUsers);
            }, fillDefault);
        }
    }

    private static void read(final List<UserQuire> userList) throws SQLException {
        if (userList == null || userList.isEmpty()) return;

        var joiner = new StringJoiner("','",
                "SELECT * FROM \"TC_Players\" WHERE \"user\" IN ('", "')");

        for (UserQuire quire : userList) joiner.add(quire.user.toString());

        DatabaseManager.db.checkConnect();
        try (ResultSet resultSet = DatabaseManager.db.statement.executeQuery(joiner.toString())) { // Throw force exit
            try {
                while (resultSet.next()) {
                    var tntUser = new TNTUser(resultSet.getObject("user", UUID.class),
                            resultSet.getObject("key", UUID.class), resultSet.getString("version"));

                    final Date date = resultSet.getTimestamp("timeLogin");
                    tntUser.timeLogin = date == null ? System.currentTimeMillis() : date.getTime();
                    tntUser.forceBlock = resultSet.getLong("blockModules");
                    tntUser.donate = resultSet.getByte("donate");
                    tntUser.status = resultSet.getByte("status");


                    for (UserQuire quire : userList) {
                        if (quire.user.equals(tntUser.user)) {
                            quire.callRead(tntUser);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Fail read data");
            }
        }

        for (UserQuire quire : userList) quire.callRead(null);
    }

    private static void write(final List<UserQuire> userList) throws SQLException {
        if (userList == null || userList.isEmpty()) return;

        var sqlRequest = new StringJoiner(",", "INSERT INTO \"TC_Players\" (\"user\", \"key\", " +
                "\"version\", \"timeLogin\", \"blockModules\", \"donate\", \"status\") VALUES ",
                " ON CONFLICT (\"user\") DO UPDATE SET \"key\" = EXCLUDED.\"key\", " +
                        "\"version\" = EXCLUDED.\"version\", \"timeLogin\" = EXCLUDED.\"timeLogin\", " +
                        "\"blockModules\" = EXCLUDED.\"blockModules\", \"donate\" = EXCLUDED.\"donate\", " +
                        "\"status\" = EXCLUDED.\"status\"");
        for (UserQuire quire : userList) {
            TNTUser user = getUser(quire.user);
            if (user == null) continue;

            sqlRequest.add("('" + user.user + "'," + (user.key == null ? "NULL" : "'" + user.key + "'") + ",'" + user.version + "','"
                    + FORMATTER.format(new Date(user.timeLogin)) + "'," + user.forceBlock
                    + "," + user.donate + "," + user.status + ")");
        }

        DatabaseManager.db.checkConnect();
        DatabaseManager.db.statement.executeUpdate(sqlRequest.toString()); // Maybe throw an exception

        for (UserQuire quire : userList) quire.callWrite(getUser(quire.user));
    }

    /**
     * @param callbackList May not return some users if there is a failure in the database or other circumstances.
     */
    private static void readUsers(final List<UUID> users, final UsersCallback callbackList, final boolean fillDefault) {
        if (callbackList == null) {
            for (UUID uuid : users)
                readUser(uuid, null);
        } else {
            final AtomicInteger readCount = new AtomicInteger();
            final List<TNTUser> returnUsers = new ArrayList<>(users.size());
            for (UUID uuid : users) {
                readUser(uuid, tntUser -> {
                    readCount.getAndIncrement();

                    if (tntUser != null) // Check timeout
                        returnUsers.add(tntUser);
                    else if (fillDefault)
                        returnUsers.add(new TNTUser(uuid, null, null));

                    if (users.size() <= readCount.get())
                        callbackList.update(returnUsers);
                });
            }
        }
    }

    /**
     * @param callbackList May not return some users if there is a failure in the database or other circumstances.
     */
    public static void writeUsers(final List<UUID> users, final UsersCallback callbackList) {
        if (callbackList == null) {
            for (UUID uuid : users)
                writeUser(uuid, null);
        } else {
            final AtomicInteger readCount = new AtomicInteger();
            final List<TNTUser> returnUsers = new ArrayList<>(users.size());
            for (UUID uuid : users) {
                writeUser(uuid, tntUser -> {
                    readCount.getAndIncrement();

                    if (tntUser != null)  // Check timeout
                        returnUsers.add(tntUser);
                    else if (users.size() <= readCount.get())
                        callbackList.update(returnUsers);
                });
            }
        }
    }

    /**
     * @param callback Will return null in callback if the database response has timed out or other circumstances.
     */
    private static void readUser(@NotNull UUID uuid, @Nullable UserCallback callback) {
        final UserQuire quire = USERS.getOrDefault(uuid, new UserQuire(uuid));
        quire.needRead();
        if (callback != null) quire.addReadCallback(callback);
        USERS.put(quire.user, quire);
    }

    /**
     * @param callback Will return null in callback if the database response has timed out or other circumstances.
     */
    public static void writeUser(@NotNull UUID uuid, @Nullable UserCallback callback) {
        final UserQuire quire = USERS.getOrDefault(uuid, new UserQuire(uuid));
        quire.needWrite();
        if (callback != null) quire.addWriteCallback(callback);
        USERS.put(quire.user, quire);
    }

    private static class UserQuire {
        private final @NotNull UUID user;
        private final List<UserCallback> readCallbackList = new ArrayList<>();
        private final List<UserCallback> writeCallbackList = new ArrayList<>();
        private long timeout = System.currentTimeMillis() + 20_000; // Wait 30 second

        private boolean needRead;
        private boolean needWrite;

        public UserQuire(@NotNull UUID user) {
            this.user = user;
        }

        public boolean isNeed() {
            if (!needRead && !needWrite) return false;

            if (timeout < System.currentTimeMillis()) {
                if (needRead) {
                    LOGGER.warn("Timeout read information, user: " + user + " user in query: " + isRead());
                }
                if (needWrite) {
                    LOGGER.warn("Timeout write information, user: " + user + " user in query: " + isWrite());
                }

                callRead(null);
                callWrite(null);
                return false;
            }
            return true;
        }

        /*
         * Write always in priority
         * */
        public boolean isWrite() {
            return needWrite;
        }

        /*
         * Read is not priority
         * */
        public boolean isRead() {
            return needRead && !isWrite();
        }

        public void callRead(@Nullable TNTUser tntUser) {
            for (UserCallback callback : readCallbackList) {
                try {
                    callback.update(tntUser);
                } catch (Exception e) {
                    LOGGER.warn("Read callback throw exception", e);
                }
            }
            readCallbackList.clear();

            needRead = false;
        }

        public void callWrite(@Nullable TNTUser tntUser) {
            for (UserCallback callback : writeCallbackList) {
                try {
                    callback.update(tntUser);
                } catch (Exception e) {
                    LOGGER.warn("Write callback throw exception", e);
                }
            }
            writeCallbackList.clear();

            needWrite = false;
        }

        /*
         * Throw null in callback when timeout
         * */
        public void addReadCallback(@NotNull UserCallback callback) {
            readCallbackList.add(callback);
        }

        /*
         * Throw null in callback when timeout
         * */
        public void addWriteCallback(@NotNull UserCallback callback) {
            writeCallbackList.add(callback);
        }

        public void needRead() {
            timeout = System.currentTimeMillis() + 20_000;
            this.needRead = true;
        }

        public void needWrite() {
            timeout = System.currentTimeMillis() + 20_000;
            this.needWrite = true;
        }
    }

    public interface UserCallback {
        void update(@Nullable TNTUser tntUser);
    }

    public interface UsersCallback {
        void update(@NotNull List<TNTUser> tntUsers);
    }
}
