package pw.elytrix.elytrixbattlepass.utils;

import java.util.Arrays;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;

public class ItemUtils {
   public static int countItems(Player player, Material material) {
      int size = player.getInventory().getSize();
      int amount = 0;

      for (int i = 0; i < size; i++) {
         ItemStack item = player.getInventory().getItem(i);
         if (item != null && item.getType().equals(material) && !item.getItemMeta().hasLore()) {
            amount += item.getAmount();
         }
      }

      return amount;
   }

   public static int countItems(Player player, Material material, PotionData potionData) {
      int size = player.getInventory().getSize();
      int amount = 0;

      for (int i = 0; i < size; i++) {
         ItemStack item = player.getInventory().getItem(i);
         PotionMeta meta;
         ItemMeta itemMeta;
         if (item != null
            && item.getType().equals(material)
            && !item.getItemMeta().hasLore()
            && (itemMeta = item.getItemMeta()) instanceof PotionMeta
            && (meta = (PotionMeta)itemMeta).getBasePotionData().equals(potionData)) {
            amount += item.getAmount();
         }
      }

      return amount;
   }

   public static void takeItems(Player player, Material material, int amount) {
      int size = player.getInventory().getSize();

      for (int i = 0; i < size; i++) {
         ItemStack item;
         if (i != 40 && (item = player.getInventory().getItem(i)) != null && item.getType().equals(material) && !item.getItemMeta().hasLore()) {
            if (amount <= 0) {
               break;
            }

            if (item.getAmount() > amount) {
               item.setAmount(item.getAmount() - amount);
               amount = 0;
            } else {
               amount -= item.getAmount();
               item = null;
            }

            player.getInventory().setItem(i, item);
         }
      }
   }

   public static void takeItems(Player player, Material material, PotionData potionData, int amount) {
      int size = player.getInventory().getSize();

      for (int i = 0; i < size; i++) {
         ItemStack item = player.getInventory().getItem(i);
         PotionMeta meta;
         ItemMeta itemMeta;
         if (item != null
            && item.getType().equals(material)
            && !item.getItemMeta().hasLore()
            && (itemMeta = item.getItemMeta()) instanceof PotionMeta
            && (meta = (PotionMeta)itemMeta).getBasePotionData().equals(potionData)) {
            if (amount <= 0) {
               break;
            }

            if (item.getAmount() > amount) {
               item.setAmount(item.getAmount() - amount);
               amount = 0;
            } else {
               amount -= item.getAmount();
               item = null;
            }

            player.getInventory().setItem(i, item);
         }
      }
   }

   public static boolean isInvFull(Player player) {
      return Arrays.stream(player.getInventory().getStorageContents()).noneMatch(Objects::isNull);
   }

   public static boolean giveOrDrop(Player player, ItemStack itemStack) {
      if (isInvFull(player)) {
         player.getWorld().dropItemNaturally(player.getLocation().clone().add(0.5, 0.0, 0.5), itemStack);
         return false;
      } else {
         player.getInventory().addItem(new ItemStack[]{itemStack});
         return true;
      }
   }
}
