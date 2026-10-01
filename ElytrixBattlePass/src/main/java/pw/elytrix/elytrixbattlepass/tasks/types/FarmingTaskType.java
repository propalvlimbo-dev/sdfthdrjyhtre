package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class FarmingTaskType extends TaskType {
    public FarmingTaskType(ElytrixBattlePass plugin) {
        super("FARMING", plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        User user = plugin.getUserManager().getUserFromCache(player);
        if (user == null || user.getCurrentQuest() == null) {
            return;
        }

        Task task = user.getCurrentQuest().getTask();
        if (!task.isActive() || !task.getType().equals(getName())) {
            return;
        }

        Object materialValue = task.getValues().get("material");
        if (!(materialValue instanceof Material material)) {
            return;
        }

        Block block = event.getBlock();
        if (block.getType() != material) {
            return;
        }

        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable && ageable.getAge() == ageable.getMaximumAge()) {
            plugin.getQuestManager().progressQuest(user, 1);
        }
    }
}