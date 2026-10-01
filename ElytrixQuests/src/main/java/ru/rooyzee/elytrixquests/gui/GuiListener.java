package ru.rooyzee.elytrixquests.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.data.PlayerQuestData;
import ru.rooyzee.elytrixquests.quest.QuestLevel;

public class GuiListener implements Listener {

    private final Main plugin;

    // Обновлённые слоты заданий (20-24, 29-33)
    private static final int[] QUEST_SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    public GuiListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getClickedInventory() == null) return;

        Player player = (Player) e.getWhoClicked();
        Object holder = e.getInventory().getHolder();

        // ------------------------------------
        // Главное меню уровней
        // ------------------------------------
        if (holder instanceof LevelsMenu) {
            e.setCancelled(true);
            int slot = e.getRawSlot();

            if (slot == 21) openLevel(player, 1);
            else if (slot == 22) openLevel(player, 2);
            else if (slot == 23) openLevel(player, 3);
            else if (slot == 31) openLevel(player, 4);
            else if (slot == 49) player.closeInventory();
        }

        // ------------------------------------
        // Меню заданий
        // ------------------------------------
        else if (holder instanceof LevelMenu) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            LevelMenu levelMenu = (LevelMenu) holder;
            int levelId = levelMenu.getLevelId();

            // Кнопка Назад (45 слот)
            if (slot == 45) {
                plugin.getGuiManager().openLevelsMenu(player);
                return;
            }

            // Кнопка Закрыть (49 слот)
            if (slot == 49) {
                player.closeInventory();
                return;
            }

            // Клик по квесту
            for (int i = 0; i < QUEST_SLOTS.length; i++) {
                if (slot == QUEST_SLOTS[i]) {
                    int questId = i + 1; // ID квеста от 1 до 10
                    plugin.getQuestManager().handleClick(player, levelId, questId);
                    refreshLevelMenu(player, levelMenu);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Object holder = e.getInventory().getHolder();
        if (holder instanceof LevelsMenu || holder instanceof LevelMenu) {
            e.setCancelled(true);
        }
    }

    private void openLevel(Player player, int levelId) {
        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("data-loading"));
            return;
        }

        if (!plugin.getQuestManager().isLevelUnlocked(data, levelId)) {
            player.sendMessage(plugin.getConfigManager().getMessage("level-locked"));
            return;
        }

        QuestLevel level = plugin.getQuestManager().getLevel(levelId);
        if (level == null) return;

        plugin.getGuiManager().openLevelMenu(player, levelId);
    }

    private void refreshLevelMenu(Player player, LevelMenu menu) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory() != null
                    && player.getOpenInventory().getTopInventory().getHolder() instanceof LevelMenu) {
                menu.build();
            }
        }, 1L);
    }
}