package pw.elytrix.elytrixbattlepass.user;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import pw.elytrix.elytrixbattlepass.integration.ElytrixSubscribeAPI;
import pw.elytrix.elytrixbattlepass.quest.Quest;

public class User {
    private final UUID uuid;
    private String name;
    private transient Player player;
    private int completedTasksTotal;
    private int completedInCycle;
    private Quest currentQuest;
    private int points;
    private int earnedToday;
    private int earnedTotal;
    private long lastDailyResetDay = -1L;
    private long cycleCooldownUntil;
    private final LinkedHashSet<String> usedTaskIds = new LinkedHashSet<>();

    public User(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public static User create(Player player) {
        User user = new User(player.getUniqueId(), player.getName());
        user.attachPlayer(player);
        return user;
    }

    public void attachPlayer(Player player) {
        this.player = player;
        this.name = player.getName();
    }

    public boolean isPremium() {
        return this.player != null && ElytrixSubscribeAPI.hasSubscription(this.player);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Player getPlayer() {
        return player;
    }

    public int getCompletedTasksTotal() {
        return completedTasksTotal;
    }

    public int getCompletedInCycle() {
        return completedInCycle;
    }

    public Quest getCurrentQuest() {
        return currentQuest;
    }

    public int getPoints() {
        return points;
    }

    public int getEarnedToday() {
        return earnedToday;
    }

    public int getEarnedTotal() {
        return earnedTotal;
    }

    public long getLastDailyResetDay() {
        return lastDailyResetDay;
    }

    public long getCycleCooldownUntil() {
        return cycleCooldownUntil;
    }

    public Set<String> getUsedTaskIds() {
        return usedTaskIds;
    }

    public int getCompletedTasksCount() {
        return completedTasksTotal;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCompletedTasksTotal(int completedTasksTotal) {
        this.completedTasksTotal = completedTasksTotal;
    }

    public void setCompletedInCycle(int completedInCycle) {
        this.completedInCycle = completedInCycle;
    }

    public void setCurrentQuest(Quest currentQuest) {
        this.currentQuest = currentQuest;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setEarnedToday(int earnedToday) {
        this.earnedToday = earnedToday;
    }

    public void setEarnedTotal(int earnedTotal) {
        this.earnedTotal = earnedTotal;
    }

    public void setLastDailyResetDay(long lastDailyResetDay) {
        this.lastDailyResetDay = lastDailyResetDay;
    }

    public void setCycleCooldownUntil(long cycleCooldownUntil) {
        this.cycleCooldownUntil = cycleCooldownUntil;
    }

    public void setCompletedTasksCount(int completedTasksCount) {
        this.completedTasksTotal = completedTasksCount;
    }

    public String serializeUsedTaskIds() {
        return String.join(",", usedTaskIds);
    }

    public void deserializeUsedTaskIds(String raw) {
        usedTaskIds.clear();
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String value : raw.split(",")) {
            String id = value.trim();
            if (!id.isEmpty()) {
                usedTaskIds.add(id);
            }
        }
    }
}