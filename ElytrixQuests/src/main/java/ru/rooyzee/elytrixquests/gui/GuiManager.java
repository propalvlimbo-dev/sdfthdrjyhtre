package ru.rooyzee.elytrixquests.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.data.PlayerQuestData;

public class GuiManager {

    private final Main plugin;

    public GuiManager(Main plugin) {
        this.plugin = plugin;
    }

    public void openLevelsMenu(Player player) {
        ensureLoaded(player, () -> new LevelsMenu(plugin, player).open());
    }

    public void openLevelMenu(Player player, int levelId) {
        ensureLoaded(player, () -> new LevelMenu(plugin, player, levelId).open());
    }

    /** Opens immediately with loading icons; when the async database read finishes, the menu refreshes itself. */
    private void ensureLoaded(final Player player, final Runnable openAction) {
        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());
        if (data != null) {
            openAction.run();
            return;
        }

        openAction.run();
        plugin.getDao().load(player.getUniqueId(), player.getName()).thenAccept(loaded ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;

                    plugin.getQuestManager().cache(player.getUniqueId(), loaded);
                    plugin.getQuestManager().discardLegacyCompletedRewards(player);

                    if (player.getOpenInventory() == null) return;
                    Object holder = player.getOpenInventory().getTopInventory().getHolder();
                    if (holder instanceof LevelsMenu) {
                        new LevelsMenu(plugin, player).open();
                    } else if (holder instanceof LevelMenu) {
                        int levelId = ((LevelMenu) holder).getLevelId();
                        new LevelMenu(plugin, player, levelId).open();
                    }
                }));
    }
}
