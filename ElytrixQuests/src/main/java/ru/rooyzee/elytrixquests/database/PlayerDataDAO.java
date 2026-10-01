package ru.rooyzee.elytrixquests.database;

import org.bukkit.Bukkit;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.data.PlayerQuestData;
import ru.rooyzee.elytrixquests.data.QuestEntry;
import ru.rooyzee.elytrixquests.data.QuestStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataDAO {

    private final Main plugin;
    private final DatabaseManager db;

    public PlayerDataDAO(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public CompletableFuture<PlayerQuestData> load(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerQuestData data = new PlayerQuestData(uuid);

            if (!db.isConnected()) {
                plugin.getLogger().severe("БД не подключена, данные игрока " + name + " не загружены!");
                return data;
            }

            Connection conn = db.getConnection();
            synchronized (db) {
                try {
                    // Upsert игрока
                    PreparedStatement upsert = conn.prepareStatement(
                            "INSERT INTO players(uuid, name, last_seen) VALUES(?,?,?) " +
                                    "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, last_seen=excluded.last_seen");
                    upsert.setString(1, uuid.toString());
                    upsert.setString(2, name);
                    upsert.setLong(3, System.currentTimeMillis());
                    upsert.executeUpdate();
                    upsert.close();

                    // Загрузка прогресса квестов
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT level_id, quest_id, status, progress, cooldown_until FROM quest_progress WHERE uuid=?");
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        int level = rs.getInt("level_id");
                        int quest = rs.getInt("quest_id");
                        QuestStatus status;
                        try {
                            status = QuestStatus.valueOf(rs.getString("status"));
                        } catch (IllegalArgumentException e) {
                            status = QuestStatus.AVAILABLE;
                        }
                        int progress = rs.getInt("progress");
                        data.setEntry(level, quest, new QuestEntry(status, progress));
                        data.setCooldownUntil(level, quest, rs.getLong("cooldown_until"));
                    }
                    rs.close();
                    ps.close();

                    // Загрузка мега-наград
                    PreparedStatement ps2 = conn.prepareStatement(
                            "SELECT level_id, mega_claimed FROM level_data WHERE uuid=?");
                    ps2.setString(1, uuid.toString());
                    ResultSet rs2 = ps2.executeQuery();
                    while (rs2.next()) {
                        data.setMegaClaimed(rs2.getInt("level_id"), rs2.getInt("mega_claimed") == 1);
                    }
                    rs2.close();
                    ps2.close();

                } catch (SQLException e) {
                    plugin.getLogger().severe("Ошибка загрузки данных " + name + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return data;
        });
    }

    public void saveProgress(UUID uuid, int level, int quest, QuestStatus status, int progress) {
        saveProgress(uuid, level, quest, status, progress, 0L);
    }

    public void saveProgress(UUID uuid, int level, int quest, QuestStatus status, int progress, long cooldownUntil) {
        if (!db.isConnected()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = db.getConnection();
            synchronized (db) {
                try {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO quest_progress(uuid, level_id, quest_id, status, progress, cooldown_until) VALUES(?,?,?,?,?,?) " +
                                    "ON CONFLICT(uuid, level_id, quest_id) DO UPDATE SET status=excluded.status, progress=excluded.progress, cooldown_until=excluded.cooldown_until");
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, level);
                    ps.setInt(3, quest);
                    ps.setString(4, status.name());
                    ps.setInt(5, progress);
                    ps.setLong(6, cooldownUntil);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Ошибка сохранения прогресса: " + e.getMessage());
                }
            }
        });
    }

    public void saveMega(UUID uuid, int level, boolean claimed) {
        if (!db.isConnected()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = db.getConnection();
            synchronized (db) {
                try {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO level_data(uuid, level_id, mega_claimed) VALUES(?,?,?) " +
                                    "ON CONFLICT(uuid, level_id) DO UPDATE SET mega_claimed=excluded.mega_claimed");
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, level);
                    ps.setInt(3, claimed ? 1 : 0);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Ошибка сохранения мега-награды: " + e.getMessage());
                }
            }
        });
    }

    public void resetPlayer(UUID uuid) {
        if (!db.isConnected()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = db.getConnection();
            synchronized (db) {
                try {
                    PreparedStatement ps1 = conn.prepareStatement(
                            "DELETE FROM quest_progress WHERE uuid=?");
                    ps1.setString(1, uuid.toString());
                    ps1.executeUpdate();
                    ps1.close();

                    PreparedStatement ps2 = conn.prepareStatement(
                            "DELETE FROM level_data WHERE uuid=?");
                    ps2.setString(1, uuid.toString());
                    ps2.executeUpdate();
                    ps2.close();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Ошибка сброса данных: " + e.getMessage());
                }
            }
        });
    }

    public void shutdown() {
        // Ничего не нужно — соединение закрывает DatabaseManager
    }
}