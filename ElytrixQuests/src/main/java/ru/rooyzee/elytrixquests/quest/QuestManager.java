package ru.rooyzee.elytrixquests.quest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.data.PlayerQuestData;
import ru.rooyzee.elytrixquests.hook.LinkChecker;
import ru.rooyzee.elytrixquests.data.QuestEntry;
import ru.rooyzee.elytrixquests.data.QuestStatus;
import ru.rooyzee.elytrixquests.util.ColorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class QuestManager {

    private final Main plugin;
    private final Map<Integer, QuestLevel> levels = new TreeMap<>();
    private final Map<UUID, PlayerQuestData> cache = new ConcurrentHashMap<>();

    private final Map<UUID, Long> awaitingWarpTeleport = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Set<UUID>>> uniqueKills = new ConcurrentHashMap<>();
    private final Map<UUID, Double> swimmingDistance = new ConcurrentHashMap<>();
    private final Map<UUID, Double> netherDistance = new ConcurrentHashMap<>();

    private static final long WARP_TELEPORT_WINDOW_MS = 120_000L;

    public QuestManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadQuests() {
        levels.clear();
        FileConfiguration cfg = plugin.getConfigManager().getQuestsConfig();
        ConfigurationSection levelsSection = cfg.getConfigurationSection("levels");
        if (levelsSection == null) {
            plugin.getLogger().warning("Секция 'levels' не найдена в quests.yml!");
            return;
        }

        for (String levelKey : levelsSection.getKeys(false)) {
            int levelId;
            try {
                levelId = Integer.parseInt(levelKey);
            } catch (NumberFormatException e) {
                continue;
            }

            ConfigurationSection levelSec = levelsSection.getConfigurationSection(levelKey);
            if (levelSec == null) continue;

            String levelName = levelSec.getString("name", "Level " + levelId);
            List<String> megaCommands = levelSec.getStringList("mega-reward.commands");
            String megaMessage = levelSec.getString("mega-reward.message", "");

            QuestLevel level = new QuestLevel(levelId, levelName, megaCommands, megaMessage);

            ConfigurationSection questsSec = levelSec.getConfigurationSection("quests");
            if (questsSec != null) {
                for (String questKey : questsSec.getKeys(false)) {
                    int questId;
                    try {
                        questId = Integer.parseInt(questKey);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    ConfigurationSection qs = questsSec.getConfigurationSection(questKey);
                    if (qs == null) continue;

                    String name = qs.getString("name", "Quest " + questId);
                    List<String> lore = qs.getStringList("lore");

                    Material icon = Material.matchMaterial(qs.getString("icon", "PAPER"));
                    if (icon == null) icon = Material.PAPER;

                    QuestType type;
                    try {
                        type = QuestType.valueOf(qs.getString("type", "MANUAL_CLICK").toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Неизвестный тип квеста level " + levelId + " quest " + questId);
                        type = QuestType.MANUAL_CLICK;
                    }

                    int amount = qs.getInt("amount", 1);

                    Material material = null;
                    if (qs.isSet("material")) {
                        material = Material.matchMaterial(qs.getString("material"));
                    }

                    EntityType entityType = null;
                    if (qs.isSet("entity")) {
                        try {
                            entityType = EntityType.valueOf(qs.getString("entity").toUpperCase());
                        } catch (IllegalArgumentException ignored) {}
                    }

                    String stringData = firstNonNull(qs, "trigger", "region", "value", "advancement", "message", "potion", "kit");
                    List<String> rewards = qs.getStringList("rewards");

                    Quest quest = new Quest(questId, levelId, name, lore, icon, type, amount,
                            material, entityType, stringData, rewards);
                    level.addQuest(quest);
                }
            }

            levels.put(levelId, level);
        }
    }

    private String firstNonNull(ConfigurationSection sec, String... keys) {
        for (String key : keys) {
            if (sec.isSet(key)) return sec.getString(key);
        }
        return null;
    }

    public void cache(UUID uuid, PlayerQuestData data) {
        cache.put(uuid, data);
    }

    public PlayerQuestData getData(UUID uuid) {
        return cache.get(uuid);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
        awaitingWarpTeleport.remove(uuid);
        uniqueKills.remove(uuid);
        swimmingDistance.remove(uuid);
        netherDistance.remove(uuid);
    }

    public void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            final UUID uuid = player.getUniqueId();
            final String name = player.getName();
            plugin.getDao().load(uuid, name).thenAccept(data -> cache.put(uuid, data));
        }
    }

    public QuestLevel getLevel(int id) {
        return levels.get(id);
    }

    public Quest getQuest(int level, int quest) {
        QuestLevel l = levels.get(level);
        return l != null ? l.getQuest(quest) : null;
    }

    public Map<Integer, QuestLevel> getLevels() {
        return levels;
    }

    public boolean isLevelUnlocked(PlayerQuestData data, int levelId) {
        if (levelId <= 1) return true;
        return isLevelFullyClaimed(data, levelId - 1);
    }

    public boolean isLevelFullyClaimed(PlayerQuestData data, int levelId) {
        QuestLevel level = levels.get(levelId);
        if (level == null || level.getQuests().isEmpty()) return false;

        for (Quest q : level.getQuests().values()) {
            QuestEntry entry = data.getEntry(levelId, q.getId());
            if (entry == null || entry.getStatus() != QuestStatus.CLAIMED) return false;
        }
        return data.isMegaClaimed(levelId);
    }

    private boolean isLevelFullyClaimedExceptMega(PlayerQuestData data, int levelId) {
        QuestLevel level = levels.get(levelId);
        if (level == null || level.getQuests().isEmpty()) return false;
        for (Quest q : level.getQuests().values()) {
            QuestEntry entry = data.getEntry(levelId, q.getId());
            if (entry == null || entry.getStatus() != QuestStatus.CLAIMED) return false;
        }
        return true;
    }

    public boolean isQuestUnlocked(PlayerQuestData data, int levelId, int questId) {
        if (!isLevelUnlocked(data, levelId)) return false;
        if (questId <= 1) return true;

        QuestLevel level = levels.get(levelId);
        if (level == null) return false;

        int prevId = -1;
        for (Integer id : level.getQuests().keySet()) {
            if (id < questId && id > prevId) prevId = id;
        }
        if (prevId == -1) return true;

        QuestEntry prev = data.getEntry(levelId, prevId);
        if (prev == null) return false;
        QuestStatus st = prev.getStatus();
        if (st != QuestStatus.COMPLETED && st != QuestStatus.CLAIMED) return false;
        return System.currentTimeMillis() >= data.getCooldownUntil(levelId, prevId);
    }

    public int getCurrentLevel(PlayerQuestData data) {
        int current = 1;
        for (Integer id : levels.keySet()) {
            if (isLevelUnlocked(data, id)) current = id;
        }
        return current;
    }

    private QuestLevel getNextLevel(int levelId) {
        Integer next = null;
        for (Integer id : levels.keySet()) {
            if (id > levelId && (next == null || id < next)) next = id;
        }
        return next != null ? levels.get(next) : null;
    }

    public QuestStatus getStatus(PlayerQuestData data, int levelId, int questId) {
        if (!isLevelUnlocked(data, levelId)) return QuestStatus.LOCKED;
        if (!isQuestUnlocked(data, levelId, questId)) return QuestStatus.LOCKED;

        QuestEntry entry = data.getEntry(levelId, questId);
        return entry != null ? entry.getStatus() : QuestStatus.AVAILABLE;
    }

    public String getStatusDisplay(QuestStatus status) {
        return plugin.getConfigManager().getMessage("status." + status.name().toLowerCase());
    }

    public void handleClick(Player player, int levelId, int questId) {
        PlayerQuestData data = cache.get(player.getUniqueId());
        if (data == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("data-loading"));
            return;
        }

        QuestLevel level = levels.get(levelId);
        Quest quest = level != null ? level.getQuest(questId) : null;
        if (level == null || quest == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("unknown-quest"));
            return;
        }

        if (!isLevelUnlocked(data, levelId)) {
            player.sendMessage(plugin.getConfigManager().getMessage("level-locked"));
            playSound(player, "level-locked");
            return;
        }

        if (!isQuestUnlocked(data, levelId, questId)) {
            QuestLevel currentLevel = levels.get(levelId);
            int previousId = -1;
            if (currentLevel != null) {
                for (Integer id : currentLevel.getQuests().keySet()) if (id < questId && id > previousId) previousId = id;
            }
            if (previousId > 0) {
                long remaining = getQuestCooldownRemaining(data, levelId, previousId);
                if (remaining > 0) {
                    long minutes = (remaining + 59999L) / 60000L;
                    Map<String, String> cooldownPh = new HashMap<>();
                    cooldownPh.put("time", minutes + " мин.");
                    player.sendMessage(plugin.getConfigManager().getMessage("quest-cooldown", cooldownPh));
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("quest-locked-sequence"));
                }
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("quest-locked-sequence"));
            }
            playSound(player, "level-locked");
            return;
        }

        QuestEntry entry = data.getOrCreate(levelId, questId);

        if (quest.getType() == QuestType.REGION_CLAIM
                && plugin.getWorldGuardHook() != null
                && plugin.getWorldGuardHook().isEnabled()
                && plugin.getWorldGuardHook().hasRegion(player)) {
            if (entry.getStatus() == QuestStatus.AVAILABLE || entry.getStatus() == QuestStatus.IN_PROGRESS) {
                markCompleted(player, data, levelId, quest, entry);
                return;
            }
        }

        if (quest.getType() == QuestType.KIT_CLAIM) {
            if (entry.getStatus() == QuestStatus.AVAILABLE || entry.getStatus() == QuestStatus.IN_PROGRESS) {
                List<String> kits = getDonateKitsForPlayer(player);
                boolean onCooldown = false;
                if (plugin.getPlayerKitsHook() != null && plugin.getPlayerKitsHook().isEnabled()) {
                    for (String k : kits) {
                        if (plugin.getPlayerKitsHook().isKitOnCooldown(player, k)) {
                            onCooldown = true;
                            break;
                        }
                    }
                }
                if (onCooldown) {
                    markCompleted(player, data, levelId, quest, entry);
                    return;
                }
            }
        }

        if (quest.getType() == QuestType.ACCOUNT_LINK
                && entry.getStatus() != QuestStatus.CLAIMED
                && entry.getStatus() != QuestStatus.COMPLETED) {
            handleAccountLinkClick(player, data, level, quest, entry);
            return;
        }

        switch (entry.getStatus()) {
            case AVAILABLE: {
                boolean instant = quest.getType() == QuestType.MANUAL_CLICK;
                entry.setStatus(instant ? QuestStatus.COMPLETED : QuestStatus.IN_PROGRESS);
                entry.setProgress(instant ? quest.getAmount() : 0);

                Map<String, String> ph = new HashMap<>();
                ph.put("quest", ColorUtils.colorize(quest.getName()));

                if (instant) {
                    player.sendMessage(plugin.getConfigManager().getMessage("quest-completed", ph));
                    playSound(player, "quest-ready");
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("quest-activated", ph));
                    for (String line : quest.getLore()) {
                        player.sendMessage(ColorUtils.colorize(line));
                    }
                    playSound(player, "quest-activated");
                }

                plugin.getDao().saveProgress(player.getUniqueId(), levelId, questId,
                        entry.getStatus(), entry.getProgress());
                if (quest.getType() == QuestType.COLLECT_FLOWERS) {
                    updateFlowerCollection(player);
                }
                break;
            }
            case IN_PROGRESS: {
                Map<String, String> ph = new HashMap<>();
                ph.put("quest", ColorUtils.colorize(quest.getName()));
                ph.put("progress", entry.getProgress() + "/" + quest.getAmount());
                player.sendMessage(plugin.getConfigManager().getMessage("quest-in-progress", ph));
                break;
            }
            case COMPLETED:
                claimReward(player, data, level, quest, entry);
                break;
            case CLAIMED:
                player.sendMessage(plugin.getConfigManager().getMessage("quest-already-claimed"));
                break;
            default:
                break;
        }
    }

    /**
     * Квест «Привязка аккаунта»: активен и проходится привязкой Telegram.
     *  - аккаунт уже привязан — задание выполняется сразу;
     *  - не привязан — игроку показывается подсказка (/addtg), задание
     *    активируется и выполнится автоматически, как только появится привязка
     *    (периодическая проверка через HTTP API ElytrixAuth, см. PeriodicTaskManager);
     *  - auth.http.enabled: false (режим без ElytrixAuth) — задание
     *    засчитывается сразу;
     *  - auth.http включён, но url/key не настроены — задание ждёт настройки
     *    и не засчитывается (защита от случайной выдачи награды).
     */
    private void handleAccountLinkClick(Player player, PlayerQuestData data,
                                        QuestLevel level, Quest quest, QuestEntry entry) {
        LinkChecker checker = plugin.getLinkChecker();
        if (checker == null || checker.isStandaloneMode()) {
            // auth.http.enabled: false — режим без ElytrixAuth: засчитываем сразу
            markCompleted(player, data, level.getId(), quest, entry);
            return;
        }
        if (!checker.isChecking()) {
            // включено, но url/key не настроены: квест ждёт настройки администратора
            Map<String, String> ph0 = new HashMap<>();
            ph0.put("quest", ColorUtils.colorize(quest.getName()));
            player.sendMessage(plugin.getConfigManager()
                    .getMessage("quest-account-link-not-ready", ph0));
            playSound(player, "level-locked");
            return;
        }

        // активируем задание, если игрок просто открыл его
        if (entry.getStatus() == QuestStatus.AVAILABLE) {
            entry.setStatus(QuestStatus.IN_PROGRESS);
            entry.setProgress(0);
            plugin.getDao().saveProgress(player.getUniqueId(), level.getId(), quest.getId(),
                    entry.getStatus(), entry.getProgress());
        }

        Map<String, String> ph = new HashMap<>();
        ph.put("quest", ColorUtils.colorize(quest.getName()));

        // Свежий кэш «привязан» — засчитываем мгновенно, без сети.
        Boolean cached = checker.cachedLinked(player.getName(), 30_000L);
        if (Boolean.TRUE.equals(cached)) {
            markCompleted(player, data, level.getId(), quest, entry);
            return;
        }

        // Иначе ВСЕГДА живая проверка: кэш «не привязан» не должен перекрывать
        // только что выполненную привязку (иначе клик сразу после /addtg снова
        // пишет «нужно привязать», хотя аккаунт уже привязан).
        final String name = player.getName();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final LinkChecker.Result result;
            try {
                result = checker.checkNow(name);
            } catch (Throwable t) {
                return; // проверка упала — задание ждёт следующего клика
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                PlayerQuestData d = cache.get(player.getUniqueId());
                if (d == null) {
                    return;
                }
                QuestEntry en = d.getEntry(level.getId(), quest.getId());
                if (en == null) {
                    return;
                }
                QuestStatus st = en.getStatus();
                if (st != QuestStatus.AVAILABLE && st != QuestStatus.IN_PROGRESS) {
                    return;
                }
                switch (result) {
                    case LINKED:
                        markCompleted(player, d, level.getId(), quest, en);
                        break;
                    case NOT_LINKED:
                        player.sendMessage(plugin.getConfigManager()
                                .getMessage("quest-account-link-not-linked", ph));
                        playSound(player, "quest-activated");
                        break;
                    case ERROR:
                    default:
                        // прокси недоступен/не настроен — не пишем «привяжи аккаунт»,
                        // а честно говорим, что проверка не удалась
                        player.sendMessage(plugin.getConfigManager()
                                .getMessage("quest-account-link-check-failed", ph));
                        playSound(player, "level-locked");
                        break;
                }
            });
        });
    }

    /** Автовыполнение квестов «Привязка аккаунта» (вызывается после подтверждения привязки). */
    public void completeAccountLinkQuests(Player player) {
        PlayerQuestData data = cache.get(player.getUniqueId());
        if (data == null) {
            return;
        }
        for (QuestLevel level : levels.values()) {
            if (!isLevelUnlocked(data, level.getId())) {
                continue;
            }
            for (Quest quest : level.getQuests().values()) {
                if (quest.getType() != QuestType.ACCOUNT_LINK) {
                    continue;
                }
                if (!isQuestUnlocked(data, level.getId(), quest.getId())) {
                    continue;
                }
                QuestEntry entry = data.getEntry(level.getId(), quest.getId());
                if (entry == null) {
                    continue;
                }
                QuestStatus st = entry.getStatus();
                if (st == QuestStatus.AVAILABLE || st == QuestStatus.IN_PROGRESS) {
                    markCompleted(player, data, level.getId(), quest, entry);
                }
            }
        }
    }

    /** Есть ли у игрока незавершённый квест «Привязка аккаунта» (нужно ли опрашивать API). */
    public boolean hasPendingAccountLink(Player player) {
        PlayerQuestData data = cache.get(player.getUniqueId());
        if (data == null) {
            return false;
        }
        for (QuestLevel level : levels.values()) {
            if (!isLevelUnlocked(data, level.getId())) {
                continue;
            }
            for (Quest quest : level.getQuests().values()) {
                if (quest.getType() != QuestType.ACCOUNT_LINK) {
                    continue;
                }
                if (!isQuestUnlocked(data, level.getId(), quest.getId())) {
                    continue;
                }
                QuestEntry entry = data.getEntry(level.getId(), quest.getId());
                if (entry == null) {
                    continue;
                }
                QuestStatus st = entry.getStatus();
                if (st == QuestStatus.AVAILABLE || st == QuestStatus.IN_PROGRESS) {
                    return true;
                }
            }
        }
        return false;
    }

    private void markCompleted(Player player, PlayerQuestData data, int levelId, Quest quest, QuestEntry entry) {
        entry.setStatus(QuestStatus.COMPLETED);
        entry.setProgress(quest.getAmount());
        long cooldownUntil = System.currentTimeMillis() + 5L * 60L * 1000L;
        data.setCooldownUntil(levelId, quest.getId(), cooldownUntil);
        plugin.getDao().saveProgress(player.getUniqueId(), levelId, quest.getId(),
                entry.getStatus(), entry.getProgress(), cooldownUntil);

        Map<String, String> ph = new HashMap<>();
        ph.put("quest", ColorUtils.colorize(quest.getName()));
        player.sendMessage(plugin.getConfigManager().getMessage("quest-completed", ph));
        playSound(player, "quest-ready");
    }

    private void claimReward(Player player, PlayerQuestData data, QuestLevel level, Quest quest, QuestEntry entry) {
        entry.setStatus(QuestStatus.CLAIMED);
        plugin.getRewardManager().giveRewards(player, quest.getRewardCommands());

        Map<String, String> ph = new HashMap<>();
        ph.put("quest", ColorUtils.colorize(quest.getName()));
        player.sendMessage(plugin.getConfigManager().getMessage("reward-claimed", ph));
        playSound(player, "reward-claimed");

        plugin.getDao().saveProgress(player.getUniqueId(), level.getId(), quest.getId(),
                entry.getStatus(), entry.getProgress(), data.getCooldownUntil(level.getId(), quest.getId()));

        if (isLevelFullyClaimedExceptMega(data, level.getId()) && !data.isMegaClaimed(level.getId())) {
            data.setMegaClaimed(level.getId(), true);
            plugin.getRewardManager().giveRewards(player, level.getMegaRewardCommands());

            Map<String, String> ph2 = new HashMap<>();
            ph2.put("level", ColorUtils.colorize(level.getName()));
            player.sendMessage(plugin.getConfigManager().getMessage("level-completed", ph2));
            playSound(player, "level-completed");

            plugin.getDao().saveMega(player.getUniqueId(), level.getId(), true);

            QuestLevel next = getNextLevel(level.getId());
            if (next != null) {
                Map<String, String> ph3 = new HashMap<>();
                ph3.put("level", ColorUtils.colorize(next.getName()));
                player.sendMessage(plugin.getConfigManager().getMessage("level-unlocked", ph3));
            }
        }
    }

    private void playSound(Player player, String key) {
        String soundName = plugin.getConfig().getString("sounds." + key, null);
        if (soundName == null) return;
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }

    public void incrementProgress(Player player, QuestType type, Predicate<Quest> matcher, int amountToAdd) {
        PlayerQuestData data = cache.get(player.getUniqueId());
        if (data == null || amountToAdd <= 0) return;

        for (QuestLevel level : levels.values()) {
            if (!isLevelUnlocked(data, level.getId())) continue;
            for (Quest quest : level.getQuests().values()) {
                if (quest.getType() != type || !matcher.test(quest)) continue;
                if (!isQuestUnlocked(data, level.getId(), quest.getId())) continue;

                QuestEntry entry = data.getEntry(level.getId(), quest.getId());
                if (entry == null || entry.getStatus() != QuestStatus.IN_PROGRESS) continue;

                int newProgress = Math.min(entry.getProgress() + amountToAdd, quest.getAmount());
                entry.setProgress(newProgress);

                long cooldownUntil = 0L;
                if (newProgress >= quest.getAmount()) {
                    entry.setStatus(QuestStatus.COMPLETED);
                    cooldownUntil = System.currentTimeMillis() + 5L * 60L * 1000L;
                    data.setCooldownUntil(level.getId(), quest.getId(), cooldownUntil);
                    notifyReady(player);
                }

                plugin.getDao().saveProgress(player.getUniqueId(), level.getId(), quest.getId(),
                        entry.getStatus(), entry.getProgress(), cooldownUntil);
            }
        }
    }

    public void setProgressAbsolute(Player player, QuestType type, Predicate<Quest> matcher, int value) {
        PlayerQuestData data = cache.get(player.getUniqueId());
        if (data == null) return;

        for (QuestLevel level : levels.values()) {
            if (!isLevelUnlocked(data, level.getId())) continue;
            for (Quest quest : level.getQuests().values()) {
                if (quest.getType() != type || !matcher.test(quest)) continue;
                if (!isQuestUnlocked(data, level.getId(), quest.getId())) continue;

                QuestEntry entry = data.getEntry(level.getId(), quest.getId());
                if (entry == null || entry.getStatus() != QuestStatus.IN_PROGRESS) continue;

                int newProgress = Math.min(Math.max(value, entry.getProgress()), quest.getAmount());
                entry.setProgress(newProgress);

                long cooldownUntil = 0L;
                if (newProgress >= quest.getAmount()) {
                    entry.setStatus(QuestStatus.COMPLETED);
                    cooldownUntil = System.currentTimeMillis() + 5L * 60L * 1000L;
                    data.setCooldownUntil(level.getId(), quest.getId(), cooldownUntil);
                    notifyReady(player);
                }

                plugin.getDao().saveProgress(player.getUniqueId(), level.getId(), quest.getId(),
                        entry.getStatus(), entry.getProgress(), cooldownUntil);
            }
        }
    }

    private void notifyReady(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessage("quest-ready"));
        playSound(player, "quest-ready");
    }

    public void addDistanceProgress(Player player, QuestType type, double distance) {
        if (distance <= 0) return;
        Map<UUID, Double> values = type == QuestType.SWIM_DISTANCE ? swimmingDistance : netherDistance;
        double total = values.getOrDefault(player.getUniqueId(), 0.0D) + distance;
        int whole = (int) total;
        values.put(player.getUniqueId(), total - whole);
        if (whole > 0) incrementProgress(player, type, q -> true, whole);
    }

    public void markAwaitingWarp(Player player) {
        awaitingWarpTeleport.put(player.getUniqueId(), System.currentTimeMillis() + WARP_TELEPORT_WINDOW_MS);
    }

    public void handleWarpTeleport(Player player) {
        Long until = awaitingWarpTeleport.get(player.getUniqueId());
        if (until == null || System.currentTimeMillis() > until) {
            awaitingWarpTeleport.remove(player.getUniqueId());
            return;
        }
        awaitingWarpTeleport.remove(player.getUniqueId());
        incrementProgress(player, QuestType.WARP_TELEPORT, q -> true, 1);
    }

    public void handleDonateReceive(String playerName) {
        if (playerName == null || playerName.isEmpty()) return;

        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase(playerName)) {
                    player = p;
                    break;
                }
            }
        }
        if (player == null) return;

        incrementProgress(player, QuestType.DONATE_RECEIVE, q -> true, 1);
    }

    public void handleKitClaim(Player player, String kitName) {
        if (player == null || kitName == null || kitName.isEmpty()) return;

        String kit = kitName.toLowerCase(Locale.ROOT).trim();
        if (kit.equals("claim") || kit.equals("get") || kit.equals("preview")
                || kit.equals("open") || kit.equals("list") || kit.equals("help") || kit.equals("gui")) {
            return;
        }

        if (!isDonateKitForPlayer(player, kit)) return;

        incrementProgress(player, QuestType.KIT_CLAIM, q -> {
            if (q.getStringData() == null || q.getStringData().isEmpty()) return true;
            return q.getStringData().equalsIgnoreCase(kit);
        }, 1);
    }

    public List<String> getDonateKitsForPlayer(Player player) {
        List<String> kits = new ArrayList<>();
        String primaryGroup = resolvePlayerKitName(player);
        if (primaryGroup != null && !isIgnoredKit(primaryGroup)) {
            kits.add(primaryGroup.toLowerCase(Locale.ROOT));
        }

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String p = info.getPermission().toLowerCase(Locale.ROOT);
            if (p.startsWith("playerkits.kit.")) {
                String kit = p.substring("playerkits.kit.".length());
                if (!kit.isEmpty() && !kit.equals("*") && !isIgnoredKit(kit)) {
                    if (!kits.contains(kit)) kits.add(kit);
                }
            }
            if (p.startsWith("group.")) {
                String grp = p.substring("group.".length());
                if (!grp.isEmpty() && !isIgnoredKit(grp)) {
                    if (!kits.contains(grp)) kits.add(grp);
                }
            }
        }
        return kits;
    }

    private boolean isIgnoredKit(String name) {
        if (name == null) return true;
        String lower = name.toLowerCase(Locale.ROOT).trim();
        return lower.equals("default") || lower.equals("player") || lower.equals("member")
                || lower.equals("start") || lower.equals("starter") || lower.equals("food")
                || lower.equals("bonus") || lower.equals("user") || lower.equals("admin")
                || lower.equals("owner") || lower.equals("moder") || lower.equals("helper");
    }

    public boolean isDonateKitForPlayer(Player player, String kitName) {
        if (kitName == null || isIgnoredKit(kitName)) return false;
        String kit = kitName.toLowerCase(Locale.ROOT).trim();

        List<String> validKits = getDonateKitsForPlayer(player);
        if (validKits.contains(kit)) {
            return true;
        }

        if (player.hasPermission("playerkits.kit." + kit) || player.isOp()) {
            return true;
        }

        return false;
    }

    public String resolvePlayerKitName(Player player) {
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = providerClass.getMethod("get").invoke(null);
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Object user = userManager.getClass()
                    .getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUniqueId());
            if (user != null) {
                Object groupObj = user.getClass().getMethod("getPrimaryGroup").invoke(user);
                if (groupObj instanceof String) {
                    String group = ((String) groupObj);
                    if (group != null && !group.isEmpty() && !group.equalsIgnoreCase("default")) {
                        return group.toLowerCase(Locale.ROOT);
                    }
                }
            }
        } catch (Throwable ignored) {}

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String p = info.getPermission().toLowerCase(Locale.ROOT);
            if (p.startsWith("group.") && !p.equals("group.default")) {
                return p.substring("group.".length());
            }
        }

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String p = info.getPermission().toLowerCase(Locale.ROOT);
            if (p.startsWith("playerkits.kit.")) {
                String kit = p.substring("playerkits.kit.".length());
                if (kit.isEmpty() || kit.equals("*")) continue;
                if (isIgnoredKit(kit)) continue;
                return kit;
            }
        }
        return null;
    }

    /** Counts successful combat hits against different players, not kills. */
    public void handleCombatHit(Player attacker, Player victim) {
        if (attacker == null || victim == null || attacker.getUniqueId().equals(victim.getUniqueId())) return;
        PlayerQuestData data = cache.get(attacker.getUniqueId());
        if (data == null) return;
        for (QuestLevel level : levels.values()) {
            if (!isLevelUnlocked(data, level.getId())) continue;
            for (Quest quest : level.getQuests().values()) {
                if (quest.getType() != QuestType.COMBAT_PLAYER_UNIQUE || !isQuestUnlocked(data, level.getId(), quest.getId())) continue;
                QuestEntry entry = data.getEntry(level.getId(), quest.getId());
                if (entry == null || entry.getStatus() != QuestStatus.IN_PROGRESS) continue;
                String key = "combat:" + level.getId() + ":" + quest.getId();
                Set<UUID> set = uniqueKills.computeIfAbsent(attacker.getUniqueId(), u -> new ConcurrentHashMap<>())
                        .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
                if (!set.add(victim.getUniqueId())) continue;
                int value = Math.min(set.size(), quest.getAmount());
                entry.setProgress(value);
                long cooldownUntil = 0L;
                if (value >= quest.getAmount()) {
                    entry.setStatus(QuestStatus.COMPLETED);
                    cooldownUntil = System.currentTimeMillis() + 5L * 60L * 1000L;
                    data.setCooldownUntil(level.getId(), quest.getId(), cooldownUntil);
                    notifyReady(attacker);
                }
                plugin.getDao().saveProgress(attacker.getUniqueId(), level.getId(), quest.getId(),
                        entry.getStatus(), entry.getProgress(), cooldownUntil);
            }
        }
    }

    public void handleUniquePlayerKill(Player killer, Player victim) {
        if (killer == null || victim == null) return;
        if (killer.getUniqueId().equals(victim.getUniqueId())) return;

        PlayerQuestData data = cache.get(killer.getUniqueId());
        if (data == null) return;

        for (QuestLevel level : levels.values()) {
            if (!isLevelUnlocked(data, level.getId())) continue;
            for (Quest quest : level.getQuests().values()) {
                if (quest.getType() != QuestType.KILL_PLAYER_UNIQUE) continue;
                if (!isQuestUnlocked(data, level.getId(), quest.getId())) continue;

                QuestEntry entry = data.getEntry(level.getId(), quest.getId());
                if (entry == null || entry.getStatus() != QuestStatus.IN_PROGRESS) continue;

                String regionName = quest.getStringData();
                if (regionName == null || regionName.isEmpty()) regionName = "pvp";

                if (plugin.getWorldGuardHook() == null || !plugin.getWorldGuardHook().isEnabled()) continue;

                boolean killerIn = plugin.getWorldGuardHook().isInRegion(killer, regionName);
                boolean victimIn = plugin.getWorldGuardHook().isInRegion(victim, regionName);
                if (!killerIn && !victimIn) continue;

                String key = level.getId() + ":" + quest.getId();
                Set<UUID> set = uniqueKills
                        .computeIfAbsent(killer.getUniqueId(), u -> new ConcurrentHashMap<>())
                        .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());

                if (!set.add(victim.getUniqueId())) continue;

                int newProgress = Math.min(set.size(), quest.getAmount());
                entry.setProgress(newProgress);

                long cooldownUntil = 0L;
                if (newProgress >= quest.getAmount()) {
                    entry.setStatus(QuestStatus.COMPLETED);
                    cooldownUntil = System.currentTimeMillis() + 5L * 60L * 1000L;
                    data.setCooldownUntil(level.getId(), quest.getId(), cooldownUntil);
                    notifyReady(killer);
                }

                plugin.getDao().saveProgress(killer.getUniqueId(), level.getId(), quest.getId(),
                        entry.getStatus(), entry.getProgress(), cooldownUntil);
            }
        }
    }

    public long getQuestCooldownRemaining(PlayerQuestData data, int levelId, int questId) {
        return Math.max(0L, data.getCooldownUntil(levelId, questId) - System.currentTimeMillis());
    }

    /** Баланс PlayerPoints для задания накопления коинов. */
    public int getPlayerPointsBalance(Player player) {
        try {
            Object playerPoints = Bukkit.getPluginManager().getPlugin("PlayerPoints");
            if (playerPoints == null) return -1;
            Object api = playerPoints.getClass().getMethod("getAPI").invoke(playerPoints);
            try {
                Object result = api.getClass().getMethod("look", UUID.class).invoke(api, player.getUniqueId());
                if (result instanceof Number) return ((Number) result).intValue();
            } catch (Throwable ignored) {
                try {
                    Object result = api.getClass().getMethod("look", String.class).invoke(api, player.getName());
                    if (result instanceof Number) return ((Number) result).intValue();
                } catch (Throwable ignoredByName) {
                    return -1;
                }
            }
            return -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public boolean hasSunShacklesBook(Player player) {
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            String text = (meta.hasDisplayName() ? meta.getDisplayName() : "").toLowerCase(Locale.ROOT);
            if (text.contains("солнеч") || text.contains("sun shackles")) return true;
            if (meta.hasLore() && meta.getLore() != null) {
                for (String line : meta.getLore()) {
                    String lower = line.toLowerCase(Locale.ROOT);
                    if (lower.contains("солнеч") || lower.contains("sun shackles")) return true;
                }
            }
        }
        return false;
    }

    public boolean hasSunHelmet(Player player) {
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.GOLDEN_HELMET || !item.hasItemMeta()) continue;
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            String text = (meta.hasDisplayName() ? meta.getDisplayName() : "").toLowerCase(Locale.ROOT);
            if (text.contains("солн") || text.contains("sun")) return true;
            if (meta.hasLore() && meta.getLore() != null) {
                for (String line : meta.getLore()) {
                    String lower = line.toLowerCase(Locale.ROOT);
                    if (lower.contains("солнеч") || lower.contains("sun shackles")) return true;
                }
            }
        }
        return false;
    }

    public void updateFlowerCollection(Player player) {
        PlayerQuestData data = cache.get(player.getUniqueId());
        if (data == null) return;
        final Set<Material> flowers = ConcurrentHashMap.newKeySet();
        Material[] types = {Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID,
                Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
                Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY,
                Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY, Material.SUNFLOWER,
                Material.LILAC, Material.ROSE_BUSH, Material.PEONY};
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            for (Material flower : types) if (item.getType() == flower) flowers.add(flower);
        }
        setProgressAbsolute(player, QuestType.COLLECT_FLOWERS, q -> true, flowers.size());
    }

    public void resetPlayer(UUID uuid) {
        PlayerQuestData fresh = new PlayerQuestData(uuid);
        cache.put(uuid, fresh);
        awaitingWarpTeleport.remove(uuid);
        uniqueKills.remove(uuid);
        plugin.getDao().resetPlayer(uuid);
    }

    public void forceComplete(Player player, int levelId, int questId) {
        PlayerQuestData data = cache.get(player.getUniqueId());
        Quest quest = getQuest(levelId, questId);
        if (data == null || quest == null) return;

        QuestEntry entry = data.getOrCreate(levelId, questId);
        entry.setStatus(QuestStatus.COMPLETED);
        entry.setProgress(quest.getAmount());
        plugin.getDao().saveProgress(player.getUniqueId(), levelId, questId,
                entry.getStatus(), entry.getProgress());
    }

    public void forceUnlock(Player player, int targetLevel) {
        PlayerQuestData data = cache.get(player.getUniqueId());
        if (data == null) return;

        for (QuestLevel level : levels.values()) {
            if (level.getId() >= targetLevel) continue;
            for (Quest q : level.getQuests().values()) {
                QuestEntry entry = data.getOrCreate(level.getId(), q.getId());
                entry.setStatus(QuestStatus.CLAIMED);
                entry.setProgress(q.getAmount());
                plugin.getDao().saveProgress(player.getUniqueId(), level.getId(), q.getId(),
                        entry.getStatus(), entry.getProgress());
            }
            data.setMegaClaimed(level.getId(), true);
            plugin.getDao().saveMega(player.getUniqueId(), level.getId(), true);
        }
    }

    public void sendInfo(CommandSender sender, Player target) {
        PlayerQuestData data = cache.get(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(ColorUtils.colorize("&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &cДанные игрока ещё не загружены."));
            return;
        }

        sender.sendMessage(ColorUtils.colorize("&f☁ &#F8BEFBᴇ&#F6BEFBʟ&#F3BEFBʏ&#F1BFFBᴛ&#EEBFFBʀ&#ECBFFBɪ&#E9BFFBx &7» &#F8BEFBПрогресс игрока: &#F8BEFB" + target.getName()));

        for (QuestLevel level : levels.values()) {
            boolean unlocked = isLevelUnlocked(data, level.getId());
            String levelStatus = unlocked ? "&aОткрыт" : "&cЗаблокирован";

            sender.sendMessage(ColorUtils.colorize("&#F8BEFB&l┃ &fУровень " + level.getId() + " (&f" + level.getName() + "&f): " + levelStatus));

            for (Quest q : level.getQuests().values()) {
                QuestStatus status = getStatus(data, level.getId(), q.getId());
                QuestEntry entry = data.getEntry(level.getId(), q.getId());
                int prog = entry != null ? entry.getProgress() : 0;

                String questStatusStr;
                switch (status) {
                    case CLAIMED: questStatusStr = "&aПолучено"; break;
                    case COMPLETED: questStatusStr = "&aГотово"; break;
                    case IN_PROGRESS: questStatusStr = "&eВ процессе"; break;
                    case AVAILABLE: questStatusStr = "&#F8BEFBДоступно"; break;
                    default: questStatusStr = "&cЗаблокировано"; break;
                }

                sender.sendMessage(ColorUtils.colorize("&#F8BEFB&l┃   &7● &f" + q.getName() + " &7— " + questStatusStr + " &7(&f" + prog + "&7/&f" + q.getAmount() + "&7)"));
            }
        }
    }
}