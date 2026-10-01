package pw.elytrix.elytrixbattlepass.guis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.user.User;
import pw.elytrix.elytrixbattlepass.utils.Colorize;

public class BossBarManager {
    private static final ElytrixBattlePass plugin = ElytrixBattlePass.getInstance();
    private static final Map<UUID, BossBar> activeBars = new HashMap<>();
    private static final Map<UUID, BukkitTask> removeTasks = new HashMap<>();
    private static final Set<UUID> persistentBars = new HashSet<>();
    private static BukkitTask updateTask;

    public static void startUpdater() {
        if (updateTask != null) return;
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new HashSet<>(persistentBars)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    persistentBars.remove(uuid);
                    continue;
                }
                User user = plugin.getUserManager().getUserFromCache(player);
                if (user == null || user.getCurrentQuest() == null || user.getCurrentQuest().getTask() == null) {
                    remove(player);
                    continue;
                }
                Task task = user.getCurrentQuest().getTask();
                if (!task.isActive()) {
                    remove(player);
                    continue;
                }
                updateBarText(player, task);
            }
        }, 20L, 20L);
    }

    public static void stopUpdater() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public static void showProgress(Player player, Task task) {
        cancelRemove(player);
        persistentBars.add(player.getUniqueId());
        updateBarText(player, task);
    }

    private static void updateBarText(Player player, Task task) {
        int current = Math.min(task.getCompleted(), task.getRequiredAmount());
        int required = task.getRequiredAmount();
        double progress = required > 0 ? Math.min(1.0, (double) current / required) : 0.0;

        String text = plugin.getConfigManager().getSettings().messages.general.bossbarProgress;
        text = text.replace("%description%", task.getDescription())
                .replace("%current%", String.valueOf(current))
                .replace("%required%", String.valueOf(required));

        BossBar bar = getOrCreate(player);
        bar.setTitle(Colorize.format(text));
        bar.setColor(BarColor.PINK);
        bar.setProgress(progress);
        bar.setVisible(true);
    }

    public static void showCompleted(Player player) {
        cancelRemove(player);
        persistentBars.remove(player.getUniqueId());

        String text = plugin.getConfigManager().getSettings().messages.general.bossbarCompleted;

        BossBar bar = getOrCreate(player);
        bar.setTitle(Colorize.format(text));
        bar.setColor(BarColor.GREEN);
        bar.setProgress(1.0);
        bar.setVisible(true);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> remove(player), 60L);
        removeTasks.put(player.getUniqueId(), task);
    }

    public static boolean isActive(Player player) {
        return persistentBars.contains(player.getUniqueId());
    }

    public static void remove(Player player) {
        cancelRemove(player);
        persistentBars.remove(player.getUniqueId());
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    private static BossBar getOrCreate(Player player) {
        BossBar bar = activeBars.get(player.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar("", BarColor.PINK, BarStyle.SOLID);
            bar.addPlayer(player);
            activeBars.put(player.getUniqueId(), bar);
        }
        return bar;
    }

    private static void cancelRemove(Player player) {
        BukkitTask task = removeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public static void cleanup() {
        stopUpdater();
        activeBars.values().forEach(BossBar::removeAll);
        activeBars.clear();
        removeTasks.values().forEach(BukkitTask::cancel);
        removeTasks.clear();
        persistentBars.clear();
    }
}