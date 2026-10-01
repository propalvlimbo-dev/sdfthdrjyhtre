package pw.elytrix.elytrixbattlepass.guis;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.integration.ElytrixSubscribeAPI;
import pw.elytrix.elytrixbattlepass.quest.Quest;
import pw.elytrix.elytrixbattlepass.user.User;
import pw.elytrix.elytrixbattlepass.utils.Colorize;
import pw.elytrix.elytrixbattlepass.utils.DateTimeUtil;

public class MainMenu {
    private static final ElytrixBattlePass plugin = ElytrixBattlePass.getInstance();
    private static final int[] QUEST_SLOTS = {36, 37, 28, 19, 10, 11, 12, 21, 30, 39, 40, 41, 32, 23, 14, 15, 16, 25, 34, 43, 44};
    private static final int[] PURPLE_DECOR = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 13, 17, 18, 20, 22, 24, 26, 27, 29, 31, 33, 35, 38, 42, 46, 47, 48, 50, 51, 52};
    private static final String ACTIVE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjViNWZhYThlNDgxZmNiODRjYmVmMWU1YzQyMGQ2YTgxYTZlNjhmNWEwNzUwMDFhMDI4ODI1YWMyMDE4ZWJlNyJ9fX0=";
    private static final String INACTIVE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTA1M2RkYzIyNDg1Mzg0MzE4MzE3NzkwYmY0YmRkMjQ1Yjc1OWU2ZDk0N2EwZGQ4ZWZiYWExMWE0YWEwZDFhZCJ9fX0=";
    private static final String BALANCE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDk3Zjg2OTBlZGQyZGY4MTAwYjdkMGQzOGQ0YTAzMjVkZTgzNDFiZTM5MGI4Y2RjMDIxYjI1MDJhMTU1MmE5NiJ9fX0=";
    private static final Map<UUID, Gui> OPEN_GUIS = new HashMap<>();

    public static Gui get(Player player, User user) {
        Gui gui = Gui.gui()
                .title(Component.text(Colorize.format("&#F8BEFB&lБоевой пропуск")))
                .rows(6)
                .disableAllInteractions()
                .create();

        OPEN_GUIS.put(player.getUniqueId(), gui);
        render(player, user, gui);
        return gui;
    }

    public static void tickOpenGuis() {
        OPEN_GUIS.entrySet().removeIf(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) return true;
            if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null
                    || !player.getOpenInventory().getTopInventory().equals(entry.getValue().getInventory())) return true;

            User user = plugin.getUserManager().getUserFromCache(player);
            if (user != null) render(player, user, entry.getValue());
            return false;
        });
    }

    public static void render(Player player, User user, Gui gui) {
        plugin.getQuestManager().ensureQuestReady(user);

        for (int slot : PURPLE_DECOR) {
            gui.setItem(slot, decorItem(Material.PURPLE_STAINED_GLASS_PANE));
        }

        boolean onCooldown = plugin.getQuestManager().isCycleOnCooldown(user);

        if (onCooldown) {
            long remainingSeconds = plugin.getQuestManager().getCycleRemainingMillis(user) / 1000L;
            GuiItem cooldown = ItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                    .setName(Colorize.format("&7« &cЦикл завершён &7»"))
                    .setLore(List.of(
                            Colorize.format("&#F8BEFB&l┃ "),
                            Colorize.format("&#F8BEFB&l┃ &fНовый набор откроется через:"),
                            Colorize.format("&#F8BEFB&l┃ &#F8BEFB" + DateTimeUtil.getFormattedTime(remainingSeconds)),
                            Colorize.format("&#F8BEFB&l┃ ")))
                    .asGuiItem();
            for (int slot : QUEST_SLOTS) gui.setItem(slot, cooldown);
        } else {
            int completed = user.getCompletedInCycle();
            for (int i = 0; i < QUEST_SLOTS.length; i++) {
                int number = i + 1;
                int slot = QUEST_SLOTS[i];

                if (number <= completed) {
                    gui.setItem(slot, completedItem());
                } else if (number == completed + 1) {
                    Quest quest = user.getCurrentQuest();
                    if (quest != null && quest.getTask() != null) {
                        gui.setItem(slot, buildQuestItem(player, user, quest));
                    } else {
                        gui.setItem(slot, ItemBuilder.from(Material.YELLOW_STAINED_GLASS_PANE)
                                .setName(Colorize.format("&7« &eЗадания недоступны &7»"))
                                .setLore(List.of(Colorize.format("&#F8BEFB&l┃ &fВ конфиге нет заданий.")))
                                .asGuiItem());
                    }
                } else {
                    gui.setItem(slot, blockedItem());
                }
            }
        }

        int dailyLimit = plugin.getQuestManager().getDailyLimit(user);

        gui.setItem(45, ItemBuilder.from(Material.BOOK)
                .setName(Colorize.format("&7« &#F8BEFBИнформация &7»"))
                .setLore(List.of(
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&#F8BEFB&l┃ &fБоевой пропуск — система заданий,"),
                        Colorize.format("&#F8BEFB&l┃ &fза выполнение вы получаете &#F8BEFBпоинты&f,"),
                        Colorize.format("&#F8BEFB&l┃ &fпотратьте их в &#F8BEFB/bpshop&f."),
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&#F8BEFB&l┃ &fВ одном цикле &#F8BEFB21 задание&f."),
                        Colorize.format("&#F8BEFB&l┃ &fПосле цикла — пауза &#F8BEFB24 часа&f."),
                        Colorize.format("&#F8BEFB&l┃ &fКаждый день сгорает &#F8BEFB5% поинтов&f."),
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&#F8BEFB&l┃ &fЛимит в день: &#F8BEFB" + dailyLimit + " поинтов"),
                        Colorize.format("&#F8BEFB&l┃ &fПодписка &#F8BEFBElytriX &fдаёт &#F8BEFBx2 поинтов&f"),
                        Colorize.format("&#F8BEFB&l┃ &fи лимит &#F8BEFB" + plugin.getConfigManager().getSettings().battlepass.premiumDailyPointsLimit + " поинтов/день&f."),
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&7● &fЛКМ &7— &fактивировать задание"),
                        Colorize.format("&7● &fShift+ЛКМ &7— &fпропустить задание"),
                        Colorize.format("&7● &fСКМ &7— &fпоказать/убрать прогресс"),
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&7● &fСайт: &#F8BEFBelytrix.pw")))
                .asGuiItem());

        gui.setItem(49, balanceItem(player, user));

        gui.setItem(53, ItemBuilder.from(Material.LODESTONE)
                .setName(Colorize.format("&7« &#F8BEFBМагазин &7»"))
                .setLore(List.of(
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&#F8BEFB&l┃ &fОткрыть магазин наград."),
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&7● &fНажмите для перехода")))
                .asGuiItem(event -> ShopMenu.get(player, user).open(player)));

        gui.update();
    }

    private static GuiItem buildQuestItem(Player player, User user, Quest quest) {
        int position = user.getCompletedInCycle() + 1;
        int min = ElytrixSubscribeAPI.applyBonus(player, quest.getTask().getMinPoints());
        int max = ElytrixSubscribeAPI.applyBonus(player, quest.getTask().getMaxPoints());

        if (!quest.getTask().isStarted()) {
            return ItemBuilder.from(createHead(INACTIVE_TEXTURE))
                    .setName(Colorize.format("&7« &#F8BEFBЗадание #" + position + " &7»"))
                    .setLore(List.of(
                            Colorize.format("&#F8BEFB&l┃ "),
                            Colorize.format("&#F8BEFB&l┃ &fЗадание засекречено"),
                            Colorize.format("&#F8BEFB&l┃ &fВремя: &#F8BEFB" + quest.getTask().getExpirationMinutes() + " мин."),
                            Colorize.format("&#F8BEFB&l┃ &fНаграда: &#F8BEFBот " + min + " до " + max + " поинтов"),
                            Colorize.format("&#F8BEFB&l┃ "),
                            Colorize.format("&7● &fЛКМ &7— &fактивировать задание")))
                    .asGuiItem(event -> {
                        if (plugin.getQuestManager().activateCurrentQuest(user)) {
                            Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.general.questStarted);
                            Gui current = OPEN_GUIS.get(player.getUniqueId());
                            if (current != null) render(player, user, current);
                        }
                    });
        }

        boolean bossbarActive = BossBarManager.isActive(player);
        String middleAction = bossbarActive ? "убрать прогресс" : "показать прогресс";

        return ItemBuilder.from(createHead(ACTIVE_TEXTURE))
                .setName(Colorize.format("&7« &#F8BEFBЗадание #" + position + " &7»"))
                .setLore(List.of(
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&#F8BEFB&l┃ &fЦель: &#F8BEFB" + quest.getTask().getDescription()),
                        Colorize.format("&#F8BEFB&l┃ &fПрогресс: &#F8BEFB" + quest.getTask().getCompleted() + " &7/ &#F8BEFB" + quest.getTask().getRequiredAmount()),
                        Colorize.format("&#F8BEFB&l┃ &fОсталось: &#F8BEFB" + DateTimeUtil.getFormattedTime(Math.max(1L, quest.getTask().getRemainingMillis() / 1000L))),
                        Colorize.format("&#F8BEFB&l┃ &fНаграда: &#F8BEFBот " + min + " до " + max + " поинтов"),
                        Colorize.format("&#F8BEFB&l┃ &fПропуск: &#F8BEFB" + quest.getTask().getSkipCost() + " поинтов"),
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&7● &fShift+ЛКМ &7— &fпропустить"),
                        Colorize.format("&7● &fСКМ &7— &f" + middleAction)))
                .asGuiItem(event -> {
                    if (event.getClick() == ClickType.MIDDLE) {
                        if (BossBarManager.isActive(player)) {
                            BossBarManager.remove(player);
                        } else {
                            BossBarManager.showProgress(player, quest.getTask());
                        }
                        Gui current = OPEN_GUIS.get(player.getUniqueId());
                        if (current != null) render(player, user, current);
                        return;
                    }

                    if (event.getClick().isShiftClick() && event.getClick().isLeftClick()) {
                        int cost = quest.getTask().getSkipCost();
                        if (user.getPoints() < cost) {
                            Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.shop.notEnoughPoints);
                            return;
                        }
                        user.setPoints(user.getPoints() - cost);
                        plugin.getQuestManager().skipCurrentQuest(user);
                        Gui current = OPEN_GUIS.get(player.getUniqueId());
                        if (current != null) render(player, user, current);
                    }
                });
    }

    private static GuiItem balanceItem(Player player, User user) {
        int burn = (int) Math.floor(user.getPoints() * (plugin.getConfigManager().getSettings().battlepass.dailyBurnPercent / 100.0D));
        int dailyLimit = plugin.getQuestManager().getDailyLimit(user);
        return ItemBuilder.from(createHead(BALANCE_TEXTURE))
                .setName(Colorize.format("&7« &#F8BEFBВаш баланс &7»"))
                .setLore(List.of(
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&#F8BEFB&l┃ &fБаланс: &#F8BEFB" + user.getPoints() + " поинтов"),
                        Colorize.format("&#F8BEFB&l┃ &fСегодня: &#F8BEFB" + user.getEarnedToday() + " &7/ &#F8BEFB" + dailyLimit),
                        Colorize.format("&#F8BEFB&l┃ &fМножитель: &#F8BEFBx" + String.format("%.2f", ElytrixSubscribeAPI.getMultiplier(player))),
                        Colorize.format("&#F8BEFB&l┃ &fЗавтра сгорит: &#F8BEFB" + burn + " поинтов &7(5%)"),
                        Colorize.format("&#F8BEFB&l┃ ")))
                .asGuiItem();
    }

    private static GuiItem completedItem() {
        return ItemBuilder.from(Material.LIME_STAINED_GLASS_PANE)
                .setName(Colorize.format("&7« &aЗадание выполнено &7»"))
                .setLore(List.of(
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&#F8BEFB&l┃ &fСтатус: &aВыполнено"),
                        Colorize.format("&#F8BEFB&l┃ ")))
                .asGuiItem();
    }

    private static GuiItem blockedItem() {
        return ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .setName(Colorize.format("&7« &cЗадание закрыто &7»"))
                .setLore(List.of(
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&#F8BEFB&l┃ &fВыполните предыдущие задания."),
                        Colorize.format("&#F8BEFB&l┃ ")))
                .asGuiItem();
    }

    private static GuiItem decorItem(Material material) {
        return ItemBuilder.from(material).setName(Colorize.format("&r")).asGuiItem();
    }

    private static ItemStack createHead(String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        try {
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta == null) return head;
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
            Field field = meta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(meta, profile);
            head.setItemMeta(meta);
        } catch (Exception ignored) {}
        return head;
    }

    public static void onPlayerQuit(Player player) {
        OPEN_GUIS.remove(player.getUniqueId());
    }

    public static void clear() {
        OPEN_GUIS.clear();
    }
}