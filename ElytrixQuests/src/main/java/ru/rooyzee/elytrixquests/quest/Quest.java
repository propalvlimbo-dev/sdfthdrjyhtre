package ru.rooyzee.elytrixquests.quest;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

public class Quest {

    private final int id;
    private final int levelId;
    private final String name;
    private final List<String> lore;
    private final Material icon;
    private final QuestType type;
    private final int amount;
    private final Material material;
    private final EntityType entityType;
    private final String stringData;
    private final List<String> rewardCommands;

    public Quest(int id, int levelId, String name, List<String> lore, Material icon, QuestType type,
                 int amount, Material material, EntityType entityType, String stringData,
                 List<String> rewardCommands) {
        this.id = id;
        this.levelId = levelId;
        this.name = name;
        this.lore = lore;
        this.icon = icon;
        this.type = type;
        this.amount = Math.max(amount, 1);
        this.material = material;
        this.entityType = entityType;
        this.stringData = stringData;
        this.rewardCommands = rewardCommands;
    }

    public int getId() { return id; }
    public int getLevelId() { return levelId; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public Material getIcon() { return icon; }
    public QuestType getType() { return type; }
    public int getAmount() { return amount; }
    public Material getMaterial() { return material; }
    public EntityType getEntityType() { return entityType; }
    public String getStringData() { return stringData; }
    public List<String> getRewardCommands() { return rewardCommands; }
}