package pw.elytrix.elytrixbattlepass.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class Settings {
    public StorageSettings storage = new StorageSettings();
    public BattlePassSettings battlepass = new BattlePassSettings();
    public Messages messages = new Messages();
    public Map<String, TaskConfig> tasks = new LinkedHashMap<>();

    public Object convertValue(String key, String value) {
        return switch (key.toLowerCase()) {
            case "material", "icon" -> Material.valueOf(value.toUpperCase());
            case "entity" -> EntityType.valueOf(value.toUpperCase());
            case "amount", "weight", "skip_cost" -> Integer.parseInt(value);
            case "require_upgraded", "require_extended" -> Boolean.parseBoolean(value);
            default -> value;
        };
    }

    public static class StorageSettings {
        public MysqlSettings mysql = new MysqlSettings();
    }

    public static class MysqlSettings {
        public boolean enabled = false;
        public String host = "localhost";
        public int port = 3306;
        public String database = "minecraft";
        public String username = "root";
        public String password = "";
    }

    public static class BattlePassSettings {
        public String startDate = "4.05.2025";
        public int cycleSize = 21;
        public int cycleCooldownHours = 24;
        public int dailyPointsLimit = 365;
        public int premiumDailyPointsLimit = 500;
        public int dailyBurnPercent = 5;
        public int easySlots = 7;
        public int mediumSlots = 14;
    }

    public static class Messages {
        public General general = new General();
        public Shop shop = new Shop();
        public Admin admin = new Admin();
    }

    public static class General {
        public String questStarted = "";
        public String questCompleted = "";
        public String questExpired = "";
        public String questSkipped = "";
        public String dailyLimitReached = "";
        public String cycleCooldown = "";
        public String cycleCompleted = "";
        public String pointsBurned = "";
        public String bossbarProgress = "";
        public String bossbarCompleted = "";
    }

    public static class Shop {
        public String itemPurchased = "";
        public String notEnoughPoints = "";
        public String premiumRequired = "";
        public String permissionRequired = "";
        public String purchaseConfirm = "";
        public String randomReward = "";
    }

    public static class Admin {
        public String noPermission = "";
        public String playerNotFound = "";
        public String playerReset = "";
        public String pointsSet = "";
        public String pointsAdded = "";
        public String levelSet = "";
        public String fragmentsGiven = "";
    }

    public static class TaskConfig {
        public boolean enabled = true;
        public String difficulty = "EASY";
        public String type = "BLOCK_BREAK";
        public String name = "";
        public String description = "";
        public String icon = "BARRIER";
        public int timeMinutes = 60;
        public int skipCost = 20;
        public int weight = 1;
        public int rewardMin = 15;
        public int rewardMax = 25;
        public Map<String, String> requirements = new LinkedHashMap<>();
    }
}