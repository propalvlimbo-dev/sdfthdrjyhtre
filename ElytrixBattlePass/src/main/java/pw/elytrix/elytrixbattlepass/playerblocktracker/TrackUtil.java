package pw.elytrix.elytrixbattlepass.playerblocktracker;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public class TrackUtil {
   public static long getChunkKey(@NotNull Chunk chunk) {
      return getChunkKey(chunk.getX(), chunk.getZ());
   }

   public static long getChunkKey(int chunkX, int chunkZ) {
      return chunkX & 4294967295L | (chunkZ & 4294967295L) << 32;
   }

   public static long getChunkKeyOfBlock(@NotNull Block block) {
      return getChunkKey(block.getX() >> 4, block.getZ() >> 4);
   }

   public static int getRelativeChunkPosition(@NotNull Block block) {
      int relX = (block.getX() % 16 + 16) % 16;
      int relZ = (block.getZ() % 16 + 16) % 16;
      int relY = block.getY();
      return relY & 65535 | (relX & 0xFF) << 16 | (relZ & 0xFF) << 24;
   }
}
