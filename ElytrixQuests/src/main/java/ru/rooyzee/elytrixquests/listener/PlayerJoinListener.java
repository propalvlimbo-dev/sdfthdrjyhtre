package ru.rooyzee.elytrixquests.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.quest.QuestType;

public class PlayerJoinListener implements Listener {

    private final Main plugin;

    public PlayerJoinListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        plugin.getDao().load(player.getUniqueId(), player.getName()).thenAccept(data -> {
            plugin.getQuestManager().cache(player.getUniqueId(), data);
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getQuestManager().incrementProgress(player, QuestType.JOIN_SERVER, q -> true, 1));
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Не удалось загрузить данные игрока " + player.getName() + ": " + ex.getMessage());
            return null;
        });
    }
}