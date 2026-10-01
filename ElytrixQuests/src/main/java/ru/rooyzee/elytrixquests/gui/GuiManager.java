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
        ensureLoaded(player, () -> {
            LevelsMenu menu = new LevelsMenu(plugin, player);
            menu.open();
        });
    }

    public void openLevelMenu(Player player, int levelId) {
        ensureLoaded(player, () -> {
            LevelMenu menu = new LevelMenu(plugin, player, levelId);
            menu.open();
        });
    }

    /**
     * Данные есть — открываем сразу.
     * Данных нет (первый вход / после сброса) — показываем "Загрузка..."
     * и сами обновляем меню, когда данные придут. Перезаход не нужен.
     */
    private void ensureLoaded(Player player, Runnable openAction) {
        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());

        if (data != null) {
            openAction.run();
            return;
        }

        // Сначала открываем с "Загрузка..."
        openAction.run();

        // Потом грузим из БД и обновляем меню
        plugin.getDao().load(player.getUniqueId(), player.getName()).thenAccept(loaded -> {
            plugin.getQuestManager().cache(player.getUniqueId(), loaded);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                if (player.getOpenInventory() != null) {
                    Object holder = player.getOpenInventory().getTopInventory().getHolder();
                    if (holder instanceof LevelsMenu) {
                        new LevelsMenu(plugin, player).open();
                    } else if (holder instanceof LevelMenu) {
                        int lvl = ((LevelMenu) holder).getLevelId();
                        new LevelMenu(plugin, player, lvl).open();
                    }
                }
            });
        });
    }
}