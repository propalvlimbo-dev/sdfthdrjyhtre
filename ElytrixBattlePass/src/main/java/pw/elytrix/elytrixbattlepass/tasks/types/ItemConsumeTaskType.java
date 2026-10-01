package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class ItemConsumeTaskType extends TaskType {
    public ItemConsumeTaskType(ElytrixBattlePass plugin) {
        super("ITEM_CONSUME", plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        User user = plugin.getUserManager().getUserFromCache(player);
        if (user == null || user.getCurrentQuest() == null) {
            return;
        }

        Task task = user.getCurrentQuest().getTask();
        if (!task.isActive() || !task.getType().equals(getName())) {
            return;
        }

        ItemStack item = event.getItem();
        Object materialValue = task.getValues().get("material");
        if (!(materialValue instanceof Material requiredMaterial)) {
            return;
        }

        if (item.getType() != requiredMaterial) {
            return;
        }

        if (requiredMaterial != Material.POTION) {
            plugin.getQuestManager().progressQuest(user, 1);
            return;
        }

        if (!(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return;
        }

        PotionData data = potionMeta.getBasePotionData();
        PotionType requiredType = PotionType.valueOf(String.valueOf(task.getValues().get("potion_type")).toUpperCase());
        Boolean requireUpgraded = task.getValues().containsKey("require_upgraded")
                ? (Boolean) task.getValues().get("require_upgraded")
                : null;
        Boolean requireExtended = task.getValues().containsKey("require_extended")
                ? (Boolean) task.getValues().get("require_extended")
                : null;

        if (data.getType() != requiredType) {
            return;
        }

        if (requireUpgraded != null && data.isUpgraded() != requireUpgraded) {
            return;
        }

        if (requireExtended != null && data.isExtended() != requireExtended) {
            return;
        }

        plugin.getQuestManager().progressQuest(user, 1);
    }
}