package ru.rooyzee.elytrixquests.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.data.PlayerQuestData;
import ru.rooyzee.elytrixquests.data.QuestEntry;
import ru.rooyzee.elytrixquests.data.QuestStatus;
import ru.rooyzee.elytrixquests.quest.Quest;
import ru.rooyzee.elytrixquests.quest.QuestLevel;
import ru.rooyzee.elytrixquests.quest.QuestManager;
import ru.rooyzee.elytrixquests.util.ColorUtils;

public class QuestsPlaceholderExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public QuestsPlaceholderExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "elytrixquests";
    }

    @Override
    public String getAuthor() {
        return "rooyzee";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        QuestManager qm = plugin.getQuestManager();
        PlayerQuestData data = qm.getData(player.getUniqueId());
        if (data == null) return "";

        try {
            if (params.equalsIgnoreCase("current_level")) {
                return String.valueOf(qm.getCurrentLevel(data));
            }

            String[] parts = params.split("_");

            // %elytrixquests_level_1_quest_1_status%
            if (parts.length >= 4 && parts[0].equalsIgnoreCase("level") && parts[2].equalsIgnoreCase("quest")) {
                int levelId = Integer.parseInt(parts[1]);
                int questId = Integer.parseInt(parts[3]);
                String field = parts.length > 4 ? parts[4] : "status";

                Quest quest = qm.getQuest(levelId, questId);
                if (quest == null) return "";

                QuestStatus status = qm.getStatus(data, levelId, questId);

                switch (field.toLowerCase()) {
                    case "status":
                        return qm.getStatusDisplay(status);
                    case "name":
                        return ColorUtils.colorize(quest.getName());
                    case "progress": {
                        QuestEntry entry = data.getEntry(levelId, questId);
                        int prog = entry != null ? entry.getProgress() : 0;
                        return prog + "/" + quest.getAmount();
                    }
                    case "icon":
                        return quest.getIcon().name();
                    default:
                        return "";
                }
            }

            // %elytrixquests_level_1_unlocked%
            if (parts.length >= 2 && parts[0].equalsIgnoreCase("level")) {
                int levelId = Integer.parseInt(parts[1]);
                String field = parts.length > 2 ? parts[2] : "unlocked";
                QuestLevel level = qm.getLevel(levelId);

                switch (field.toLowerCase()) {
                    case "unlocked":
                        return qm.isLevelUnlocked(data, levelId) ? "yes" : "no";
                    case "name":
                        return level != null ? ColorUtils.colorize(level.getName()) : "";
                    case "completed": {
                        if (level == null) return "0";
                        long count = level.getQuests().values().stream()
                                .filter(q -> qm.getStatus(data, levelId, q.getId()) == QuestStatus.CLAIMED)
                                .count();
                        return String.valueOf(count);
                    }
                    case "total":
                        return level != null ? String.valueOf(level.getQuests().size()) : "0";
                    default:
                        return "";
                }
            }
        } catch (Exception ex) {
            return "";
        }

        return null;
    }
}