package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class PickupItemTaskType extends TaskType {
    public PickupItemTaskType(ElytrixBattlePass plugin) {
        super("PICKUP_ITEM", plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        User user = plugin.getUserManager().getUserFromCache(player);
        if (user == null || user.getCurrentQuest() == null) {
            return;
        }

        Task task = user.getCurrentQuest().getTask();
        if (!task.isActive() || !task.getType().equals(getName())) {
            return;
        }

        Object materialValue = task.getValues().get("material");
        if (materialValue instanceof Material material && event.getItem().getItemStack().getType() == material) {
            plugin.getQuestManager().progressQuest(user, event.getItem().getItemStack().getAmount());
        }
    }
}