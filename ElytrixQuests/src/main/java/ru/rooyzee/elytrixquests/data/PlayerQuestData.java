package ru.rooyzee.elytrixquests.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerQuestData {

    private final UUID uuid;
    private final Map<Long, QuestEntry> entries = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> megaClaimed = new ConcurrentHashMap<>();

    public PlayerQuestData(UUID uuid) {
        this.uuid = uuid;
    }

    private long key(int level, int quest) {
        return level * 1000L + quest;
    }

    public QuestEntry getEntry(int level, int quest) {
        return entries.get(key(level, quest));
    }

    public void setEntry(int level, int quest, QuestEntry entry) {
        entries.put(key(level, quest), entry);
    }

    public QuestEntry getOrCreate(int level, int quest) {
        return entries.computeIfAbsent(key(level, quest), k -> new QuestEntry(QuestStatus.AVAILABLE, 0));
    }

    public boolean isMegaClaimed(int level) {
        return megaClaimed.getOrDefault(level, false);
    }

    public void setMegaClaimed(int level, boolean value) {
        megaClaimed.put(level, value);
    }

    public UUID getUuid() {
        return uuid;
    }
}