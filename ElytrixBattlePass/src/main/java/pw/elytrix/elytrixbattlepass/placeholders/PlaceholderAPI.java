package pw.elytrix.elytrixbattlepass.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.user.User;

public class PlaceholderAPI extends PlaceholderExpansion {
    private final ElytrixBattlePass plugin;

    public PlaceholderAPI(ElytrixBattlePass plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "elytrixbattlepass";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Smusl.pwn,Smail";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        User user = plugin.getUserManager().getUserFromCache(player.getUniqueId());
        if (user == null) {
            return "0";
        }

        return switch (params.toLowerCase()) {
            case "points" -> String.valueOf(user.getPoints());
            case "earned_today" -> String.valueOf(user.getEarnedToday());
            case "earned_total" -> String.valueOf(user.getEarnedTotal());
            case "completed_cycle" -> String.valueOf(user.getCompletedInCycle());
            default -> "";
        };
    }
}