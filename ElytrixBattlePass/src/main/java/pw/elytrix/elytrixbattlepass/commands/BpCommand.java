package pw.elytrix.elytrixbattlepass.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.guis.MainMenu;
import pw.elytrix.elytrixbattlepass.user.User;
import pw.elytrix.elytrixbattlepass.utils.Colorize;
import pw.elytrix.elytrixbattlepass.utils.ItemUtils;

public class BpCommand implements CommandExecutor, TabCompleter {
    private final ElytrixBattlePass plugin;

    public BpCommand(ElytrixBattlePass plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) return true;
            User user = plugin.getUserManager().getUser(player);
            if (user == null) {
                Colorize.sendMessage(player, "&cДанные ещё не загружены.");
                return true;
            }
            MainMenu.get(player, user).open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            return handleAdmin(sender, args);
        }

        if (sender instanceof Player player) {
            User user = plugin.getUserManager().getUser(player);
            if (user == null) return true;
            MainMenu.get(player, user).open(player);
        }

        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elytrixbattlepass.admin")) {
            Colorize.sendMessage(sender, plugin.getConfigManager().getSettings().messages.admin.noPermission);
            return true;
        }

        if (args.length == 1) {
            sendAdminHelp(sender);
            return true;
        }

        String sub = args[1].toLowerCase();

        switch (sub) {
            case "top" -> handleTop(sender);
            case "reset" -> {
                if (args.length < 3) {
                    Colorize.sendMessage(sender, "&cИспользование: /battlepass admin reset <ник>");
                    return true;
                }
                handleReset(sender, args[2]);
            }
            case "setpoints" -> {
                if (args.length < 4) {
                    Colorize.sendMessage(sender, "&cИспользование: /battlepass admin setpoints <ник> <кол-во>");
                    return true;
                }
                handleSetPoints(sender, args[2], args[3]);
            }
            case "addpoints" -> {
                if (args.length < 4) {
                    Colorize.sendMessage(sender, "&cИспользование: /battlepass admin addpoints <ник> <кол-во>");
                    return true;
                }
                handleAddPoints(sender, args[2], args[3]);
            }
            case "givefragment" -> {
                if (args.length < 3) {
                    Colorize.sendMessage(sender, "&cИспользование: /battlepass admin givefragment <ник> [кол-во]");
                    return true;
                }
                handleGiveFragment(sender, args);
            }
            default -> sendAdminHelp(sender);
        }

        return true;
    }

    private void sendAdminHelp(CommandSender sender) {
        Colorize.sendMessage(sender, "&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &#F8BEFBАдмин-команды");
        Colorize.sendMessage(sender, "&#F8BEFB&l┃ ");
        Colorize.sendMessage(sender, "&#F8BEFB&l┃ &#F8BEFB/battlepass admin top &7— &fтоп 10 по поинтам");
        Colorize.sendMessage(sender, "&#F8BEFB&l┃ &#F8BEFB/battlepass admin reset <ник> &7— &fсброс");
        Colorize.sendMessage(sender, "&#F8BEFB&l┃ &#F8BEFB/battlepass admin setpoints <ник> <n> &7— &fустановить");
        Colorize.sendMessage(sender, "&#F8BEFB&l┃ &#F8BEFB/battlepass admin addpoints <ник> <n> &7— &fдобавить");
        Colorize.sendMessage(sender, "&#F8BEFB&l┃ &#F8BEFB/battlepass admin givefragment <ник> [n] &7— &fвыдать фрагменты");
        Colorize.sendMessage(sender, "&#F8BEFB&l┃ ");
    }

    private void handleTop(CommandSender sender) {
        List<User> allUsers = new ArrayList<>(plugin.getUserManager().getUsers().values());
        allUsers.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));

        Colorize.sendMessage(sender, "&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &#F8BEFBТоп 10 по поинтам");
        Colorize.sendMessage(sender, "&#F8BEFB&l┃ ");

        int count = Math.min(10, allUsers.size());
        for (int i = 0; i < count; i++) {
            User user = allUsers.get(i);
            Colorize.sendMessage(sender, "&#F8BEFB&l┃ &f#" + (i + 1) + " &#F8BEFB" + user.getName() + " &7— &#F8BEFB" + user.getPoints() + " поинтов");
        }

        if (allUsers.isEmpty()) {
            Colorize.sendMessage(sender, "&#F8BEFB&l┃ &7— Отсутствует");
        }

        Colorize.sendMessage(sender, "&#F8BEFB&l┃ ");
    }

    private void handleReset(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Colorize.sendMessage(sender, plugin.getConfigManager().getSettings().messages.admin.playerNotFound);
            return;
        }

        User user = plugin.getUserManager().getUser(target);
        if (user == null) {
            Colorize.sendMessage(sender, plugin.getConfigManager().getSettings().messages.admin.playerNotFound);
            return;
        }

        user.setPoints(0);
        user.setEarnedToday(0);
        user.setEarnedTotal(0);
        user.setCompletedTasksTotal(0);
        user.setCompletedInCycle(0);
        user.setCycleCooldownUntil(0L);
        user.getUsedTaskIds().clear();
        user.setCurrentQuest(null);
        plugin.getQuestManager().ensureQuestReady(user);

        Colorize.sendMessage(sender, Colorize.formatWithPlaceholders(
                plugin.getConfigManager().getSettings().messages.admin.playerReset,
                Map.of("player", targetName)));
    }

    private void handleSetPoints(CommandSender sender, String targetName, String amountStr) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Colorize.sendMessage(sender, plugin.getConfigManager().getSettings().messages.admin.playerNotFound);
            return;
        }
        User user = plugin.getUserManager().getUser(target);
        if (user == null) return;

        try {
            int amount = Integer.parseInt(amountStr);
            user.setPoints(amount);
            Colorize.sendMessage(sender, Colorize.formatWithPlaceholders(
                    plugin.getConfigManager().getSettings().messages.admin.pointsSet,
                    Map.of("player", targetName, "points", String.valueOf(amount))));
        } catch (NumberFormatException e) {
            Colorize.sendMessage(sender, "&cНекорректное число.");
        }
    }

    private void handleAddPoints(CommandSender sender, String targetName, String amountStr) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Colorize.sendMessage(sender, plugin.getConfigManager().getSettings().messages.admin.playerNotFound);
            return;
        }
        User user = plugin.getUserManager().getUser(target);
        if (user == null) return;

        try {
            int amount = Integer.parseInt(amountStr);
            user.setPoints(user.getPoints() + amount);
            user.setEarnedTotal(user.getEarnedTotal() + amount);
            Colorize.sendMessage(sender, Colorize.formatWithPlaceholders(
                    plugin.getConfigManager().getSettings().messages.admin.pointsAdded,
                    Map.of("player", targetName, "points", String.valueOf(amount))));
        } catch (NumberFormatException e) {
            Colorize.sendMessage(sender, "&cНекорректное число.");
        }
    }

    private void handleGiveFragment(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Colorize.sendMessage(sender, plugin.getConfigManager().getSettings().messages.admin.playerNotFound);
            return;
        }

        int amount = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
        ItemUtils.giveOrDrop(target, plugin.getFragmentItem(amount));
        Colorize.sendMessage(sender, Colorize.formatWithPlaceholders(
                plugin.getConfigManager().getSettings().messages.admin.fragmentsGiven,
                Map.of("player", target.getName(), "amount", String.valueOf(amount))));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("elytrixbattlepass.admin")) {
                completions.add("admin");
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("elytrixbattlepass.admin")) {
            return List.of("top", "reset", "setpoints", "addpoints", "givefragment");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("elytrixbattlepass.admin")) {
            String sub = args[1].toLowerCase();
            if (List.of("reset", "setpoints", "addpoints", "givefragment").contains(sub)) {
                List<String> names = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    names.add(player.getName());
                }
                return names;
            }
        }

        return Collections.emptyList();
    }
}