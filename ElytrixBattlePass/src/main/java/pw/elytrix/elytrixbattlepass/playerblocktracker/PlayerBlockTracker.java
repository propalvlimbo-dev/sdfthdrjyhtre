package pw.elytrix.elytrixbattlepass.playerblocktracker;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerBlockTracker {
    public static final Set<Predicate<Block>> BLOCK_FILTERS = new HashSet<>();
    public static final NamespacedKey TRACKED_DATA_KEY = NamespacedKey.minecraft("tracked_chunk_data");
    private static final Map<UUID, TrackedWorld> TRACKED_WORLD_MAP = new Object2ObjectOpenHashMap<>();
    private static TrackListener listener;

    public static void initialize(Plugin plugin) {
        if (listener != null) {
            return;
        }
        initCurrentlyLoadedWorlds();
        listener = new TrackListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public static void shutdown() {
        if (listener == null) {
            return;
        }
        terminateCurrentlyLoadedWorlds();
        HandlerList.unregisterAll(listener);
        listener = null;
        BLOCK_FILTERS.clear();
    }

    public static void initWorld(@NotNull World world) {
        TrackedWorld trackedWorld = new TrackedWorld();
        for (Chunk loadedChunk : world.getLoadedChunks()) {
            trackedWorld.initChunk(loadedChunk);
        }
        TRACKED_WORLD_MAP.put(world.getUID(), trackedWorld);
    }

    public static void terminateWorld(@NotNull World world) {
        TrackedWorld trackedWorld = TRACKED_WORLD_MAP.remove(world.getUID());
        if (trackedWorld != null) {
            for (Chunk loadedChunk : world.getLoadedChunks()) {
                trackedWorld.terminateChunk(loadedChunk);
            }
        }
    }

    public static void initChunk(@NotNull Chunk chunk) {
        TrackedWorld trackedWorld = getTrackedWorldOf(chunk);
        if (trackedWorld != null) {
            trackedWorld.initChunk(chunk);
        }
    }

    public static void terminateChunk(@NotNull Chunk chunk) {
        TrackedWorld trackedWorld = getTrackedWorldOf(chunk);
        if (trackedWorld != null) {
            trackedWorld.terminateChunk(chunk);
        }
    }

    public static void initCurrentlyLoadedWorlds() {
        Bukkit.getWorlds().forEach(PlayerBlockTracker::initWorld);
    }

    public static void terminateCurrentlyLoadedWorlds() {
        Bukkit.getWorlds().forEach(PlayerBlockTracker::terminateWorld);
    }

    public static boolean isTracked(@NotNull Block block) {
        TrackedWorld trackedWorld = getTrackedWorldOf(block);
        return trackedWorld != null && trackedWorld.isTracked(block);
    }

    public static void track(@NotNull Block block) {
        trackForce(block);
    }

    public static void trackForce(@NotNull Block block) {
        TrackedWorld trackedWorld = getTrackedWorldOf(block);
        if (trackedWorld != null) {
            trackedWorld.add(block);
        }
    }

    public static void unTrack(@NotNull Block block) {
        TrackedWorld trackedWorld = getTrackedWorldOf(block);
        if (trackedWorld != null) {
            trackedWorld.remove(block);
        }
    }

    public static void track(@NotNull Collection<Block> blocks) {
        blocks.forEach(PlayerBlockTracker::trackForce);
    }

    public static void unTrack(@NotNull Collection<Block> blocks) {
        blocks.forEach(PlayerBlockTracker::unTrack);
    }

    public static void shift(@NotNull BlockFace direction, @NotNull List<Block> blocks) {
        unTrack(blocks);
        track(blocks.stream().map(block -> block.getRelative(direction)).toList());
    }

    @Nullable
    private static TrackedWorld getTrackedWorldOf(@NotNull Block block) {
        return TRACKED_WORLD_MAP.get(block.getWorld().getUID());
    }

    @Nullable
    private static TrackedWorld getTrackedWorldOf(@NotNull Chunk chunk) {
        return TRACKED_WORLD_MAP.get(chunk.getWorld().getUID());
    }
}