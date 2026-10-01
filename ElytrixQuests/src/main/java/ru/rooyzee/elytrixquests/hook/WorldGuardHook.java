package ru.rooyzee.elytrixquests.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    private boolean enabled = false;

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            enabled = true;
            return true;
        }
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasRegion(Player player) {
        if (!enabled) return false;
        try {
            for (World world : Bukkit.getWorlds()) {
                RegionManager manager = WorldGuard.getInstance()
                        .getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                if (manager == null) continue;
                for (ProtectedRegion region : manager.getRegions().values()) {
                    if (region.getOwners().contains(player.getUniqueId())) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public boolean isInRegion(Player player, String regionName) {
        if (!enabled || player == null || regionName == null || regionName.isEmpty()) return false;
        try {
            Location loc = player.getLocation();
            if (loc.getWorld() == null) return false;

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
            if (manager == null) return false;

            BlockVector3 vec = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            ApplicableRegionSet set = manager.getApplicableRegions(vec);

            for (ProtectedRegion region : set) {
                if (region.getId().equalsIgnoreCase(regionName)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}