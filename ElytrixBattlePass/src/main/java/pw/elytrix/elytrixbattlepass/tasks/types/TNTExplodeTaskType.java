package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class TNTExplodeTaskType extends TaskType {
    public TNTExplodeTaskType(ElytrixBattlePass plugin) {
        super("TNT_EXPLODE", plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntityType() != EntityType.PRIMED_TNT) {
            return;
        }

        TNTPrimed tnt = (TNTPrimed) event.getEntity();
        if (!(tnt.getSource() instanceof Player player)) {
            return;
        }

        User user = plugin.getUserManager().getUserFromCache(player);
        if (user == null || user.getCurrentQuest() == null) {
            return;
        }

        Task task = user.getCurrentQuest().getTask();
        if (task.isActive() && task.getType().equals(getName())) {
            plugin.getQuestManager().progressQuest(user, 1);
        }
    }
}