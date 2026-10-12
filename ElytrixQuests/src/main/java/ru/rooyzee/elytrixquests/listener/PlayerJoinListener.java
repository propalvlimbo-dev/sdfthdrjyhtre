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
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        plugin.getDao().load(player.getUniqueId(), player.getName()).thenAccept(data ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    plugin.getQuestManager().cache(player.getUniqueId(), data);
                    plugin.getQuestManager().recoverPendingRewards(player);
                    plugin.getQuestManager().incrementProgress(player, QuestType.JOIN_SERVER, q -> true, 1);
                })).exceptionally(exception -> {
            plugin.getLogger().severe("Не удалось загрузить данные игрока " + player.getName() + ": "
                    + exception.getMessage());
            return null;
        });
    }
}
