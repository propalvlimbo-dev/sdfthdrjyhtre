package pw.elytrix.elytrixbattlepass.tasks.types;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class CraftingTaskType extends TaskType {
    public CraftingTaskType(ElytrixBattlePass plugin) {
        super("CRAFTING", plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        if (event.getAction() == InventoryAction.NOTHING) {
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
        if (!(materialValue instanceof Material material) || event.getCurrentItem().getType() != material) {
            return;
        }

        int amount = event.getCurrentItem().getAmount();

        if (event.isShiftClick() && event.getClick() != ClickType.CONTROL_DROP) {
            int multiplier = event.getInventory().getMaxStackSize();
            for (ItemStack itemStack : event.getInventory().getMatrix()) {
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    continue;
                }
                multiplier = Math.min(multiplier, itemStack.getAmount());
            }
            amount *= multiplier;
            amount = Math.min(amount, getAvailableSpace(player, event.getCurrentItem()));
            if (amount <= 0) {
                return;
            }
        }

        plugin.getQuestManager().progressQuest(user, amount);
    }

    private int getAvailableSpace(Player player, ItemStack item) {
        int available = 0;
        PlayerInventory inventory = player.getInventory();

        Map<Integer, ? extends ItemStack> same = inventory.all(item.getType());
        for (ItemStack existing : same.values()) {
            if (item.isSimilar(existing)) {
                available += item.getMaxStackSize() - existing.getAmount();
            }
        }

        for (ItemStack existing : inventory.getStorageContents()) {
            if (existing == null) {
                available += item.getMaxStackSize();
            }
        }

        return available;
    }
}