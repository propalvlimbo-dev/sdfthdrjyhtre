package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.playerblocktracker.PlayerBlockTracker;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class BlockBreakTaskType extends TaskType {
    public BlockBreakTaskType(ElytrixBattlePass plugin) {
        super("BLOCK_BREAK", plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (PlayerBlockTracker.isTracked(event.getBlock())) {
            return;
        }

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
        if (materialValue instanceof Material material && event.getBlock().getType() == material) {
            plugin.getQuestManager().progressQuest(user, 1);
        }
    }
}