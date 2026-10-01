package ru.rooyzee.elytrixquests.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.util.ColorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElytrixQuestsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private static final String PREFIX = "&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» ";

    public ElytrixQuestsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getGuiManager().openLevelsMenu((Player) sender);
            } else {
                sender.sendMessage(ColorUtils.colorize(PREFIX + "&#F8BEFBElytrixQuests &fv" + plugin.getDescription().getVersion()));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "activate":
                if (sender instanceof Player) {
                    handleActivate((Player) sender, args);
                } else {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cЭта команда доступна только игрокам."));
                }
                break;

            case "admin":
                handleAdmin(sender, args);
                break;

            default:
                if (sender instanceof Player) {
                    plugin.getGuiManager().openLevelsMenu((Player) sender);
                } else {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cНеизвестная подкоманда. Используйте &f/elytrixquests admin"));
                }
                break;
        }

        return true;
    }

    private void handleActivate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ColorUtils.colorize(PREFIX + "&cИспользование: &f/elytrixquests activate <уровень> <квест>"));
            return;
        }
        try {
            int level = Integer.parseInt(args[1]);
            int quest = Integer.parseInt(args[2]);
            plugin.getQuestManager().handleClick(player, level, quest);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize(PREFIX + "&cПараметры уровня и квеста должны быть числами."));
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elytrixquests.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sendAdminHelp(sender);
            return;
        }

        switch (args[1].toLowerCase()) {

            case "reload":
                plugin.reload();
                sender.sendMessage(ColorUtils.colorize(PREFIX + "&aКонфигурация и данные игроков успешно перезагружены."));
                break;

            case "reset": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cИспользование: &f/elytrixquests admin reset <игрок>"));
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                plugin.getQuestManager().resetPlayer(target.getUniqueId());
                sender.sendMessage(ColorUtils.colorize(PREFIX + "&aПрогресс квестов игрока &#F8BEFB" + args[2] + " &aуспешно сброшен."));
                break;
            }

            case "complete": {
                if (args.length < 5) {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cИспользование: &f/elytrixquests admin complete <игрок> <уровень> <квест>"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cИгрок &#F8BEFB" + args[2] + " &cне найден на сервере."));
                    return;
                }
                try {
                    int level = Integer.parseInt(args[3]);
                    int quest = Integer.parseInt(args[4]);
                    plugin.getQuestManager().forceComplete(target, level, quest);
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&aКвест &#F8BEFB" + level + "-" + quest + " &aуспешно засчитан для игрока &#F8BEFB" + target.getName() + "&a."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cПараметры уровня и квеста должны быть числами."));
                }
                break;
            }

            case "unlock": {
                if (args.length < 4) {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cИспользование: &f/elytrixquests admin unlock <игрок> <уровень>"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cИгрок &#F8BEFB" + args[2] + " &cне найден на сервере."));
                    return;
                }
                try {
                    int level = Integer.parseInt(args[3]);
                    plugin.getQuestManager().forceUnlock(target, level);
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&aУровень &#F8BEFB" + level + " &aуспешно открыт для игрока &#F8BEFB" + target.getName() + "&a."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cНомер уровня должен быть числом."));
                }
                break;
            }

            case "info": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cИспользование: &f/elytrixquests admin info <игрок>"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ColorUtils.colorize(PREFIX + "&cИгрок &#F8BEFB" + args[2] + " &cне найден на сервере."));
                    return;
                }
                plugin.getQuestManager().sendInfo(sender, target);
                break;
            }

            default:
                sendAdminHelp(sender);
                break;
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize(PREFIX + "&#F8BEFBКоманды управления квестами"));
        sender.sendMessage(ColorUtils.colorize("&#F8BEFB&l┃ &f/elytrixquests admin reload &7— &#F8BEFBПерезагрузить конфигурацию"));
        sender.sendMessage(ColorUtils.colorize("&#F8BEFB&l┃ &f/elytrixquests admin reset &f<игрок> &7— &#F8BEFBСбросить весь прогресс"));
        sender.sendMessage(ColorUtils.colorize("&#F8BEFB&l┃ &f/elytrixquests admin complete &f<игрок> <ур> <квест> &7— &#F8BEFBЗасчитать квест"));
        sender.sendMessage(ColorUtils.colorize("&#F8BEFB&l┃ &f/elytrixquests admin unlock &f<игрок> <ур> &7— &#F8BEFBОткрыть уровень"));
        sender.sendMessage(ColorUtils.colorize("&#F8BEFB&l┃ &f/elytrixquests admin info &f<игрок> &7— &#F8BEFBПосмотреть прогресс"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("elytrixquests.admin")) {
                result.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("elytrixquests.admin")) {
                result.addAll(Arrays.asList("reload", "reset", "complete", "unlock", "info"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("elytrixquests.admin")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    result.add(p.getName());
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("complete")) {
            result.addAll(Arrays.asList("1", "2", "3", "4"));
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("complete")) {
            result.addAll(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
        }

        return result;
    }
}