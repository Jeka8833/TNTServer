package com.Jeka8833.TNTServer.dataBase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final Logger LOGGER = LogManager.getLogger(DatabaseManager.class);

    private Connection connection;
    public Statement statement;

    private final String host;
    private final String userName;
    private final String password;

    public DatabaseManager(String host, String userName, String password) {
        this.host = host;
        this.userName = userName;
        this.password = password;
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection(host, userName, password);
            statement = connection.createStatement();
        } catch (Exception e) {
            LOGGER.error("Fail connect to DB:", e);
        }
    }

    public void checkConnect() {
        try {
            if (!connection.isValid(5)) { // Timeout 5 second
                close();
                connect();
            }
        } catch (SQLException throwable) {
            close();
            connect();
        }
    }

    public void close() {
        try {
            if (connection != null)
                connection.close();
            if (statement != null)
                statement.close();
        } catch (Exception e) {
            LOGGER.warn("Fail close to DB:", e);

        }
    }

    public static DatabaseManager db;

    public static void initConnect(final String ipAndPort, final String user, final String password) {
        if (db != null) return; // Re-init protection

        db = new DatabaseManager("jdbc:postgresql://" + ipAndPort + "/", user, password);
        db.connect();
    }
}
