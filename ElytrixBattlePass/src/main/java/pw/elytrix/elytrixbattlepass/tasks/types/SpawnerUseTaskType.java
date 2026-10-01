package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class SpawnerUseTaskType extends TaskType {
    public SpawnerUseTaskType(ElytrixBattlePass plugin) {
        super("SPAWNER_USE", plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.SPAWNER) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !(item.getItemMeta() instanceof SpawnEggMeta eggMeta)) {
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

        EntityType required = EntityType.valueOf(String.valueOf(task.getValues().get("entity")).toUpperCase());
        EntityType eggType = eggMeta.getSpawnedType();
        CreatureSpawner spawner = (CreatureSpawner) event.getClickedBlock().getState();

        if (eggType == required && spawner.getSpawnedType() != eggType) {
            plugin.getQuestManager().progressQuest(user, 1);
        }
    }
}