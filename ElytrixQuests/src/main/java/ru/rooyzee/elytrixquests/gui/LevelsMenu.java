package ru.rooyzee.elytrixquests.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.data.PlayerQuestData;
import ru.rooyzee.elytrixquests.data.QuestStatus;
import ru.rooyzee.elytrixquests.quest.Quest;
import ru.rooyzee.elytrixquests.quest.QuestLevel;
import ru.rooyzee.elytrixquests.util.ColorUtils;
import ru.rooyzee.elytrixquests.util.ItemBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LevelsMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Inventory inventory;
    private final MenuAnimator animator;

    private static final int[] LEVEL_SLOTS = {21, 22, 23, 31};
    private static final String[] LEVEL_TEXTURES = {
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTg4YmNlNDk3Y2ZhNWY2MTE4MzlmNmRhMjFjOTVkMzRlM2U3MjNjMmNjNGMzYzMxOWI1NjI3NzNkMTIxNiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjNlZWI0NDA0YTIyZTNjNWZiZGQ0ODM2YzcyYTdmNTljMTYxNTU4OGE5YzU3ZDI4NzE1NTQ1MzcyOGFlYSJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWM3ZjdiNzJmYzNlNzMzODI4ZmNjY2MwY2E4Mjc4YWNhMjYzM2FhMzNhMjMxYzkzYTY4MmQxNGFjNTRhYTBjNCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzRhNWRiOGVhNzk3N2M3Zjk2YTY4ZGZlMWZhNDliNjZlYWZiZWNkMzgxMWE2YjZkM2QxNzdkZGE1Zjk0MjMxMyJ9fX0="
    };

    private static final int[] BLACK_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17, 18, 26};
    private static final int[] PURPLE_SLOTS = {27, 35, 36, 37, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53};

    public LevelsMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54,
                ColorUtils.colorize("&#F8BEFB&lКвесты &7» &#F8BEFBУровни"));
        this.animator = new MenuAnimator(plugin);
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
        for (Map.Entry<Integer, ItemStack> item : createLevelItems().entrySet()) {
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

    private Map<Integer, ItemStack> createLevelItems() {
        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());

        for (int index = 0; index < LEVEL_SLOTS.length; index++) {
            int levelId = index + 1;
            QuestLevel level = plugin.getQuestManager().getLevel(levelId);
            String levelName = level != null ? level.getName() : "Уровень " + levelId;

            if (data == null) {
                items.put(LEVEL_SLOTS[index], new ItemBuilder(Material.CLOCK)
                        .name("&7« &eЗагрузка... &7»")
                        .lore(
                                "&#F8BEFB&l┃ ",
                                "&#F8BEFB&l┃ &fДанные профиля",
                                "&#F8BEFB&l┃ &fещё загружаются.",
                                "&#F8BEFB&l┃ ",
                                "&7● &fПожалуйста, подождите"
                        )
                        .build());
                continue;
            }

            if (level == null) {
                items.put(LEVEL_SLOTS[index], new ItemBuilder(Material.PLAYER_HEAD)
                        .texture(LEVEL_TEXTURES[index])
                        .name("&7« &#F8BEFBУровень " + levelId + " &7»")
                        .lore("&#F8BEFB&l┃ ", "&#F8BEFB&l┃ &fСкоро появится")
                        .build());
                continue;
            }

            boolean unlocked = plugin.getQuestManager().isLevelUnlocked(data, levelId);
            boolean completed = plugin.getQuestManager().isLevelFullyClaimed(data, levelId);
            int completedCount = 0;
            int totalCount = level.getQuests().size();
            for (Quest quest : level.getQuests().values()) {
                if (plugin.getQuestManager().getStatus(data, levelId, quest.getId()) == QuestStatus.CLAIMED) {
                    completedCount++;
                }
            }

            List<String> lore = new ArrayList<>();
            lore.add("&#F8BEFB&l┃ ");

            if (!unlocked) {
                long cooldownRemaining = plugin.getQuestManager().getLevelCooldownRemaining(data, levelId);
                if (cooldownRemaining > 0L) {
                    lore.add("&#F8BEFB&l┃ &fСтатус: &eПерерыв после уровня");
                    lore.add("&#F8BEFB&l┃ &fДо открытия: &#F8BEFB"
                            + plugin.getQuestManager().formatCooldownRemaining(cooldownRemaining));
                    lore.add("&#F8BEFB&l┃ ");
                    lore.add("&7● &fУровень откроется автоматически");
                } else {
                    lore.add("&#F8BEFB&l┃ &fСтатус: &cЗаблокировано");
                    lore.add("&#F8BEFB&l┃ &fТребование: &#F8BEFBПройти предыдущий уровень");
                    lore.add("&#F8BEFB&l┃ ");
                    lore.add("&7● &cСначала завершите предыдущий уровень");
                }
            } else if (completed) {
                lore.add("&#F8BEFB&l┃ &fСтатус: &aПройден полностью");
                lore.add("&#F8BEFB&l┃ &fКвесты: &#F8BEFB" + completedCount + " &fиз &#F8BEFB" + totalCount);
                lore.add("&#F8BEFB&l┃ ");
                lore.add("&7● &fНажмите для просмотра");
            } else {
                lore.add("&#F8BEFB&l┃ &fСтатус: &#F8BEFBДоступен");
                lore.add("&#F8BEFB&l┃ &fКвесты: &#F8BEFB" + completedCount + " &fиз &#F8BEFB" + totalCount);
                lore.add("&#F8BEFB&l┃ ");
                lore.add("&7● &fНажмите для открытия");
            }

            ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                    .texture(LEVEL_TEXTURES[index])
                    .name("&7« &#F8BEFB" + levelName + " &7»")
                    .lore(lore);

            if (completed) {
                builder.glow();
            }
            items.put(LEVEL_SLOTS[index], builder.build());
        }
        return items;
    }

    public boolean isAnimating() {
        return animator.isRunning();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
