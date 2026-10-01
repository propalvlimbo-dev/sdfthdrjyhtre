package pw.elytrix.elytrixbattlepass;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pw.elytrix.elytrixbattlepass.commands.BpCommand;
import pw.elytrix.elytrixbattlepass.commands.BpShopCommand;
import pw.elytrix.elytrixbattlepass.commands.FragmentCommand;
import pw.elytrix.elytrixbattlepass.config.ConfigManager;
import pw.elytrix.elytrixbattlepass.guis.BossBarManager;
import pw.elytrix.elytrixbattlepass.guis.MainMenu;
import pw.elytrix.elytrixbattlepass.integration.ElytrixSubscribeAPI;
import pw.elytrix.elytrixbattlepass.placeholders.PlaceholderAPI;
import pw.elytrix.elytrixbattlepass.playerblocktracker.PlayerBlockTracker;
import pw.elytrix.elytrixbattlepass.quest.QuestManager;
import pw.elytrix.elytrixbattlepass.storage.DataStorage;
import pw.elytrix.elytrixbattlepass.storage.StorageFactory;
import pw.elytrix.elytrixbattlepass.tasks.types.BlockBreakTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.CraftingTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.EnchantItemTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.EntityDamageTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.FarmingTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.FishTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.InteractEntityTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.ItemBreakTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.ItemConsumeTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.KillEntityTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.PickupItemTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.PlayerMoveTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.ResurrectTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.SpawnerUseTaskType;
import pw.elytrix.elytrixbattlepass.tasks.types.TNTExplodeTaskType;
import pw.elytrix.elytrixbattlepass.user.User;
import pw.elytrix.elytrixbattlepass.user.UserManager;
import pw.elytrix.elytrixbattlepass.utils.Colorize;

public final class ElytrixBattlePass extends JavaPlugin implements Listener {
    private static ElytrixBattlePass instance;
    private ConfigManager configManager;
    private UserManager userManager;
    private QuestManager questManager;
    private DataStorage storage;

    @Override
    public void onEnable() {
        instance = this;

        try {
            configManager = new ConfigManager(this);
            questManager = new QuestManager(this);
            questManager.load();
            storage = StorageFactory.createStorage(this);
            userManager = new UserManager(storage);

            Bukkit.getPluginManager().registerEvents(this, this);

            registerTasks();
            PlayerBlockTracker.initialize(this);
            BossBarManager.startUpdater();

            if (getCommand("battlepass") == null) {
                throw new IllegalStateException("Команда battlepass не найдена в plugin.yml");
            }

            BpCommand bpCommand = new BpCommand(this);
            getCommand("battlepass").setExecutor(bpCommand);
            getCommand("battlepass").setTabCompleter(bpCommand);

            FragmentCommand fragmentCommand = new FragmentCommand(this);
            getCommand("fragment").setExecutor(fragmentCommand);
            getCommand("fragment").setTabCompleter(fragmentCommand);

            getCommand("bpshop").setExecutor(new BpShopCommand(this));

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new PlaceholderAPI(this).register();
            }

            ElytrixSubscribeAPI.initialize();

            Bukkit.getOnlinePlayers().forEach(player -> {
                User user = userManager.getUser(player);
                questManager.ensureQuestReady(user);
            });

            Bukkit.getScheduler().runTaskTimer(this, MainMenu::tickOpenGuis, 20L, 20L);

            Bukkit.getScheduler().runTaskTimer(this, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (countFragments(player) > 0) {
                        Colorize.sendActionBar(player, "&#F8BEFB● &fВ инвентаре боевые фрагменты");
                    }
                }
            }, 40L, 100L);

            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                for (User user : userManager.getUsers().values()) {
                    storage.saveUser(user);
                }
            }, 6000L, 6000L);

            getLogger().info("ElytrixBattlePass enabled");
        } catch (Throwable t) {
            getLogger().severe("Ошибка при запуске ElytrixBattlePass");
            t.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        MainMenu.clear();
        BossBarManager.cleanup();

        if (userManager != null && storage != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                User user = userManager.getUserFromCache(player);
                if (user != null) {
                    storage.saveUser(user);
                }
                player.closeInventory();
            }
        }

        PlayerBlockTracker.shutdown();

        if (storage != null) {
            storage.close();
        }
    }

    public void registerTasks() {
        new BlockBreakTaskType(this);
        new CraftingTaskType(this);
        new EntityDamageTaskType(this);
        new FarmingTaskType(this);
        new InteractEntityTaskType(this);
        new ItemBreakTaskType(this);
        new ItemConsumeTaskType(this);
        new PlayerMoveTaskType(this);
        new ResurrectTaskType(this);
        new EnchantItemTaskType(this);
        new TNTExplodeTaskType(this);
        new SpawnerUseTaskType(this);
        new KillEntityTaskType(this);
        new FishTaskType(this);
        new PickupItemTaskType(this);
    }

    public int countFragments(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isFragment(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isFragment(item)) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                item.setAmount(0);
            }
        }

        MainMenu.onPlayerQuit(player);
        BossBarManager.remove(player);

        User user = userManager.getUserFromCache(player);
        if (user != null) {
            storage.saveUser(user);
            userManager.deleteUserFromCache(player);
        }
    }

    public ItemStack getFragmentItem(int amount) {
        ItemStack item = new ItemStack(Material.PRISMARINE_CRYSTALS, amount);
        item.editMeta(meta -> {
            meta.setDisplayName(Colorize.format("&#F8BEFB&lБоевой фрагмент"));
            meta.setLore(List.of(
                    Colorize.format("&#F8BEFB&l┃ "),
                    Colorize.format("&#F8BEFB&l┃ &fОбменяй их на коины БП"),
                    Colorize.format("&#F8BEFB&l┃ &fКоманда: &#F8BEFB/fragment swap"),
                    Colorize.format("&#F8BEFB&l┃ ")
            ));
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(new NamespacedKey(this, "fragment"), PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public boolean isFragment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this, "fragment"), PersistentDataType.BYTE);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public DataStorage getStorage() {
        return storage;
    }

    public static ElytrixBattlePass getInstance() {
        return instance;
    }
}