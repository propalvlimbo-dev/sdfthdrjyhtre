package pw.elytrix.elytrixbattlepass.quest;

public enum QuestDifficulty {
    EASY,
    MEDIUM,
    HARD;

    public static QuestDifficulty fromString(String value) {
        if (value == null) {
            return EASY;
        }
        try {
            return QuestDifficulty.valueOf(value.trim().toUpperCase());
        } catch (Exception ignored) {
            return EASY;
        }
    }
}