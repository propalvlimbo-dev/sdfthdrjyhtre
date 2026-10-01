package pw.elytrix.elytrixbattlepass.quest;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;

public class Task {
    private final String id;
    private String displayName;
    private String description;
    private Material icon;
    private String type;
    private QuestDifficulty difficulty;
    private Map<String, Object> values;
    private int completed;
    private long startedAt;
    private int expirationMinutes;
    private int minPoints;
    private int maxPoints;
    private int skipCost;
    private int weight;

    public Task(
            String id,
            String displayName,
            String description,
            Material icon,
            String type,
            QuestDifficulty difficulty,
            Map<String, Object> values,
            int completed,
            long startedAt,
            int expirationMinutes,
            int minPoints,
            int maxPoints,
            int skipCost,
            int weight
    ) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.type = type;
        this.difficulty = difficulty;
        this.values = values;
        this.completed = completed;
        this.startedAt = startedAt;
        this.expirationMinutes = expirationMinutes;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.skipCost = skipCost;
        this.weight = Math.max(1, weight);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public String getType() {
        return type;
    }

    public QuestDifficulty getDifficulty() {
        return difficulty;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public int getCompleted() {
        return completed;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public int getSkipCost() {
        return skipCost;
    }

    public int getWeight() {
        return weight;
    }

    public int getRequiredAmount() {
        Object value = values.get("amount");
        if (value instanceof Integer integer) {
            return integer;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    public boolean isStarted() {
        return startedAt > 0L;
    }

    public boolean isActive() {
        return isStarted() && getRemainingMillis() > 0L;
    }

    public boolean isExpired() {
        return isStarted() && getRemainingMillis() <= 0L;
    }

    public long getRemainingMillis() {
        if (!isStarted()) {
            return expirationMinutes * 60_000L;
        }
        long end = startedAt + expirationMinutes * 60_000L;
        return end - System.currentTimeMillis();
    }

    public Task copy() {
        return new Task(
                id,
                displayName,
                description,
                icon,
                type,
                difficulty,
                new HashMap<>(values),
                completed,
                startedAt,
                expirationMinutes,
                minPoints,
                maxPoints,
                skipCost,
                weight
        );
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }
}