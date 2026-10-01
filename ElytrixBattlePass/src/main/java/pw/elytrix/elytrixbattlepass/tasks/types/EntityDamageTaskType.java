package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class EntityDamageTaskType extends TaskType {
    public EntityDamageTaskType(ElytrixBattlePass plugin) {
        super("ENTITY_DAMAGE", plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
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

        EntityType requiredType = EntityType.valueOf(String.valueOf(task.getValues().get("entity")).toUpperCase());
        if (event.getEntityType() == requiredType) {
            plugin.getQuestManager().progressQuest(user, Math.max(1, (int) Math.round(event.getFinalDamage())));
        }
    }
}