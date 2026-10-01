package ru.rooyzee.elytrixquests.quest;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class QuestLevel {

    private final int id;
    private final String name;
    private final Map<Integer, Quest> quests = new TreeMap<>();
    private final List<String> megaRewardCommands;
    private final String megaRewardMessage;

    public QuestLevel(int id, String name, List<String> megaRewardCommands, String megaRewardMessage) {
        this.id = id;
        this.name = name;
        this.megaRewardCommands = megaRewardCommands;
        this.megaRewardMessage = megaRewardMessage;
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
    public List<String> getMegaRewardCommands() { return megaRewardCommands; }
    public String getMegaRewardMessage() { return megaRewardMessage; }
}