package pw.elytrix.elytrixbattlepass.tasks.types;

import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.potion.PotionEffectType;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.quest.Task;
import pw.elytrix.elytrixbattlepass.tasks.TaskType;
import pw.elytrix.elytrixbattlepass.user.User;

public class PlayerMoveTaskType extends TaskType {
    public PlayerMoveTaskType(ElytrixBattlePass plugin) {
        super("PLAYER_MOVE", plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!hasChangedBlock(event.getFrom(), event.getTo())) {
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

        String mode = String.valueOf(task.getValues().get("mode")).toLowerCase();
        if (isNormalMoveMode(mode) && validateMode(player, mode)) {
            plugin.getQuestManager().progressQuest(user, 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!hasChangedBlock(event.getFrom(), event.getTo())) {
            return;
        }

        if (event.getVehicle().getPassengers().isEmpty()) {
            return;
        }

        event.getVehicle().getPassengers().forEach(passenger -> {
            if (!(passenger instanceof Player player)) {
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

            String mode = String.valueOf(task.getValues().get("mode")).toLowerCase();
            if (validateMode(player, mode)) {
                plugin.getQuestManager().progressQuest(user, 1);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStatistic(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() != Statistic.JUMP) {
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

        String mode = String.valueOf(task.getValues().get("mode")).toLowerCase();
        if (mode.equals("jump")) {
            plugin.getQuestManager().progressQuest(user, 1);
        }
    }

    private boolean hasChangedBlock(Location from, Location to) {
        return to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ());
    }

    private boolean isNormalMoveMode(String mode) {
        return mode.equals("walking")
                || mode.equals("running")
                || mode.equals("sneaking")
                || mode.equals("swimming")
                || mode.equals("flying")
                || mode.equals("elytra")
                || mode.equals("levitation");
    }

    private boolean validateMode(Player player, String mode) {
        return switch (mode) {
            case "boat" -> player.getVehicle() instanceof Boat;
            case "horse" -> player.getVehicle() instanceof AbstractHorse;
            case "pig" -> player.getVehicle() instanceof Pig;
            case "minecart" -> player.getVehicle() instanceof Minecart;
            case "strider" -> player.getVehicle() != null && player.getVehicle().getType() == EntityType.STRIDER;
            case "sneaking" -> player.isSneaking() && !player.isGliding() && !player.isFlying() && !player.isSwimming();
            case "walking" -> !player.isSneaking() && !player.isSprinting() && !player.isGliding() && !player.isFlying() && !player.isSwimming();
            case "running" -> player.isSprinting() && !player.isSneaking() && !player.isGliding() && !player.isFlying() && !player.isSwimming();
            case "swimming" -> player.isSwimming();
            case "flying" -> player.isFlying();
            case "elytra" -> player.isGliding();
            case "levitation" -> player.hasPotionEffect(PotionEffectType.LEVITATION);
            default -> false;
        };
    }
}