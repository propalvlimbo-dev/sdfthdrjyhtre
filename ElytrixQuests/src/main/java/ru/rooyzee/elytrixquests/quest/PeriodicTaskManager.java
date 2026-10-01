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
        // PLAYTIME — раз в минуту
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.getQuestManager().incrementProgress(p, QuestType.PLAYTIME, q -> true, 1);
            }
        }, 20L * 60, 20L * 60);

        // MONEY_BALANCE — раз в 30 секунд, только если есть Vault
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    double balance = plugin.getVaultHook().getBalance(p);
                    plugin.getQuestManager().setProgressAbsolute(p, QuestType.MONEY_BALANCE, q -> true, (int) balance);
                }
            }, 20L * 30, 20L * 30);
        }

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