package ru.rooyzee.elytrixquests.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + hex).toString();
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static List<String> colorize(List<String> list) {
        List<String> result = new ArrayList<>();
        if (list == null) return result;
        for (String s : list) result.add(colorize(s));
        return result;
    }
}