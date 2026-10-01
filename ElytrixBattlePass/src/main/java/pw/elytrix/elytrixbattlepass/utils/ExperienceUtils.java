package pw.elytrix.elytrixbattlepass.utils;

import org.bukkit.entity.Player;

public class ExperienceUtils {
   public static void setTotalExperience(Player player, int exp) {
      if (exp < 0) {
         throw new IllegalArgumentException("Experience is negative!");
      }

      player.setExp(0.0F);
      player.setLevel(0);
      player.setTotalExperience(0);
      int amount = exp;

      while (amount > 0) {
         int expToLevel = getExperienceAtLevel(player.getLevel());
         if ((amount -= expToLevel) >= 0) {
            player.giveExp(expToLevel);
         } else {
            int var4;
            player.giveExp(var4 = amount + expToLevel);
            amount = 0;
         }
      }
   }

   public static int getTotalExperience(Player player) {
      int experience = Math.round(getExperienceAtLevel(player.getLevel()) * player.getExp());
      int currentLevel = player.getLevel();

      while (currentLevel > 0) {
         experience += getExperienceAtLevel(--currentLevel);
      }

      if (experience < 0) {
         experience = 0;
      }

      return experience;
   }

   private static int getExperienceAtLevel(int level) {
      if (level <= 15) {
         return (level << 1) + 7;
      } else {
         return level <= 30 ? level * 5 - 38 : level * 9 - 158;
      }
   }
}
