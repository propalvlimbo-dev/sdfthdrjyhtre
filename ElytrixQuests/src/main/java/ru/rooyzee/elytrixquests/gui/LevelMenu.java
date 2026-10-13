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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LevelMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final int levelId;
    private final Inventory inventory;
    private final MenuAnimator animator;

    private static final int[] QUEST_SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    private static final int[] BLACK_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17, 18, 26};
    private static final int[] PURPLE_SLOTS = {27, 35, 36, 37, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53};

    public LevelMenu(Main plugin, Player player, int levelId) {
        this.plugin = plugin;
        this.player = player;
        this.levelId = levelId;
        this.animator = new MenuAnimator(plugin);

        QuestLevel level = plugin.getQuestManager().getLevel(levelId);
        String title = level != null ? level.getName() : "Уровень " + levelId;
        this.inventory = Bukkit.createInventory(this, 54,
                ColorUtils.colorize("&#F8BEFB&lКвесты &7» &#F8BEFB" + title));
    }

    public void open() {
        build();
        Map<Integer, ItemStack> menuItems = animator.snapshot(inventory);
        inventory.clear();
        player.openInventory(inventory);
        animator.animate(player, inventory, menuItems, this::build);
    }

    public void build() {
        if (animator.isRunning()) {
            return;
        }
        buildFrame();
        for (Map.Entry<Integer, ItemStack> item : createQuestItems().entrySet()) {
            inventory.setItem(item.getKey(), item.getValue());
        }
    }

    private void buildFrame() {
        inventory.clear();

        ItemStack blackPanel = ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot : BLACK_SLOTS) {
            inventory.setItem(slot, blackPanel);
        }

        ItemStack purplePanel = ItemBuilder.filler(Material.PURPLE_STAINED_GLASS_PANE);
        for (int slot : PURPLE_SLOTS) {
            inventory.setItem(slot, purplePanel);
        }

        inventory.setItem(45, new ItemBuilder(Material.BLACK_DYE)
                .name("&7« &#F8BEFBНазад к уровням &7»")
                .lore(
                        "&#F8BEFB&l┃ ",
                        "&#F8BEFB&l┃ &fВернуться к выбору уровня.",
                        "&#F8BEFB&l┃ ",
                        "&7● &fНажмите для возврата"
                )
                .build());

        inventory.setItem(49, new ItemBuilder(Material.RED_DYE)
                .name("&7« &cЗакрыть меню &7»")
                .lore(
                        "&#F8BEFB&l┃ ",
                        "&#F8BEFB&l┃ &fВернуться в игровой мир.",
                        "&#F8BEFB&l┃ ",
                        "&7● &fНажмите для закрытия"
                )
                .build());
    }

    private Map<Integer, ItemStack> createQuestItems() {
        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        QuestLevel level = plugin.getQuestManager().getLevel(levelId);
        if (level == null) {
            return items;
        }

        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());
        int slotIndex = 0;
        for (Quest quest : level.getQuests().values()) {
            if (slotIndex >= QUEST_SLOTS.length) {
                break;
            }

            if (data == null) {
                items.put(QUEST_SLOTS[slotIndex], new ItemBuilder(Material.CLOCK)
                        .name("&7« &eЗагрузка... &7»")
                        .lore(
                                "&#F8BEFB&l┃ ",
                                "&#F8BEFB&l┃ &fДанные профиля",
                                "&#F8BEFB&l┃ &fещё загружаются.",
                                "&#F8BEFB&l┃ ",
                                "&7● &fПожалуйста, подождите"
                        )
                        .build());
            } else {
                QuestStatus status = plugin.getQuestManager().getStatus(data, levelId, quest.getId());
                QuestEntry entry = data.getEntry(levelId, quest.getId());
                int progress = entry != null ? entry.getProgress() : 0;
                long cooldownRemaining = status == QuestStatus.CLAIMED
                        ? plugin.getQuestManager().getCompletedQuestCooldownRemaining(data, levelId, quest.getId())
                        : plugin.getQuestManager().getQuestCooldownRemaining(data, levelId, quest.getId());
                items.put(QUEST_SLOTS[slotIndex], buildQuestItem(quest, status, progress, cooldownRemaining));
            }
            slotIndex++;
        }
        return items;
    }

    private ItemStack buildQuestItem(Quest quest, QuestStatus status, int progress, long cooldownRemaining) {
        if (status == QuestStatus.LOCKED) {
            if (cooldownRemaining > 0L) {
                return new ItemBuilder(Material.CLOCK)
                        .name("&7« &eОжидание &7»")
                        .lore(
                                "&#F8BEFB&l┃ ",
                                "&#F8BEFB&l┃ &fСтатус: &eПерерыв после задания",
                                "&#F8BEFB&l┃ &fДо открытия: &#F8BEFB" + plugin.getQuestManager().formatCooldownRemaining(cooldownRemaining),
                                "&#F8BEFB&l┃ ",
                                "&7● &fСледующее задание откроется автоматически"
                        )
                        .build();
            }
            return new ItemBuilder(Material.BARRIER)
                    .name("&7« &cЗакрыто &7»")
                    .lore(
                            "&#F8BEFB&l┃ ",
                            "&#F8BEFB&l┃ &fСтатус: &cЗаблокировано",
                            "&#F8BEFB&l┃ ",
                            "&7● &fСначала завершите предыдущее задание"
                    )
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
                actionText = "&7● &fНажмите, чтобы начать";
                break;
            case IN_PROGRESS:
                material = quest.getIcon();
                statusText = "&eВ процессе";
                actionText = "&7● &fПрогресс засчитывается автоматически";
                break;
            case COMPLETED:
                material = Material.BOOK;
                statusText = "&aВыполнено";
                actionText = "&7● &fНаграда будет выдана автоматически";
                glow = true;
                break;
            case CLAIMED:
                material = Material.ENCHANTED_BOOK;
                statusText = "&aНаграда получена";
                actionText = "&7● &fНаграда выдана автоматически";
                glow = true;
                break;
            default:
                material = Material.PAPER;
                statusText = "&7Недоступно";
                actionText = "&7—";
        }

        List<String> lore = new ArrayList<>();
        lore.add("&#F8BEFB&l┃ ");
        if (quest.getLore() != null && !quest.getLore().isEmpty()) {
            lore.addAll(quest.getLore());
            lore.add("&#F8BEFB&l┃ ");
        }
        lore.add("&#F8BEFB&l┃ &fСтатус: " + statusText);

        if (status == QuestStatus.IN_PROGRESS) {
            lore.add("&#F8BEFB&l┃ &fПрогресс: &#F8BEFB" + progress + " &fиз &#F8BEFB" + quest.getAmount());
        }
        if (status == QuestStatus.CLAIMED && cooldownRemaining > 0L) {
            lore.add("&#F8BEFB&l┃ &fПерерыв после выполнения: &#F8BEFB"
                    + plugin.getQuestManager().formatCooldownRemaining(cooldownRemaining));
        }

        lore.add("&#F8BEFB&l┃ ");
        lore.add(actionText);

        ItemBuilder builder = new ItemBuilder(material)
                .name("&7« &#F8BEFB" + quest.getName() + " &7»")
                .lore(lore);
        if (glow) {
            builder.glow();
        }
        return builder.build();
    }

    public int getLevelId() {
        return levelId;
    }

    public boolean isAnimating() {
        return animator.isRunning();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
