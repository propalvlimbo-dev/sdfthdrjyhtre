package ru.rooyzee.elytrixquests.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.rooyzee.elytrixquests.Main;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

public class LuckPermsHook {

    private final Main plugin;
    private boolean enabled = false;

    public LuckPermsHook(Main plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) return false;
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = providerClass.getMethod("get").invoke(null);
            Object eventBus = api.getClass().getMethod("getEventBus").invoke(api);

            Class<?> nodeAddEvent = Class.forName("net.luckperms.api.event.node.NodeAddEvent");

            Method subscribe = null;
            for (Method m : eventBus.getClass().getMethods()) {
                if (!m.getName().equals("subscribe")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && p[2].equals(Consumer.class)) {
                    subscribe = m;
                    break;
                }
            }
            if (subscribe == null) {
                for (Method m : eventBus.getClass().getMethods()) {
                    if (!m.getName().equals("subscribe")) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 2 && p[1].equals(Consumer.class)) {
                        subscribe = m;
                        break;
                    }
                }
            }
            if (subscribe == null) return false;

            Consumer<Object> handler = this::onNodeAdd;

            if (subscribe.getParameterCount() == 3) {
                subscribe.invoke(eventBus, plugin, nodeAddEvent, handler);
            } else {
                subscribe.invoke(eventBus, nodeAddEvent, handler);
            }

            enabled = true;
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("LuckPerms hook: " + t.getMessage());
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void onNodeAdd(Object event) {
        try {
            Method isUser = event.getClass().getMethod("isUser");
            Object userFlag = isUser.invoke(event);
            if (!(userFlag instanceof Boolean) || !(Boolean) userFlag) return;

            Object node = event.getClass().getMethod("getNode").invoke(event);
            if (node == null) return;

            String nodeType = String.valueOf(node.getClass().getSimpleName()).toLowerCase(Locale.ROOT);
            String key = null;
            try {
                Object k = node.getClass().getMethod("getKey").invoke(node);
                if (k != null) key = k.toString();
            } catch (Throwable ignored) {}

            boolean inheritance = nodeType.contains("inheritance")
                    || (key != null && key.toLowerCase(Locale.ROOT).startsWith("group."));
            if (!inheritance) {
                try {
                    Object type = node.getClass().getMethod("getType").invoke(node);
                    if (type != null && type.toString().toLowerCase(Locale.ROOT).contains("inherit")) {
                        inheritance = true;
                    }
                } catch (Throwable ignored) {}
            }
            if (!inheritance) return;

            Object target = event.getClass().getMethod("getTarget").invoke(event);
            UUID uuid = null;
            try {
                Object id = target.getClass().getMethod("getUniqueId").invoke(target);
                if (id instanceof UUID) uuid = (UUID) id;
            } catch (Throwable ignored) {}

            if (uuid == null) return;

            final UUID finalUuid = uuid;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(finalUuid);
                if (player != null) {
                    plugin.getQuestManager().handleDonateReceive(player.getName());
                }
            });
        } catch (Throwable ignored) {}
    }
}