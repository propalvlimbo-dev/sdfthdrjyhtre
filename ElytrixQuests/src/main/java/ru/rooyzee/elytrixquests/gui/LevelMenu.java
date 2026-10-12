package ru.rooyzee.elytrixquests.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.data.PlayerQuestData;
import ru.rooyzee.elytrixquests.data.QuestEntry;
import ru.rooyzee.elytrixquests.data.QuestStatus;
import ru.rooyzee.elytrixquests.quest.Quest;
import ru.rooyzee.elytrixquests.quest.QuestLevel;
import ru.rooyzee.elytrixquests.util.ColorUtils;
import ru.rooyzee.elytrixquests.util.ItemBuilder;

import java.util.ArrayList;
import java.util.List;

public class LevelMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final int levelId;
    private final Inventory inventory;

    private static final int[] QUEST_SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    private static final int[] BLACK_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17, 18, 26};
    private static final int[] PURPLE_SLOTS = {27, 35, 36, 37, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53};

    public LevelMenu(Main plugin, Player player, int levelId) {
        this.plugin = plugin;
        this.player = player;
        this.levelId = levelId;

        QuestLevel level = plugin.getQuestManager().getLevel(levelId);
        String title = level != null ? level.getName() : "Уровень " + levelId;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.colorize("&#F8BEFB&lКвесты &7» &#F8BEFB" + title));
    }

    public void open() {
        build();
        player.openInventory(inventory);
    }

    public void build() {
        inventory.clear();

        ItemStack blackPanel = ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot : BLACK_SLOTS) inventory.setItem(slot, blackPanel);

        ItemStack purplePanel = ItemBuilder.filler(Material.PURPLE_STAINED_GLASS_PANE);
        for (int slot : PURPLE_SLOTS) inventory.setItem(slot, purplePanel);

        QuestLevel level = plugin.getQuestManager().getLevel(levelId);
        if (level == null) return;

        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());
        boolean isLoading = (data == null);

        int slotIndex = 0;
        for (Quest quest : level.getQuests().values()) {
            if (slotIndex >= QUEST_SLOTS.length) break;

            if (isLoading) {
                inventory.setItem(QUEST_SLOTS[slotIndex], new ItemBuilder(Material.CLOCK)
                        .name("&7« &eЗагрузка... &7»")
                        .lore(
                                "&#F8BEFB&l┃ ",
                                "&#F8BEFB&l┃ &fДанные профиля",
                                "&#F8BEFB&l┃ &fещё не загрузились.",
                                "&#F8BEFB&l┃ ",
                                "&7● &fПожалуйста, подождите"
                        )
                        .build());
            } else {
                QuestStatus status = plugin.getQuestManager().getStatus(data, levelId, quest.getId());
                QuestEntry entry = data.getEntry(levelId, quest.getId());
                int progress = entry != null ? entry.getProgress() : 0;
                inventory.setItem(QUEST_SLOTS[slotIndex], buildQuestItem(quest, status, progress, data));
            }
            slotIndex++;
        }

        inventory.setItem(45, new ItemBuilder(Material.BLACK_DYE)
                .name("&7« &#F8BEFBНазад к уровням &7»")
                .lore(
                        "&#F8BEFB&l┃ ",
                        "&#F8BEFB&l┃ &fВозврат в меню выбора.",
                        "&#F8BEFB&l┃ ",
                        "&7● &fНажмите для перехода"
                )
                .build());

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

    private ItemStack buildQuestItem(Quest quest, QuestStatus status, int progress, PlayerQuestData data) {
        if (status == QuestStatus.LOCKED) {
            int previousId = -1;
            QuestLevel currentLevel = plugin.getQuestManager().getLevel(quest.getLevelId());
            if (currentLevel != null) {
                for (Integer id : currentLevel.getQuests().keySet()) {
                    if (id < quest.getId() && id > previousId) previousId = id;
                }
            }
            long remaining = previousId > 0
                    ? plugin.getQuestManager().getQuestCooldownRemaining(data, quest.getLevelId(), previousId) : 0L;
            if (remaining > 0L) {
                long seconds = (remaining + 999L) / 1000L;
                long hours = seconds / 3600L;
                long minutes = (seconds % 3600L) / 60L;
                long rest = seconds % 60L;
                String time = hours > 0 ? hours + " ч. " + minutes + " мин." : minutes + " мин. " + rest + " сек.";
                return new ItemBuilder(Material.CLOCK)
                        .name("&7« &eОжидание &7»")
                        .lore("&#F8BEFB&l┃ ", "&#F8BEFB&l┃ &fСледующее задание доступно через:",
                                "&#F8BEFB&l┃ &e" + time)
                        .build();
            }
            return new ItemBuilder(Material.BARRIER)
                    .name("&7« &cЗакрыто &7»")
                    .lore("&#F8BEFB&l┃ ", "&#F8BEFB&l┃ &fСтатус: &cЗаблокировано",
                            "&#F8BEFB&l┃ ", "&7● &cСначала выполните предыдущее задание")
                    .build();
        }

        Material material;
        String statusText;
        String actionText;
        boolean glow = false;

        switch (status) {
            case AVAILABLE:
                material = quest.getIcon();
                statusText = "&#F8BEFBДоступно";
                actionText = "&7● &fНажмите, чтобы взять";
                break;
            case IN_PROGRESS:
                material = quest.getIcon();
                statusText = "&eВ процессе";
                actionText = "&7● &fВыполняйте условие";
                break;
            case COMPLETED:
                material = Material.BOOK;
                statusText = "&aВыполнено!";
                actionText = "&7● &fНажмите, чтобы забрать награду";
                glow = true;
                break;
            case CLAIMED:
                material = Material.BOOK;
                statusText = "&aНаграда получена";
                actionText = "&7● &fЗадание завершено";
                glow = true;
                break;
            default:
                material = Material.PAPER;
                statusText = "&7Отсутствует";
                actionText = "&7—";
        }

        List<String> lore = new ArrayList<>();
        lore.add("&#F8BEFB&l┃ ");

        if (quest.getLore() != null && !quest.getLore().isEmpty()) {
            for (String line : quest.getLore()) lore.add(line);
            lore.add("&#F8BEFB&l┃ ");
        }

        lore.add("&#F8BEFB&l┃ &fСтатус: " + statusText);

        if (status == QuestStatus.IN_PROGRESS) {
            lore.add("&#F8BEFB&l┃ &fПрогресс: &#F8BEFB" + progress + " &fиз &#F8BEFB" + quest.getAmount());
        }

        lore.add("&#F8BEFB&l┃ ");
        lore.add(actionText);

        ItemBuilder builder = new ItemBuilder(material)
                .name("&7« &#F8BEFB" + quest.getName() + " &7»")
                .lore(lore);

        if (glow) builder.glow();
        return builder.build();
    }

    public int getLevelId() { return levelId; }

    @Override
    public Inventory getInventory() { return inventory; }
}