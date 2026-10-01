package ru.rooyzee.elytrixquests.database;

import ru.rooyzee.elytrixquests.Main;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final Main plugin;
    private Connection connection;
    private final Object lock = new Object();

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "database.db");

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
            }
            createTables();
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite драйвер не найден! " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка подключения к SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "last_seen INTEGER)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS quest_progress (" +
                    "uuid TEXT, " +
                    "level_id INTEGER, " +
                    "quest_id INTEGER, " +
                    "status TEXT, " +
                    "progress INTEGER, " +
                    "PRIMARY KEY(uuid, level_id, quest_id))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS level_data (" +
                    "uuid TEXT, " +
                    "level_id INTEGER, " +
                    "mega_claimed INTEGER, " +
                    "PRIMARY KEY(uuid, level_id))");
        }
    }

    public Connection getConnection() {
        synchronized (lock) {
            try {
                if (connection == null || connection.isClosed()) {
                    connect();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка проверки соединения: " + e.getMessage());
            }
            return connection;
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void close() {
        synchronized (lock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка закрытия БД: " + e.getMessage());
            }
        }
    }
}