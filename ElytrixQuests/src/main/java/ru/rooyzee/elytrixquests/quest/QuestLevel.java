package ru.rooyzee.elytrixquests.quest;

import java.util.Map;
import java.util.TreeMap;

public class QuestLevel {

    private final int id;
    private final String name;
    private final Map<Integer, Quest> quests = new TreeMap<>();

    public QuestLevel(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addQuest(Quest quest) {
        quests.put(quest.getId(), quest);
    }

    public Quest getQuest(int id) {
        return quests.get(id);
    }

    public Map<Integer, Quest> getQuests() { return quests; }
    public int getId() { return id; }
    public String getName() { return name; }
}
