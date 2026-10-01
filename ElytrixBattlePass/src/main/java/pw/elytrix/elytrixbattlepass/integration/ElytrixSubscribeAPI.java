package pw.elytrix.elytrixbattlepass.integration;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;

public class ElytrixSubscribeAPI {

    private static boolean available = false;
    private static Object subManagerInstance = null;
    private static Method hasSubMethod = null;
    private static final double PREMIUM_MULTIPLIER = 2.0;

    public static void initialize() {
        Plugin subscribePlugin = Bukkit.getPluginManager().getPlugin("ElytrixSubscribe");
        if (subscribePlugin == null || !subscribePlugin.isEnabled()) {
            ElytrixBattlePass.getInstance().getLogger().info("ElytrixSubscribe не найден — бонус x2 недоступен.");
            return;
        }

        try {
            Class<?> mainClass = Class.forName("ru.rooyzee.elytrixsubscribe.Main");
            Method getInstance = mainClass.getMethod("getInstance");
            Object mainInstance = getInstance.invoke(null);

            Method getSubManager = mainClass.getMethod("getSubManager");
            subManagerInstance = getSubManager.invoke(mainInstance);

            hasSubMethod = subManagerInstance.getClass().getMethod("hasSub", Player.class);

            available = true;
            ElytrixBattlePass.getInstance().getLogger().info("Интеграция с ElytrixSubscribe успешно активирована. Бонус x2 включён.");
        } catch (Exception e) {
            ElytrixBattlePass.getInstance().getLogger().severe("Ошибка инициализации ElytrixSubscribe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean hasSubscription(Player player) {
        if (!available || player == null) return false;
        try {
            Object result = hasSubMethod.invoke(subManagerInstance, player);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    public static int applyBonus(Player player, int amount) {
        if (hasSubscription(player)) {
            return (int) Math.round(amount * PREMIUM_MULTIPLIER);
        }
        return amount;
    }

    public static double getMultiplier(Player player) {
        return hasSubscription(player) ? PREMIUM_MULTIPLIER : 1.00;
    }

    public static boolean isAvailable() {
        return available;
    }
}