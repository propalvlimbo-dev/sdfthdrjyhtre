package pw.elytrix.elytrixbattlepass.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.integration.ElytrixSubscribeAPI;
import pw.elytrix.elytrixbattlepass.user.User;
import pw.elytrix.elytrixbattlepass.utils.Colorize;
import pw.elytrix.elytrixbattlepass.utils.ItemUtils;

public class FragmentCommand implements CommandExecutor, TabCompleter {
    private final ElytrixBattlePass plugin;
    private final Map<UUID, Long> swapConfirmations = new HashMap<>();

    public FragmentCommand(ElytrixBattlePass plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length >= 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("elytrixbattlepass.admin")) {
                giveFragments(sender, args);
            }
            return true;
        }

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            Colorize.sendMessage(player, "&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &#F8BEFBФрагменты");
            Colorize.sendMessage(player, "  &#F8BEFB&l┃ ");
            Colorize.sendMessage(player, "  &#F8BEFB&l┃ &fФрагменты — предметы из ивентов.");
            Colorize.sendMessage(player, "  &#F8BEFB&l┃ &fОбменяйте их на &#F8BEFBкоины БП&f.");
            Colorize.sendMessage(player, "  &#F8BEFB&l┃ ");
            Colorize.sendMessage(player, "  &7● &fКоманда: &#F8BEFB/fragment swap");
            Colorize.sendMessage(player, "  &7● &fДержите фрагменты в руке");
            return true;
        }

        if (args[0].equalsIgnoreCase("swap")) {
            handleSwapCommand(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && sender.hasPermission("elytrixbattlepass.admin")) {
            giveFragments(sender, args);
            return true;
        }

        return true;
    }

    private void handleSwapCommand(Player player) {
        UUID playerId = player.getUniqueId();

        if (swapConfirmations.containsKey(playerId)) {
            long lastAttempt = swapConfirmations.get(playerId);
            if (System.currentTimeMillis() - lastAttempt <= 40000L) {
                swapConfirmations.remove(playerId);
                swapFragments(player);
                return;
            }
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (!plugin.isFragment(itemStack)) {
            Colorize.sendMessage(player, "&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &cДержите фрагменты в руке.");
            return;
        }

        int amount = itemStack.getAmount();
        int minPoints = ElytrixSubscribeAPI.applyBonus(player, amount * 10);
        int maxPoints = ElytrixSubscribeAPI.applyBonus(player, amount * 20);
        boolean hasBonus = ElytrixSubscribeAPI.hasSubscription(player);

        Colorize.sendMessage(player, "&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &#F8BEFBОбмен фрагментов");
        Colorize.sendMessage(player, "  &#F8BEFB&l┃ ");
        Colorize.sendMessage(player, "  &#F8BEFB&l┃ &fФрагментов: &#F8BEFB" + amount);
        Colorize.sendMessage(player, "  &#F8BEFB&l┃ &fКоинов: &#F8BEFB" + minPoints + " &7— &#F8BEFB" + maxPoints);
        if (hasBonus) {
            Colorize.sendMessage(player, "  &#F8BEFB&l┃ &fБонус подписки: &ax2");
        }
        Colorize.sendMessage(player, "  &#F8BEFB&l┃ ");
        Colorize.sendMessage(player, "  &7● &fВведите &#F8BEFB/fragment swap &fснова для подтверждения");

        swapConfirmations.put(playerId, System.currentTimeMillis());
        new BukkitRunnable() {
            @Override
            public void run() {
                swapConfirmations.remove(playerId);
            }
        }.runTaskLater(plugin, 800L);
    }

    private void swapFragments(Player player) {
        User user = plugin.getUserManager().getUserFromCache(player);
        if (user == null) {
            Colorize.sendMessage(player, "&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &cДанные не загружены, перезайдите.");
            return;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (!plugin.isFragment(itemStack)) {
            Colorize.sendMessage(player, "&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &cДержите фрагменты в руке.");
            return;
        }

        int amount = itemStack.getAmount();
        int basePoints = 0;
        for (int i = 0; i < amount; i++) {
            basePoints += ThreadLocalRandom.current().nextInt(10, 21);
        }

        int finalPoints = ElytrixSubscribeAPI.applyBonus(player, basePoints);
        boolean hasBonus = ElytrixSubscribeAPI.hasSubscription(player);

        itemStack.setAmount(0);
        user.setPoints(user.getPoints() + finalPoints);
        user.setEarnedTotal(user.getEarnedTotal() + finalPoints);

        Colorize.sendMessage(player, "&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &aОбмен выполнен!");
        Colorize.sendMessage(player, "  &#F8BEFB&l┃ ");
        Colorize.sendMessage(player, "  &#F8BEFB&l┃ &fОбменено: &#F8BEFB" + amount + " фрагментов");
        Colorize.sendMessage(player, "  &#F8BEFB&l┃ &fПолучено: &#F8BEFB" + finalPoints + " коинов");
        if (hasBonus) {
            Colorize.sendMessage(player, "  &#F8BEFB&l┃ &fБонус подписки: &ax2");
        }
    }

    private void giveFragments(CommandSender sender, String[] args) {
        if (args.length < 2) return;

        Player targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            Colorize.sendMessage(sender, plugin.getConfigManager().getSettings().messages.admin.playerNotFound);
            return;
        }

        int amount = args.length >= 3 ? Integer.parseInt(args[2]) : 1;
        ItemUtils.giveOrDrop(targetPlayer, plugin.getFragmentItem(amount));
        Colorize.sendMessage(sender, Colorize.formatWithPlaceholders(
                plugin.getConfigManager().getSettings().messages.admin.fragmentsGiven,
                Map.of("player", targetPlayer.getName(), "amount", String.valueOf(amount))));
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("help");
            suggestions.add("swap");
            if (sender.hasPermission("elytrixbattlepass.admin")) {
                suggestions.add("give");
            }
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("elytrixbattlepass.admin")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }

        return List.of();
    }
}