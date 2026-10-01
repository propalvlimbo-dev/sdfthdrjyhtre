package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class KillEntityTaskType extends TaskType {
    public KillEntityTaskType(ElytrixBattlePass plugin) {
        super("KILL_ENTITY", plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) {
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

        EntityType required = EntityType.valueOf(String.valueOf(task.getValues().get("entity")).toUpperCase());
        if (event.getEntityType() == required) {
            plugin.getQuestManager().progressQuest(user, 1);
        }
    }
}