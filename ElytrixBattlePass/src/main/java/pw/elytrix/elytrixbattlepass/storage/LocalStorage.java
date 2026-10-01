package pw.elytrix.elytrixbattlepass.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.bukkit.entity.Player;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Quest;
import pw.elytrix.elytrixbattlepass.user.User;

public class LocalStorage implements DataStorage {
    private final ElytrixBattlePass plugin;
    private Connection connection;

    public LocalStorage(ElytrixBattlePass plugin) {
        this.plugin = plugin;
        connect();
        createTable();
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            File file = new File(plugin.getDataFolder(), "battlepass.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        } catch (Exception e) {
            plugin.getLogger().severe("SQLite error: " + e.getMessage());
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS battlepass_users (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    points INTEGER NOT NULL DEFAULT 0,
                    earned_today INTEGER NOT NULL DEFAULT 0,
                    earned_total INTEGER NOT NULL DEFAULT 0,
                    last_daily_reset_day INTEGER NOT NULL DEFAULT -1,
                    completed_tasks_total INTEGER NOT NULL DEFAULT 0,
                    completed_in_cycle INTEGER NOT NULL DEFAULT 0,
                    cycle_cooldown_until INTEGER NOT NULL DEFAULT 0,
                    current_task_id TEXT,
                    current_task_progress INTEGER NOT NULL DEFAULT 0,
                    current_task_started_at INTEGER NOT NULL DEFAULT 0,
                    used_task_ids TEXT NOT NULL DEFAULT ''
                );
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            plugin.getLogger().severe("SQLite table error: " + e.getMessage());
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
                ON CONFLICT(uuid) DO UPDATE SET
                name=excluded.name,
                points=excluded.points,
                earned_today=excluded.earned_today,
                earned_total=excluded.earned_total,
                last_daily_reset_day=excluded.last_daily_reset_day,
                completed_tasks_total=excluded.completed_tasks_total,
                completed_in_cycle=excluded.completed_in_cycle,
                cycle_cooldown_until=excluded.cycle_cooldown_until,
                current_task_id=excluded.current_task_id,
                current_task_progress=excluded.current_task_progress,
                current_task_started_at=excluded.current_task_started_at,
                used_task_ids=excluded.used_task_ids
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
            plugin.getLogger().severe("SQLite save error: " + e.getMessage());
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
            plugin.getLogger().severe("SQLite load error: " + e.getMessage());
        }

        return user;
    }

    @Override
    public synchronized void deleteUser(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM battlepass_users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("SQLite delete error: " + e.getMessage());
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
            plugin.getLogger().severe("SQLite close error: " + e.getMessage());
        }
    }
}