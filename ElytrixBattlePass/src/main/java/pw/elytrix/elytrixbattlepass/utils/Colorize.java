package pw.elytrix.elytrixbattlepass.utils;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Colorize {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    public static void sendMessage(String player, String message) {
        Player p = Bukkit.getPlayerExact(player);
        if (p != null) {
            p.sendMessage(colorize(message));
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(colorize(message));
    }

    @Nullable
    public static String format(String message) {
        return colorize(message);
    }

    public static String formatDouble(double number) {
        DecimalFormat decimalFormat = new DecimalFormat("###,###.##");
        return decimalFormat.format(number);
    }

    public static String formatWithPlaceholders(String message, Map<String, String> placeholders) {
        String formatted = colorize(message);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return formatted;
    }

    public static void sendMessage(Player player, String message, Map<String, String> placeholders) {
        player.sendMessage(formatWithPlaceholders(message, placeholders));
    }
}