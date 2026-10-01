package ru.rooyzee.elytrixquests.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.data.PlayerQuestData;
import ru.rooyzee.elytrixquests.quest.Quest;
import ru.rooyzee.elytrixquests.quest.QuestLevel;
import ru.rooyzee.elytrixquests.util.ColorUtils;
import ru.rooyzee.elytrixquests.util.ItemBuilder;

import java.util.ArrayList;
import java.util.List;

public class LevelsMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Inventory inventory;

    private static final int[] LEVEL_SLOTS = {21, 22, 23, 31};
    private static final Material[] LEVEL_ICONS = {
            Material.EMERALD,
            Material.GOLD_INGOT,
            Material.DIAMOND,
            Material.NETHER_STAR
    };

    // Точный массив из твоего меню
    private static final int[] BLACK_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17, 18, 26};
    private static final int[] PURPLE_SLOTS = {27, 35, 36, 37, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53};

    public LevelsMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.colorize("&#F8BEFB&lКвесты &7» &#F8BEFBУровни"));
    }

    public void open() {
        build();
        player.openInventory(inventory);
    }

    private void build() {
        inventory.clear();

        ItemStack blackPanel = ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot : BLACK_SLOTS) inventory.setItem(slot, blackPanel);

        ItemStack purplePanel = ItemBuilder.filler(Material.PURPLE_STAINED_GLASS_PANE);
        for (int slot : PURPLE_SLOTS) inventory.setItem(slot, purplePanel);

        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());
        boolean isLoading = (data == null);

        int levelIndex = 0;
        for (int levelId : new int[]{1, 2, 3, 4}) {
            QuestLevel level = plugin.getQuestManager().getLevel(levelId);
            String levelName = level != null ? level.getName() : "Уровень " + levelId;

            if (isLoading) {
                inventory.setItem(LEVEL_SLOTS[levelIndex], new ItemBuilder(Material.CLOCK)
                        .name("&7« &eЗагрузка... &7»")
                        .lore(
                                "&#F8BEFB&l┃ ",
                                "&#F8BEFB&l┃ &fДанные профиля",
                                "&#F8BEFB&l┃ &fещё не загрузились.",
                                "&#F8BEFB&l┃ ",
                                "&7● &fПожалуйста, подождите"
                        )
                        .build());
                levelIndex++;
                continue;
            }

            if (level == null) {
                inventory.setItem(LEVEL_SLOTS[levelIndex], new ItemBuilder(Material.CLOCK)
                        .name("&7« &#F8BEFBУровень " + levelId + " &7»")
                        .lore("&#F8BEFB&l┃ ", "&#F8BEFB&l┃ &fВ скором времени")
                        .build());
                levelIndex++;
                continue;
            }

            boolean unlocked = plugin.getQuestManager().isLevelUnlocked(data, levelId);
            boolean completed = plugin.getQuestManager().isLevelFullyClaimed(data, levelId);

            int completedCount = 0;
            int totalCount = level != null ? level.getQuests().size() : 0;
            if (level != null) {
                for (Quest q : level.getQuests().values()) {
                    if (plugin.getQuestManager().getStatus(data, levelId, q.getId()) == ru.rooyzee.elytrixquests.data.QuestStatus.CLAIMED) {
                        completedCount++;
                    }
                }
            }

            Material icon = LEVEL_ICONS[levelIndex];
            List<String> lore = new ArrayList<>();
            lore.add("&#F8BEFB&l┃ ");

            if (!unlocked) {
                icon = Material.BARRIER;
                lore.add("&#F8BEFB&l┃ &fСтатус: &cЗаблокировано");
                lore.add("&#F8BEFB&l┃ &fТребование: &#F8BEFBПройти Уровень " + (levelId - 1));
                lore.add("&#F8BEFB&l┃ ");
                lore.add("&7● &cНедоступно для выбора");
            } else if (completed) {
                lore.add("&#F8BEFB&l┃ &fСтатус: &aПройден полностью");
                lore.add("&#F8BEFB&l┃ &fПрогресс: &#F8BEFB" + completedCount + " &fиз &#F8BEFB" + totalCount);
                lore.add("&#F8BEFB&l┃ ");
                lore.add("&7● &fНажмите для просмотра");
            } else {
                lore.add("&#F8BEFB&l┃ &fСтатус: &#F8BEFBДоступен");
                lore.add("&#F8BEFB&l┃ &fПрогресс: &#F8BEFB" + completedCount + " &fиз &#F8BEFB" + totalCount);
                lore.add("&#F8BEFB&l┃ ");
                lore.add("&7● &fНажмите для перехода");
            }

            ItemBuilder builder = new ItemBuilder(icon)
                    .name("&7« &#F8BEFB" + levelName + " &7»")
                    .lore(lore);

            if (completed) builder.glow();

            inventory.setItem(LEVEL_SLOTS[levelIndex], builder.build());
            levelIndex++;
        }

        // Кнопка закрыть меню
        inventory.setItem(49, new ItemBuilder(Material.RED_DYE)
                .name("&7« &cЗакрыть меню &7»")
                .lore(
                        "&#F8BEFB&l┃ ",
                        "&#F8BEFB&l┃ &fВыход в игровой мир.",
                        "&#F8BEFB&l┃ ",
                        "&7● &fНажмите для закрытия"
                )
                .build());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}