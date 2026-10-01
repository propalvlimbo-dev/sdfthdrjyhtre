package ru.rooyzee.elytrixquests.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;

public class PlayerKitsHook {

    private boolean enabled = false;
    private Plugin playerKitsPlugin;

    public boolean setup() {
        playerKitsPlugin = Bukkit.getPluginManager().getPlugin("PlayerKits");
        if (playerKitsPlugin == null) {
            playerKitsPlugin = Bukkit.getPluginManager().getPlugin("PlayerKits2");
        }
        enabled = playerKitsPlugin != null && playerKitsPlugin.isEnabled();
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isKitOnCooldown(Player player, String kitName) {
        if (!enabled || player == null || kitName == null) return false;
        try {
            Object api = findApiInstance();
            if (api == null) api = playerKitsPlugin;

            for (Method m : api.getClass().getMethods()) {
                String n = m.getName().toLowerCase(Locale.ROOT);
                if (!n.equals("getkitcooldown") && !n.equals("getcooldown") && !n.equals("getcooldowntime") && !n.equals("getplayercooldown")) {
                    continue;
                }

                Class<?>[] p = m.getParameterTypes();
                Object result = null;

                if (p.length == 2 && Player.class.isAssignableFrom(p[0]) && p[1] == String.class) {
                    result = m.invoke(api, player, kitName);
                } else if (p.length == 2 && p[0] == String.class && Player.class.isAssignableFrom(p[1])) {
                    result = m.invoke(api, kitName, player);
                }

                if (result instanceof Number) {
                    long cooldownVal = ((Number) result).longValue();
                    return cooldownVal > 0;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private Object findApiInstance() {
        try {
            String[] classes = {
                    "pk.ajneb.playerkits2.api.PlayerKitsAPI",
                    "pk.ajneb.playerkits.api.PlayerKitsAPI",
                    "com.ajneb.playerkits.api.PlayerKitsAPI"
            };
            for (String cn : classes) {
                try {
                    Class<?> c = Class.forName(cn);
                    try {
                        return c.getMethod("getInstance").invoke(null);
                    } catch (NoSuchMethodException e) {
                        return c.getMethod("getAPI").invoke(null);
                    }
                } catch (ClassNotFoundException ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }
}