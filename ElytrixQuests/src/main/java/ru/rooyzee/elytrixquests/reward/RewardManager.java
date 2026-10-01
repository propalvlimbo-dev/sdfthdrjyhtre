package ru.rooyzee.elytrixquests.reward;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.rooyzee.elytrixquests.Main;

import java.util.List;

public class RewardManager {

    private final Main plugin;

    public RewardManager(Main plugin) {
        this.plugin = plugin;
    }

    public void giveRewards(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String raw : commands) {
                String cmd = raw.replace("%player%", player.getName())
                        .replace("%uuid%", player.getUniqueId().toString());

                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    cmd = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, cmd);
                }

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        });
    }
}