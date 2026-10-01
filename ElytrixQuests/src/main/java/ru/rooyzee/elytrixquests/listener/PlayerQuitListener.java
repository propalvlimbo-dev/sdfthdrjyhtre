package ru.rooyzee.elytrixquests.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.rooyzee.elytrixquests.Main;

public class PlayerQuitListener implements Listener {

    private final Main plugin;

    public PlayerQuitListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getQuestManager().unload(e.getPlayer().getUniqueId());
    }
}