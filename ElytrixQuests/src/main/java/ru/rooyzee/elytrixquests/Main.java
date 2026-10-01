package ru.rooyzee.elytrixquests;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rooyzee.elytrixquests.command.ElytrixQuestsCommand;
import ru.rooyzee.elytrixquests.config.ConfigManager;
import ru.rooyzee.elytrixquests.database.DatabaseManager;
import ru.rooyzee.elytrixquests.database.PlayerDataDAO;
import ru.rooyzee.elytrixquests.gui.GuiListener;
import ru.rooyzee.elytrixquests.gui.GuiManager;
import ru.rooyzee.elytrixquests.hook.LinkChecker;
import ru.rooyzee.elytrixquests.hook.LuckPermsHook;
import ru.rooyzee.elytrixquests.hook.PlayerKitsHook;
import ru.rooyzee.elytrixquests.hook.VaultHook;
import ru.rooyzee.elytrixquests.hook.WorldGuardHook;
import ru.rooyzee.elytrixquests.listener.PlayerJoinListener;
import ru.rooyzee.elytrixquests.listener.PlayerQuitListener;
import ru.rooyzee.elytrixquests.listener.QuestTrackingListener;
import ru.rooyzee.elytrixquests.placeholder.QuestsPlaceholderExpansion;
import ru.rooyzee.elytrixquests.quest.PeriodicTaskManager;
import ru.rooyzee.elytrixquests.quest.QuestManager;
import ru.rooyzee.elytrixquests.reward.RewardManager;

public class Main extends JavaPlugin {

    private static Main instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PlayerDataDAO dao;
    private QuestManager questManager;
    private RewardManager rewardManager;
    private VaultHook vaultHook;
    private WorldGuardHook worldGuardHook;
    private PlayerKitsHook playerKitsHook;
    private LuckPermsHook luckPermsHook;
    private LinkChecker linkChecker;
    private PeriodicTaskManager periodicTaskManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.load();

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        dao = new PlayerDataDAO(this, databaseManager);
        rewardManager = new RewardManager(this);

        questManager = new QuestManager(this);
        questManager.loadQuests();

        guiManager = new GuiManager(this);

        vaultHook = new VaultHook();
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultHook.setup();
        }

        worldGuardHook = new WorldGuardHook();
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook.setup();
        }

        playerKitsHook = new PlayerKitsHook();
        playerKitsHook.setup();

        luckPermsHook = new LuckPermsHook(this);
        luckPermsHook.setup();

        registerListeners();
        registerCommand();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new QuestsPlaceholderExpansion(this).register();
        }

        linkChecker = new LinkChecker(this);

        periodicTaskManager = new PeriodicTaskManager(this);
        periodicTaskManager.start();

        questManager.loadOnlinePlayers();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
        if (dao != null) dao.shutdown();
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new QuestTrackingListener(this), this);
        pm.registerEvents(new GuiListener(this), this);
    }

    private void registerCommand() {
        PluginCommand cmd = getCommand("elytrixquests");
        if (cmd != null) {
            ElytrixQuestsCommand executor = new ElytrixQuestsCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
    }

    public void reload() {
        configManager.reload();
        linkChecker = new LinkChecker(this);
        questManager.loadQuests();
        questManager.loadOnlinePlayers();
    }

    public static Main getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerDataDAO getDao() { return dao; }
    public QuestManager getQuestManager() { return questManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public VaultHook getVaultHook() { return vaultHook; }
    public WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public PlayerKitsHook getPlayerKitsHook() { return playerKitsHook; }
    public LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
    public LinkChecker getLinkChecker() { return linkChecker; }
    public GuiManager getGuiManager() { return guiManager; }
}