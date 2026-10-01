package pw.elytrix.elytrixbattlepass.playerblocktracker;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class TrackedChunk {
   private final IntSet trackedBlockPositions;

   protected TrackedChunk(@NotNull PersistentDataContainer container) {
      int[] data = (int[])container.get(PlayerBlockTracker.TRACKED_DATA_KEY, PersistentDataType.INTEGER_ARRAY);
      this.trackedBlockPositions = data == null ? new IntOpenHashSet() : new IntOpenHashSet(data);
   }

   protected void add(@NotNull Block block) {
      this.trackedBlockPositions.add(TrackUtil.getRelativeChunkPosition(block));
   }

   protected void remove(@NotNull Block block) {
      this.trackedBlockPositions.remove(TrackUtil.getRelativeChunkPosition(block));
   }

   protected boolean isTracked(@NotNull Block block) {
      return this.trackedBlockPositions.contains(TrackUtil.getRelativeChunkPosition(block));
   }

   protected void saveTo(@NotNull PersistentDataContainer container) {
      int[] data = this.trackedBlockPositions.toIntArray();
      container.set(PlayerBlockTracker.TRACKED_DATA_KEY, PersistentDataType.INTEGER_ARRAY, data);
   }

   protected boolean isEmpty() {
      return this.trackedBlockPositions.isEmpty();
   }
}
