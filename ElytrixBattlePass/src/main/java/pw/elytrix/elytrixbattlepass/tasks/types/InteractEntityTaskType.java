package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class InteractEntityTaskType extends TaskType {
    public InteractEntityTaskType(ElytrixBattlePass plugin) {
        super("INTERACT_ENTITY", plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        User user = plugin.getUserManager().getUserFromCache(player);
        if (user == null || user.getCurrentQuest() == null) {
            return;
        }

        Task task = user.getCurrentQuest().getTask();
        if (!task.isActive() || !task.getType().equals(getName())) {
            return;
        }

        ItemStack item = player.getInventory().getItem(event.getHand());
        if (item == null) {
            return;
        }

        Object materialValue = task.getValues().get("material");
        if (!(materialValue instanceof Material material)) {
            return;
        }

        if (item.getType() == material) {
            plugin.getQuestManager().progressQuest(user, 1);
        }
    }
}