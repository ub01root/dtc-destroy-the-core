package com.imperiordevelopment.dtc.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

public final class WorldGuardHook {

    public boolean isInRegion(Location location, String regionId) {
        if (location == null || location.getWorld() == null || regionId == null || regionId.isEmpty()) {
            return false;
        }
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
            .get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) {
            return false;
        }
        BlockVector3 vector = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        ApplicableRegionSet regions = regionManager.getApplicableRegions(vector);
        for (ProtectedRegion region : regions) {
            if (region.getId().equalsIgnoreCase(regionId)) {
                return true;
            }
        }
        return false;
    }
}


