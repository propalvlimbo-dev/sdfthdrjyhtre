package pw.elytrix.elytrixbattlepass.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;
import org.bukkit.entity.Player;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Quest;
import pw.elytrix.elytrixbattlepass.user.User;

public class MySQLStorage implements DataStorage {
    private final ElytrixBattlePass plugin;
    private Connection connection;

    public MySQLStorage(ElytrixBattlePass plugin, String host, int port, String database, String username, String password) {
        this.plugin = plugin;
        connect(host, port, database, username, password);
        createTable();
    }

    private void connect(String host, int port, String database, String username, String password) {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("useSSL", "false");
            properties.setProperty("autoReconnect", "true");
            properties.setProperty("characterEncoding", "utf8");
            properties.setProperty("serverTimezone", "UTC");
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, properties);
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL error: " + e.getMessage());
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS battlepass_users (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(16) NOT NULL,
                    points INT NOT NULL DEFAULT 0,
                    earned_today INT NOT NULL DEFAULT 0,
                    earned_total INT NOT NULL DEFAULT 0,
                    last_daily_reset_day BIGINT NOT NULL DEFAULT -1,
                    completed_tasks_total INT NOT NULL DEFAULT 0,
                    completed_in_cycle INT NOT NULL DEFAULT 0,
                    cycle_cooldown_until BIGINT NOT NULL DEFAULT 0,
                    current_task_id VARCHAR(100),
                    current_task_progress INT NOT NULL DEFAULT 0,
                    current_task_started_at BIGINT NOT NULL DEFAULT 0,
                    used_task_ids TEXT NOT NULL
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL table error: " + e.getMessage());
        }
    }

    @Override
    public synchronized void saveUser(User user) {
        if (user == null) {
            return;
        }

        String sql = """
                INSERT INTO battlepass_users
                (uuid, name, points, earned_today, earned_total, last_daily_reset_day, completed_tasks_total, completed_in_cycle, cycle_cooldown_until, current_task_id, current_task_progress, current_task_started_at, used_task_ids)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                points = VALUES(points),
                earned_today = VALUES(earned_today),
                earned_total = VALUES(earned_total),
                last_daily_reset_day = VALUES(last_daily_reset_day),
                completed_tasks_total = VALUES(completed_tasks_total),
                completed_in_cycle = VALUES(completed_in_cycle),
                cycle_cooldown_until = VALUES(cycle_cooldown_until),
                current_task_id = VALUES(current_task_id),
                current_task_progress = VALUES(current_task_progress),
                current_task_started_at = VALUES(current_task_started_at),
                used_task_ids = VALUES(used_task_ids)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            Quest quest = user.getCurrentQuest();
            String taskId = null;
            int progress = 0;
            long startedAt = 0L;

            if (quest != null && quest.getTask() != null) {
                taskId = quest.getTaskId();
                progress = quest.getTask().getCompleted();
                startedAt = quest.getTask().getStartedAt();
            }

            ps.setString(1, user.getUuid().toString());
            ps.setString(2, user.getName());
            ps.setInt(3, user.getPoints());
            ps.setInt(4, user.getEarnedToday());
            ps.setInt(5, user.getEarnedTotal());
            ps.setLong(6, user.getLastDailyResetDay());
            ps.setInt(7, user.getCompletedTasksTotal());
            ps.setInt(8, user.getCompletedInCycle());
            ps.setLong(9, user.getCycleCooldownUntil());
            ps.setString(10, taskId);
            ps.setInt(11, progress);
            ps.setLong(12, startedAt);
            ps.setString(13, user.serializeUsedTaskIds());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL save error: " + e.getMessage());
        }
    }

    @Override
    public synchronized User loadUser(Player player) {
        String sql = "SELECT * FROM battlepass_users WHERE uuid = ?";
        User user = new User(player.getUniqueId(), player.getName());
        user.attachPlayer(player);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return user;
                }

                user.setName(rs.getString("name"));
                user.setPoints(rs.getInt("points"));
                user.setEarnedToday(rs.getInt("earned_today"));
                user.setEarnedTotal(rs.getInt("earned_total"));
                user.setLastDailyResetDay(rs.getLong("last_daily_reset_day"));
                user.setCompletedTasksTotal(rs.getInt("completed_tasks_total"));
                user.setCompletedInCycle(rs.getInt("completed_in_cycle"));
                user.setCycleCooldownUntil(rs.getLong("cycle_cooldown_until"));
                user.deserializeUsedTaskIds(rs.getString("used_task_ids"));

                String taskId = rs.getString("current_task_id");
                int progress = rs.getInt("current_task_progress");
                long startedAt = rs.getLong("current_task_started_at");

                if (taskId != null && !taskId.isBlank()) {
                    user.setCurrentQuest(plugin.getQuestManager().createQuest(taskId, progress, startedAt));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL load error: " + e.getMessage());
        }

        return user;
    }

    @Override
    public synchronized void deleteUser(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM battlepass_users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL delete error: " + e.getMessage());
        }
    }

    @Override
    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL close error: " + e.getMessage());
        }
    }
}