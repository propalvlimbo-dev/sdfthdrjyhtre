package pw.elytrix.elytrixbattlepass.quest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.config.Settings;
import pw.elytrix.elytrixbattlepass.guis.BossBarManager;
import pw.elytrix.elytrixbattlepass.integration.ElytrixSubscribeAPI;
import pw.elytrix.elytrixbattlepass.user.User;
import pw.elytrix.elytrixbattlepass.utils.Colorize;
import pw.elytrix.elytrixbattlepass.utils.DateTimeUtil;
import pw.elytrix.elytrixbattlepass.utils.ExperienceUtils;
import pw.elytrix.elytrixbattlepass.utils.ItemUtils;

public class QuestManager {
    private final ElytrixBattlePass plugin;
    private final Map<String, Task> tasks = new HashMap<>();
    private final ZoneId zoneId = ZoneId.of("Europe/Moscow");

    public QuestManager(ElytrixBattlePass plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                User user = plugin.getUserManager().getUserFromCache(player);
                if (user == null) continue;

                ensureQuestReady(user);
                if (user.getCurrentQuest() == null) continue;

                Task task = user.getCurrentQuest().getTask();
                if (task == null || !task.isActive()) continue;

                switch (task.getType()) {
                    case "HAVE_EXP" -> setProgressQuest(user, ExperienceUtils.getTotalExperience(player));
                    case "HAVE_ITEM" -> {
                        Object materialValue = task.getValues().get("material");
                        if (materialValue instanceof Material material) {
                            setProgressQuest(user, ItemUtils.countItems(player, material));
                        }
                    }
                }
            }
        }, 40L, 40L);
    }

    public void load() {
        tasks.clear();

        Map<String, Settings.TaskConfig> configTasks = plugin.getConfigManager().getSettings().tasks;
        if (configTasks == null || configTasks.isEmpty()) {
            plugin.getLogger().warning("В config.yml нет ни одного задания!");
            return;
        }

        for (Map.Entry<String, Settings.TaskConfig> entry : configTasks.entrySet()) {
            String id = entry.getKey();
            Settings.TaskConfig config = entry.getValue();
            if (config == null || !config.enabled) continue;

            Map<String, Object> values = new HashMap<>();
            if (config.requirements != null) {
                for (Map.Entry<String, String> valueEntry : config.requirements.entrySet()) {
                    try {
                        values.put(valueEntry.getKey(), plugin.getConfigManager().getSettings().convertValue(valueEntry.getKey(), valueEntry.getValue()));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Задание " + id + ": некорректное значение " + valueEntry.getKey() + "=" + valueEntry.getValue());
                    }
                }
            }

            Material icon = Material.matchMaterial(config.icon == null ? "BARRIER" : config.icon);
            if (icon == null) icon = Material.BARRIER;

            tasks.put(id, new Task(id, config.name, config.description, icon, config.type,
                    QuestDifficulty.fromString(config.difficulty), values, 0, 0L,
                    config.timeMinutes, config.rewardMin, config.rewardMax, config.skipCost, config.weight));
        }

        plugin.getLogger().info("Загружено заданий: " + tasks.size());
    }

    public Task getTemplate(String taskId) {
        return tasks.get(taskId);
    }

    public Quest createQuest(String taskId, int progress, long startedAt) {
        Task template = tasks.get(taskId);
        if (template == null) return null;
        Task task = template.copy();
        task.setCompleted(progress);
        task.setStartedAt(startedAt);
        return new Quest(task, taskId);
    }

    public void ensureQuestReady(User user) {
        handleDailyReset(user);

        if (user.getCycleCooldownUntil() > 0L) {
            if (System.currentTimeMillis() >= user.getCycleCooldownUntil()) {
                resetCycleInternal(user);
            } else {
                user.setCurrentQuest(null);
                return;
            }
        }

        int cycleSize = plugin.getConfigManager().getSettings().battlepass.cycleSize;
        if (user.getCompletedInCycle() >= cycleSize) {
            user.setCycleCooldownUntil(System.currentTimeMillis()
                    + plugin.getConfigManager().getSettings().battlepass.cycleCooldownHours * 3_600_000L);
            user.setCurrentQuest(null);
            return;
        }

        if (user.getCurrentQuest() != null && user.getCurrentQuest().getTask() != null && user.getCurrentQuest().getTask().isExpired()) {
            user.setCurrentQuest(generateQuestForUser(user));
            Player player = user.getPlayer();
            if (player != null) Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.general.questExpired);
            return;
        }

        if (user.getCurrentQuest() == null) {
            user.setCurrentQuest(generateQuestForUser(user));
        }
    }

    public boolean activateCurrentQuest(User user) {
        ensureQuestReady(user);
        if (user.getCurrentQuest() == null) return false;

        if (isCycleOnCooldown(user)) {
            Player player = user.getPlayer();
            if (player != null) {
                Colorize.sendMessage(player, Colorize.formatWithPlaceholders(
                        plugin.getConfigManager().getSettings().messages.general.cycleCooldown,
                        Map.of("time", DateTimeUtil.getFormattedTime(getCycleRemainingMillis(user) / 1000L))));
            }
            return false;
        }

        if (isDailyLimitReached(user)) {
            Player player = user.getPlayer();
            if (player != null) Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.general.dailyLimitReached);
            return false;
        }

        Task task = user.getCurrentQuest().getTask();
        if (!task.isStarted()) task.setStartedAt(System.currentTimeMillis());
        return true;
    }

    public boolean skipCurrentQuest(User user) {
        ensureQuestReady(user);
        if (user.getCurrentQuest() == null || isCycleOnCooldown(user)) return false;
        user.setCurrentQuest(generateQuestForUser(user));
        Player player = user.getPlayer();
        if (player != null) Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.general.questSkipped);
        return true;
    }

    public void progressQuest(User user, int amount) {
        if (amount <= 0) return;
        ensureQuestReady(user);
        if (user.getCurrentQuest() == null || isCycleOnCooldown(user) || isDailyLimitReached(user)) return;

        Task task = user.getCurrentQuest().getTask();
        if (task == null || !task.isActive()) return;

        task.setCompleted(task.getCompleted() + amount);

        if (task.getCompleted() >= task.getRequiredAmount()) {
            completeQuest(user);
        }
    }

    public void setProgressQuest(User user, int amount) {
        ensureQuestReady(user);
        if (user.getCurrentQuest() == null || isCycleOnCooldown(user) || isDailyLimitReached(user)) return;

        Task task = user.getCurrentQuest().getTask();
        if (task == null || !task.isActive()) return;

        task.setCompleted(Math.max(0, amount));

        if (task.getCompleted() >= task.getRequiredAmount()) {
            completeQuest(user);
        }
    }

    public void completeQuest(User user) {
        ensureQuestReady(user);
        if (user.getCurrentQuest() == null || isCycleOnCooldown(user)) return;

        int limit = getDailyLimit(user);
        int remaining = limit - user.getEarnedToday();
        if (remaining <= 0) {
            Player player = user.getPlayer();
            if (player != null) Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.general.dailyLimitReached);
            return;
        }

        Task task = user.getCurrentQuest().getTask();
        int min = task.getMinPoints();
        int max = task.getMaxPoints();
        int basePoints = min >= max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        int points = ElytrixSubscribeAPI.applyBonus(user.getPlayer(), basePoints);
        points = Math.min(points, remaining);
        points = Math.max(0, points);

        user.setPoints(user.getPoints() + points);
        user.setEarnedToday(user.getEarnedToday() + points);
        user.setEarnedTotal(user.getEarnedTotal() + points);
        user.setCompletedTasksTotal(user.getCompletedTasksTotal() + 1);
        user.setCompletedInCycle(user.getCompletedInCycle() + 1);

        Player player = user.getPlayer();
        if (player != null) {
            Colorize.sendMessage(player, Colorize.formatWithPlaceholders(
                    plugin.getConfigManager().getSettings().messages.general.questCompleted,
                    Map.of("points", String.valueOf(points))));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.0F);
            BossBarManager.showCompleted(player);
        }

        user.setCurrentQuest(null);
        int cycleSize = plugin.getConfigManager().getSettings().battlepass.cycleSize;

        if (user.getCompletedInCycle() >= cycleSize) {
            user.setCycleCooldownUntil(System.currentTimeMillis()
                    + plugin.getConfigManager().getSettings().battlepass.cycleCooldownHours * 3_600_000L);
            user.getUsedTaskIds().clear();
            if (player != null) Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.general.cycleCompleted);
            return;
        }

        ensureQuestReady(user);
    }

    public void resetCycle(User user) {
        resetCycleInternal(user);
        ensureQuestReady(user);
    }

    private void resetCycleInternal(User user) {
        user.setCompletedInCycle(0);
        user.setCycleCooldownUntil(0L);
        user.getUsedTaskIds().clear();
        user.setCurrentQuest(null);
    }

    public boolean isCycleOnCooldown(User user) {
        return user.getCycleCooldownUntil() > System.currentTimeMillis();
    }

    public long getCycleRemainingMillis(User user) {
        return Math.max(0L, user.getCycleCooldownUntil() - System.currentTimeMillis());
    }

    public boolean isDailyLimitReached(User user) {
        return user.getEarnedToday() >= getDailyLimit(user);
    }

    public int getDailyLimit(User user) {
        if (user.isPremium()) {
            return plugin.getConfigManager().getSettings().battlepass.premiumDailyPointsLimit;
        }
        return plugin.getConfigManager().getSettings().battlepass.dailyPointsLimit;
    }

    private Quest generateQuestForUser(User user) {
        if (tasks.isEmpty()) return null;
        int position = user.getCompletedInCycle() + 1;
        QuestDifficulty target = getDifficultyForPosition(position);

        List<Task> pool = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task.getDifficulty() == target && !user.getUsedTaskIds().contains(task.getId())) pool.add(task);
        }
        if (pool.isEmpty()) {
            for (Task task : tasks.values()) {
                if (task.getDifficulty() == target) pool.add(task);
            }
        }
        if (pool.isEmpty()) {
            for (Task task : tasks.values()) {
                if (!user.getUsedTaskIds().contains(task.getId())) pool.add(task);
            }
        }
        if (pool.isEmpty()) pool.addAll(tasks.values());
        if (pool.isEmpty()) return null;

        Task selected = weightedPick(pool);
        user.getUsedTaskIds().add(selected.getId());
        Task copy = selected.copy();
        copy.setCompleted(0);
        copy.setStartedAt(0L);
        return new Quest(copy, selected.getId());
    }

    private Task weightedPick(List<Task> pool) {
        int totalWeight = 0;
        for (Task task : pool) totalWeight += Math.max(1, task.getWeight());
        if (totalWeight <= 0) return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int current = 0;
        for (Task task : pool) {
            current += Math.max(1, task.getWeight());
            if (random < current) return task;
        }
        return pool.get(0);
    }

    private QuestDifficulty getDifficultyForPosition(int position) {
        int easySlots = plugin.getConfigManager().getSettings().battlepass.easySlots;
        int mediumSlots = plugin.getConfigManager().getSettings().battlepass.mediumSlots;
        if (position <= easySlots) return QuestDifficulty.EASY;
        if (position <= mediumSlots) return QuestDifficulty.MEDIUM;
        return QuestDifficulty.HARD;
    }

    private void handleDailyReset(User user) {
        long epochDay = LocalDate.now(zoneId).toEpochDay();
        if (user.getLastDailyResetDay() == epochDay) return;
        user.setLastDailyResetDay(epochDay);
        user.setEarnedToday(0);
        int burnPercent = plugin.getConfigManager().getSettings().battlepass.dailyBurnPercent;
        int toBurn = (int) Math.floor(user.getPoints() * (burnPercent / 100.0D));
        if (toBurn > 0) {
            user.setPoints(Math.max(0, user.getPoints() - toBurn));
            Player player = user.getPlayer();
            if (player != null) {
                Colorize.sendMessage(player, Colorize.formatWithPlaceholders(
                        plugin.getConfigManager().getSettings().messages.general.pointsBurned,
                        Map.of("burned_points", String.valueOf(toBurn))));
            }
        }
    }
}