package pw.elytrix.elytrixbattlepass.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.guis.ShopMenu;
import pw.elytrix.elytrixbattlepass.user.User;
import pw.elytrix.elytrixbattlepass.utils.Colorize;

public class BpShopCommand implements CommandExecutor {
    private final ElytrixBattlePass plugin;

    public BpShopCommand(ElytrixBattlePass plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }

        User user = plugin.getUserManager().getUser(player);
        if (user == null) {
            Colorize.sendMessage(player, "&cДанные не загружены, перезайдите.");
            return true;
        }

        ShopMenu.get(player, user).open(player);
        return true;
    }
}