package pw.elytrix.elytrixbattlepass.guis;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.config.ShopSettings;
import pw.elytrix.elytrixbattlepass.user.User;
import pw.elytrix.elytrixbattlepass.utils.Colorize;

public class ShopMenu {
    private static final ElytrixBattlePass plugin = ElytrixBattlePass.getInstance();
    private static final Map<UUID, Confirmation> confirmations = new HashMap<>();

    /* Оформление Elytrix: рамка из панелей, внутренняя область НЕ заливается */
    private static final int[] BLACK_PANES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17, 18, 26};
    private static final int[] PURPLE_PANES = {27, 35, 36, 37, 43, 44, 46, 47, 48, 50, 51, 52, 53};
    private static final int BACK_SLOT = 45;
    private static final int BALANCE_SLOT = 49;
    private static final Set<Integer> RESERVED_SLOTS = new LinkedHashSet<>();
    private static final String BALANCE_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDk3Zjg2OTBlZGQyZGY4MTAwYjdkMGQzOGQ0YTAzMjVkZTgzNDFiZTM5MGI4Y2RjMDIxYjI1MDJhMTU1MmE5NiJ9fX0=";

    static {
        for (int slot : BLACK_PANES) RESERVED_SLOTS.add(slot);
        for (int slot : PURPLE_PANES) RESERVED_SLOTS.add(slot);
        RESERVED_SLOTS.add(BACK_SLOT);
        RESERVED_SLOTS.add(BALANCE_SLOT);
    }

    public static Gui get(Player player, User user) {
        String title = plugin.getConfigManager().getShopSettings().menuTitle;
        Gui gui = Gui.gui()
                .title(Component.text(Colorize.format(title == null || title.isBlank()
                        ? "&#F8BEFB&lМагазин боевого пропуска" : title)))
                .rows(6)
                .disableAllInteractions()
                .create();

        renderDecor(gui);
        renderItems(player, user, gui);
        renderUi(player, user, gui);
        return gui;
    }

    private static void renderDecor(Gui gui) {
        for (int slot : BLACK_PANES) {
            gui.setItem(slot, decor(Material.BLACK_STAINED_GLASS_PANE));
        }
        for (int slot : PURPLE_PANES) {
            gui.setItem(slot, decor(Material.PURPLE_STAINED_GLASS_PANE));
        }
    }

    private static void renderItems(Player player, User user, Gui gui) {
        Map<String, ShopSettings.ShopItem> items = plugin.getConfigManager().getShopSettings().items;
        if (items == null || items.isEmpty()) {
            plugin.getLogger().warning("В shop.yml нет предметов!");
            return;
        }

        items.forEach((id, item) -> {
            if (item == null || !item.enabled) return;

            if (item.slot < 0 || item.slot > 53) {
                plugin.getLogger().warning("Предмет " + id + " имеет некорректный slot " + item.slot + ". Разрешено 0-53.");
                return;
            }

            if (RESERVED_SLOTS.contains(item.slot)) {
                plugin.getLogger().warning("Предмет " + id + " стоит в служебном слоте " + item.slot
                        + " (рамка/баланс/назад). Используйте слоты 11-15, 19-25, 28-34, 38-42.");
                return;
            }

            ItemStack stack = buildItem(player, user, item);
            if (stack == null) return;

            GuiItem guiItem = new GuiItem(stack, event -> {
                event.setCancelled(true);
                handleBuy(player, user, id, item);
            });

            gui.setItem(item.slot, guiItem);
        });
    }

    private static void renderUi(Player player, User user, Gui gui) {
        gui.setItem(BACK_SLOT, ItemBuilder.from(Material.BLACK_DYE)
                .setName(Colorize.format("&7« &#F8BEFBНазад &7»"))
                .setLore(List.of(
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&#F8BEFB&l┃ &fВернуться в боевой пропуск."),
                        Colorize.format("&#F8BEFB&l┃ "),
                        Colorize.format("&7● &fНажмите для перехода")
                ))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    MainMenu.get(player, user).open(player);
                }));

        int burn = (int) Math.floor(user.getPoints()
                * (plugin.getConfigManager().getSettings().battlepass.dailyBurnPercent / 100.0D));
        String premiumLine = user.isPremium()
                ? "&#F8BEFB&l┃ &fПодписка: &aактивна &7(доступ к премиум-товарам)"
                : "&#F8BEFB&l┃ &fПодписка: &cотсутствует &7(elytrix.pw)";

        ItemStack balanceHead = createHead(BALANCE_TEXTURE);
        ItemMeta balanceMeta = balanceHead.getItemMeta();
        if (balanceMeta != null) {
            balanceMeta.setDisplayName(Colorize.format("&7« &#F8BEFBВаш баланс &7»"));
            balanceMeta.setLore(List.of(
                    Colorize.format("&#F8BEFB&l┃ "),
                    Colorize.format("&#F8BEFB&l┃ &fИгрок: &#F8BEFB" + player.getName()),
                    Colorize.format("&#F8BEFB&l┃ &fБаланс: &#F8BEFB" + user.getPoints() + " поинтов"),
                    Colorize.format("&#F8BEFB&l┃ &fЗавтра сгорит: &#F8BEFB" + burn + " поинтов &7(5%)"),
                    Colorize.format(premiumLine),
                    Colorize.format("&#F8BEFB&l┃ "),
                    Colorize.format("&7● &fПоинты выдаются за задания &#F8BEFB/bp")
            ));
            balanceHead.setItemMeta(balanceMeta);
        }
        gui.setItem(BALANCE_SLOT, new GuiItem(balanceHead, event -> event.setCancelled(true)));
    }

    private static void handleBuy(Player player, User user, String id, ShopSettings.ShopItem item) {
        if (item.premiumOnly && !user.isPremium()) {
            Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.shop.premiumRequired);
            return;
        }

        if (item.requiredPermission != null && !item.requiredPermission.isBlank()
                && !player.hasPermission(item.requiredPermission)) {
            Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.shop.permissionRequired);
            return;
        }

        if (user.getPoints() < item.cost) {
            Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.shop.notEnoughPoints);
            return;
        }

        if (item.confirmPurchase && !hasConfirmation(player.getUniqueId(), id)) {
            confirmations.put(player.getUniqueId(), new Confirmation(id, System.currentTimeMillis() + 10_000L));
            Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.shop.purchaseConfirm);
            Gui pending = get(player, user);
            pending.open(player);
            return;
        }

        confirmations.remove(player.getUniqueId());

        user.setPoints(user.getPoints() - item.cost);

        Purchase purchase = resolvePurchase(item);

        Colorize.sendMessage(player, plugin.getConfigManager().getSettings().messages.shop.itemPurchased);

        if (purchase.display() != null && !purchase.display().isBlank()) {
            String randomMessage = plugin.getConfigManager().getSettings().messages.shop.randomReward;
            if (randomMessage != null && !randomMessage.isBlank()) {
                Colorize.sendMessage(player, randomMessage.replace("%reward%", purchase.display()));
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String command : purchase.commands()) {
                if (command == null || command.isBlank()) continue;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("%player_name%", player.getName()).replace("%player%", player.getName()));
            }
        });

        if (item.broadcast != null && !item.broadcast.isBlank()) {
            Bukkit.broadcastMessage(Colorize.format(item.broadcast
                    .replace("%player_name%", player.getName())
                    .replace("%reward%", purchase.display() == null ? "" : purchase.display())));
        }

        try {
            player.playSound(player.getLocation(), Sound.valueOf(item.sound), 1.0F, 1.0F);
        } catch (Exception ignored) {
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0F, 1.0F);
        }

        if (item.closeAfterPurchase) {
            player.closeInventory();
            return;
        }

        Gui refreshed = get(player, user);
        refreshed.open(player);
    }

    private static boolean hasConfirmation(UUID uuid, String id) {
        Confirmation confirmation = confirmations.get(uuid);
        return confirmation != null && confirmation.expiresAt() > System.currentTimeMillis() && confirmation.id().equals(id);
    }

    private static Purchase resolvePurchase(ShopSettings.ShopItem item) {
        if (item.randomCommand) {
            if (item.rewards != null && !item.rewards.isEmpty()) {
                ShopSettings.RewardCommand reward = weightedReward(item.rewards);
                return new Purchase(reward.allCommands(), reward.display);
            }
            if (item.commands != null && !item.commands.isEmpty()) {
                return new Purchase(List.of(item.commands.get(ThreadLocalRandom.current().nextInt(item.commands.size()))), "");
            }
            return new Purchase(List.of(), "");
        }

        List<String> commands = new ArrayList<>();
        if (item.rewards != null) {
            for (ShopSettings.RewardCommand reward : item.rewards) {
                if (reward != null) commands.addAll(reward.allCommands());
            }
        }
        if (commands.isEmpty() && item.commands != null) {
            commands.addAll(item.commands);
        }
        return new Purchase(commands, "");
    }

    private static ShopSettings.RewardCommand weightedReward(List<ShopSettings.RewardCommand> rewards) {
        int total = 0;
        for (ShopSettings.RewardCommand reward : rewards) {
            total += Math.max(1, reward.weight);
        }

        int random = ThreadLocalRandom.current().nextInt(total);
        int current = 0;

        for (ShopSettings.RewardCommand reward : rewards) {
            current += Math.max(1, reward.weight);
            if (random < current) return reward;
        }

        return rewards.get(0);
    }

    private static ItemStack buildItem(Player player, User user, ShopSettings.ShopItem item) {
        ItemStack stack;

        if ("PLAYER_HEAD".equalsIgnoreCase(item.iconType) && item.iconTexture != null && !item.iconTexture.isBlank()) {
            stack = createHead(item.iconTexture);
        } else {
            Material material = Material.matchMaterial(item.iconType == null ? "" : item.iconType);
            stack = new ItemStack(material == null ? Material.BARRIER : material);
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(applyPlaceholders(player, user, item, item.name == null ? "" : item.name));

            String status = statusLine(player, user, item);
            List<String> lore = new ArrayList<>();
            boolean statusUsed = false;

            if (item.lore != null) {
                for (String line : item.lore) {
                    String raw = line == null ? "" : line;
                    if (raw.contains("%status%")) {
                        statusUsed = true;
                        raw = raw.replace("%status%", status);
                    }
                    lore.add(applyPlaceholders(player, user, item, raw));
                }
            }

            if (!statusUsed) {
                lore.add(Colorize.format(status));
            }

            meta.setLore(lore);

            if (item.iconEnchanted) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS,
                    ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
            stack.setItemMeta(meta);
        }

        return stack;
    }

    private static String statusLine(Player player, User user, ShopSettings.ShopItem item) {
        if (item.premiumOnly && !user.isPremium()) {
            return "&7● &cТребуется подписка &#F8BEFBElytriX &7(elytrix.pw)";
        }
        if (item.requiredPermission != null && !item.requiredPermission.isBlank()
                && !player.hasPermission(item.requiredPermission)) {
            return "&7● &cНедостаточно прав для покупки";
        }
        if (user.getPoints() < item.cost) {
            return "&7● &cНе хватает &#F8BEFB" + (item.cost - user.getPoints()) + " поинтов";
        }
        if (item.confirmPurchase) {
            return "&7● &fНажмите дважды для покупки";
        }
        return "&7● &fНажмите для покупки";
    }

    private static String applyPlaceholders(Player player, User user, ShopSettings.ShopItem item, String text) {
        String result = text
                .replace("%cost%", String.valueOf(item.cost))
                .replace("%points%", String.valueOf(user.getPoints()))
                .replace("%balance%", String.valueOf(user.getPoints()))
                .replace("%player_name%", player.getName());
        return Colorize.format(result);
    }

    private static GuiItem decor(Material material) {
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
        } catch (Exception ignored) {
        }
        return head;
    }

    private record Confirmation(String id, long expiresAt) {
    }

    private record Purchase(List<String> commands, String display) {
    }
}
