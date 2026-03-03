package com.imperiordevelopment.dtc.listener;

import com.imperiordevelopment.dtc.manager.EventManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CoreListener implements Listener {

    private final EventManager eventManager;

    public CoreListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (!eventManager.isCoreBlock(location)) {
            return;
        }
        if (!eventManager.isRunning()) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        event.getBlock().setType(eventManager.getCoreBlockType());
        eventManager.handleDamage(event.getPlayer(), location);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Location location = event.getClickedBlock().getLocation();
        if (!eventManager.isCoreBlock(location)) {
            return;
        }
        if (!eventManager.isRunning()) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        event.getClickedBlock().setType(eventManager.getCoreBlockType());
        eventManager.handleDamage(event.getPlayer(), location);
    }
}


