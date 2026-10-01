package pw.elytrix.elytrixbattlepass.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.user.User;
import pw.elytrix.elytrixbattlepass.user.UserManager;

public class PlayerListener implements Listener {
   private final ElytrixBattlePass plugin;
   private final UserManager userManager;

   public PlayerListener(ElytrixBattlePass plugin) {
      this.plugin = plugin;
      this.userManager = plugin.getUserManager();
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      Bukkit.getScheduler().runTask(this.plugin, () -> {
         User user = this.userManager.getUser(player);
         if (user == null) {
            this.userManager.createNewUser(player);
         }
      });
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      User user = this.userManager.getUserFromCache(player);
      if (user != null) {
         this.userManager.updateUser(user);
         this.userManager.deleteUserFromCache(player);
      }
   }

   @EventHandler
   public void onWorldSave(WorldSaveEvent event) {
      Bukkit.getOnlinePlayers().forEach(player -> {
         User user = this.userManager.getUserFromCache(player);
         if (user != null) {
            this.userManager.updateUser(user);
         }
      });
   }
}
