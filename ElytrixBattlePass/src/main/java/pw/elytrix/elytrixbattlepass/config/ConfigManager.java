package pw.elytrix.elytrixbattlepass.config;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;

public class ConfigManager {
    private final ElytrixBattlePass plugin;
    private Settings settings;
    private ShopSettings shopSettings;

    public ConfigManager(ElytrixBattlePass plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        File shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }

        loadSettings();
        loadShop();
    }

    private void loadSettings() {
        settings = new Settings();
        File file = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        Settings.MysqlSettings mysql = settings.storage.mysql;
        mysql.enabled = config.getBoolean("storage.mysql.enabled", false);
        mysql.host = config.getString("storage.mysql.host", "localhost");
        mysql.port = config.getInt("storage.mysql.port", 3306);
        mysql.database = config.getString("storage.mysql.database", "minecraft");
        mysql.username = config.getString("storage.mysql.username", "root");
        mysql.password = config.getString("storage.mysql.password", "");

        Settings.BattlePassSettings bp = settings.battlepass;
        bp.startDate = config.getString("battlepass.start_date", "4.05.2025");
        bp.cycleSize = config.getInt("battlepass.cycle_size", 21);
        bp.cycleCooldownHours = config.getInt("battlepass.cycle_cooldown_hours", 24);
        bp.dailyPointsLimit = config.getInt("battlepass.daily_points_limit", 365);
        bp.premiumDailyPointsLimit = config.getInt("battlepass.premium_daily_points_limit", 500);
        bp.dailyBurnPercent = config.getInt("battlepass.daily_burn_percent", 5);
        bp.easySlots = config.getInt("battlepass.easy_slots", 7);
        bp.mediumSlots = config.getInt("battlepass.medium_slots", 14);

        Settings.General g = settings.messages.general;
        g.questStarted = config.getString("messages.general.quest_started", "&aЗадание активировано.");
        g.questCompleted = config.getString("messages.general.quest_completed", "&aЗадание выполнено. Награда: %points%");
        g.questExpired = config.getString("messages.general.quest_expired", "&cВремя задания истекло.");
        g.questSkipped = config.getString("messages.general.quest_skipped", "&aЗадание пропущено.");
        g.dailyLimitReached = config.getString("messages.general.daily_limit_reached", "&cДневной лимит достигнут.");
        g.cycleCooldown = config.getString("messages.general.cycle_cooldown", "&cЦикл завершён. Через %time%");
        g.cycleCompleted = config.getString("messages.general.cycle_completed", "&aЦикл завершён.");
        g.pointsBurned = config.getString("messages.general.points_burned", "&fСгорело %burned_points% коинов.");
        g.bossbarProgress = config.getString("messages.general.bossbar_progress", "&#F8BEFB%description% &7— &#F8BEFB%current%&7/&#F8BEFB%required%");
        g.bossbarCompleted = config.getString("messages.general.bossbar_completed", "&aЗАДАНИЕ ВЫПОЛНЕНО");

        Settings.Shop s = settings.messages.shop;
        s.itemPurchased = config.getString("messages.shop.item_purchased", "&aПокупка завершена.");
        s.notEnoughPoints = config.getString("messages.shop.not_enough_points", "&cНедостаточно коинов.");
        s.premiumRequired = config.getString("messages.shop.premium_required", "&cТребуется подписка.");
        s.permissionRequired = config.getString("messages.shop.permission_required", "&cНедостаточно прав.");
        s.purchaseConfirm = config.getString("messages.shop.purchase_confirm", "&fНажмите ещё раз для подтверждения.");
        s.randomReward = config.getString("messages.shop.random_reward", "&fВам выпало: %reward%");

        Settings.Admin a = settings.messages.admin;
        a.noPermission = config.getString("messages.admin.no_permission", "&cНедостаточно прав.");
        a.playerNotFound = config.getString("messages.admin.player_not_found", "&cИгрок не найден.");
        a.playerReset = config.getString("messages.admin.player_reset", "&aПрогресс игрока %player% сброшен.");
        a.pointsSet = config.getString("messages.admin.points_set", "&aБаланс игрока %player% установлен: %points%");
        a.pointsAdded = config.getString("messages.admin.points_added", "&aИгроку %player% начислено %points% коинов");
        a.levelSet = config.getString("messages.admin.level_set", "&aИгроку %player% установлен уровень: %level%");
        a.fragmentsGiven = config.getString("messages.admin.fragments_given", "&aВыдано %amount% фрагментов игроку %player%");

        settings.tasks.clear();
        ConfigurationSection tasksSection = config.getConfigurationSection("tasks");
        if (tasksSection == null) {
            plugin.getLogger().warning("Секция tasks не найдена в config.yml!");
            return;
        }

        for (String taskId : tasksSection.getKeys(false)) {
            ConfigurationSection ts = tasksSection.getConfigurationSection(taskId);
            if (ts == null) continue;

            Settings.TaskConfig tc = new Settings.TaskConfig();
            tc.enabled = ts.getBoolean("enabled", true);
            tc.difficulty = ts.getString("difficulty", "EASY");
            tc.type = ts.getString("type", "BLOCK_BREAK");
            tc.name = ts.getString("name", "");
            tc.description = ts.getString("description", "");
            tc.icon = ts.getString("icon", "BARRIER");
            tc.timeMinutes = ts.getInt("time_minutes", 60);
            tc.skipCost = ts.getInt("skip_cost", 20);
            tc.weight = ts.getInt("weight", 1);
            tc.rewardMin = ts.getInt("reward.min", 15);
            tc.rewardMax = ts.getInt("reward.max", 25);

            tc.requirements = new LinkedHashMap<>();
            ConfigurationSection reqSection = ts.getConfigurationSection("requirements");
            if (reqSection != null) {
                for (String key : reqSection.getKeys(false)) {
                    tc.requirements.put(key, String.valueOf(reqSection.get(key)));
                }
            }

            settings.tasks.put(taskId, tc);
        }

        plugin.getLogger().info("Загружено заданий: " + settings.tasks.size());
    }

    private void loadShop() {
        shopSettings = new ShopSettings();
        File file = new File(plugin.getDataFolder(), "shop.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        shopSettings.menuTitle = config.getString("menu_title", "&#F8BEFB&lМагазин боевого пропуска");

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("Секция items не найдена в shop.yml!");
            return;
        }

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection is = itemsSection.getConfigurationSection(itemId);
            if (is == null) continue;

            ShopSettings.ShopItem si = new ShopSettings.ShopItem();
            si.enabled = is.getBoolean("enabled", true);
            si.slot = is.getInt("slot", -1);
            si.cost = is.getInt("cost", 0);
            si.name = is.getString("name", "");
            si.lore = is.getStringList("lore");
            si.iconType = is.getString("icon.type", "STONE");
            si.iconTexture = is.getString("icon.texture", "");
            si.iconEnchanted = is.getBoolean("icon.enchanted", false);
            si.premiumOnly = is.getBoolean("premium_only", false);
            si.randomCommand = is.getBoolean("random_command", false);
            si.confirmPurchase = is.getBoolean("confirm_purchase", false);
            si.closeAfterPurchase = is.getBoolean("close_after_purchase", false);
            si.requiredPermission = is.getString("required_permission", "");
            si.broadcast = is.getString("broadcast", "");
            si.sound = is.getString("sound", "BLOCK_END_PORTAL_FRAME_FILL");
            si.commands = is.getStringList("commands");

            si.rewards = new ArrayList<>();
            List<Map<?, ?>> rewardsList = is.getMapList("rewards");
            for (Map<?, ?> rewardMap : rewardsList) {
                ShopSettings.RewardCommand rc = new ShopSettings.RewardCommand();
                Object commandObj = rewardMap.get("command");
                rc.command = commandObj == null ? "" : String.valueOf(commandObj);

                Object commandsObj = rewardMap.get("commands");
                if (commandsObj instanceof List<?> list) {
                    for (Object cmd : list) {
                        if (cmd != null) rc.commands.add(String.valueOf(cmd));
                    }
                }

                Object displayObj = rewardMap.get("display");
                rc.display = displayObj == null ? "" : String.valueOf(displayObj);

                Object weightObj = rewardMap.get("weight");
                rc.weight = weightObj instanceof Number ? ((Number) weightObj).intValue() : 1;
                si.rewards.add(rc);
            }

            shopSettings.items.put(itemId, si);
        }

        plugin.getLogger().info("Загружено товаров: " + shopSettings.items.size());
    }

    public Settings getSettings() {
        return settings;
    }

    public ShopSettings getShopSettings() {
        return shopSettings;
    }
}