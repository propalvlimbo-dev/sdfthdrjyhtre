package ru.rooyzee.elytrixquests.quest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.rooyzee.elytrixquests.Main;

public class PeriodicTaskManager {

    private final Main plugin;

    public PeriodicTaskManager(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        final java.util.Map<java.util.UUID, org.bukkit.Location> lastLocations = new java.util.HashMap<>();
        final java.util.Map<java.util.UUID, Boolean> hadSunBook = new java.util.HashMap<>();

        // Движение считаем здесь, а не через PlayerMoveEvent: так не теряются
        // маленькие перемещения и одинаково работают вода и Незер.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                org.bukkit.Location current = p.getLocation();
                org.bukkit.Location previous = lastLocations.put(p.getUniqueId(), current.clone());
                if (previous != null && previous.getWorld() == current.getWorld()) {
                    double distance = previous.distance(current);
                    if (distance > 0.0D && distance <= 20.0D) {
                        if (p.isSwimming() || p.isInWater()) {
                            plugin.getQuestManager().addDistanceProgress(p, QuestType.SWIM_DISTANCE, distance);
                        }
                        if (current.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
                            plugin.getQuestManager().addDistanceProgress(p, QuestType.NETHER_TRAVEL_DISTANCE, distance);
                        }
                    }
                }

                boolean book = plugin.getQuestManager().hasSunShacklesBook(p);
                boolean helmet = plugin.getQuestManager().hasSunHelmet(p);
                Boolean wasBook = hadSunBook.put(p.getUniqueId(), book);
                if (helmet && (!book || Boolean.TRUE.equals(wasBook))) {
                    plugin.getQuestManager().incrementProgress(p, QuestType.CRAFT_SUN_HELMET, q -> true, 1);
                }
            }
        }, 1L, 2L);

        // Обновляем открытое меню в реальном времени, чтобы прогресс движения
        // и другие счётчики был виден без повторного клика.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getOpenInventory() != null && p.getOpenInventory().getTopInventory().getHolder()
                        instanceof ru.rooyzee.elytrixquests.gui.LevelMenu) {
                    ((ru.rooyzee.elytrixquests.gui.LevelMenu) p.getOpenInventory().getTopInventory().getHolder()).build();
                }
            }
        }, 10L, 10L);

        // PLAYTIME — раз в минуту
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.getQuestManager().incrementProgress(p, QuestType.PLAYTIME, q -> true, 1);
            }
        }, 20L * 60, 20L * 60);

        // MONEY_BALANCE — баланс PlayerPoints раз в 30 секунд.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                int points = plugin.getQuestManager().getPlayerPointsBalance(p);
                if (points >= 0) {
                    plugin.getQuestManager().setProgressAbsolute(p, QuestType.MONEY_BALANCE, q -> true, points);
                }
            }
        }, 20L * 30, 20L * 30);

        // CASINO_WIN — любое увеличение баланса Vault после активации квеста.
        final java.util.Map<java.util.UUID, Double> previousBalance = new java.util.HashMap<>();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (plugin.getVaultHook() == null || !plugin.getVaultHook().isEnabled()) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                double now = plugin.getVaultHook().getBalance(p);
                Double before = previousBalance.put(p.getUniqueId(), now);
                if (before != null && now > before) {
                    plugin.getQuestManager().incrementProgress(p, QuestType.CASINO_WIN, q -> true, 1);
                }
            }
        }, 20L * 5, 20L * 30);

        // ElytrixBattlePass: отслеживаем фактическое завершение задания,
        // а не открытие меню /bp.
        final java.util.Map<java.util.UUID, Integer> battlepassCompleted = new java.util.HashMap<>();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Object bp = Bukkit.getPluginManager().getPlugin("ElytrixBattlePass");
            if (bp == null) return;
            try {
                Object userManager = bp.getClass().getMethod("getUserManager").invoke(bp);
                Object questManager = bp.getClass().getMethod("getQuestManager").invoke(bp);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Object user = userManager.getClass().getMethod("getUser", Player.class).invoke(userManager, p);
                    if (user == null) continue;
                    int completed = ((Number) user.getClass().getMethod("getCompletedTasksTotal").invoke(user)).intValue();
                    Integer previous = battlepassCompleted.put(p.getUniqueId(), completed);
                    if (previous != null && completed > previous) {
                        plugin.getQuestManager().incrementProgress(p, QuestType.BATTLEPASS_COMPLETE, q -> true, completed - previous);
                    }
                }
            } catch (Throwable ignored) {
                // BattlePass API недоступен — повторим проверку в следующем цикле.
            }
        }, 40L, 40L);

        // ACCOUNT_LINK — квест «Привязка аккаунта»: раз в auth.http.poll-seconds
        // проверяем привязку к Telegram (HTTP API ElytrixAuth) и автовыполняем
        // задание, как только она появится. Сеть — в async, завершение — на main.
        int accountLinkPoll = Math.max(5, plugin.getConfig().getInt("auth.http.poll-seconds", 15));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (final Player p : Bukkit.getOnlinePlayers()) {
                if (!plugin.getQuestManager().hasPendingAccountLink(p)) {
                    continue;
                }
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    final boolean linked;
                    try {
                        linked = plugin.getLinkChecker().isLinked(p.getName());
                    } catch (Throwable t) {
                        return; // прокси недоступен — попробуем в следующий раз
                    }
                    if (linked && p.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                plugin.getQuestManager().completeAccountLinkQuests(p));
                    }
                });
            }
        }, 20L * 5L, (long) accountLinkPoll * 20L);
    }
}