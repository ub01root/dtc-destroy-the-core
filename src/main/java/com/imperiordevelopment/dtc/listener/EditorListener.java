package com.imperiordevelopment.dtc.listener;

import com.imperiordevelopment.dtc.editor.EditorMenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class EditorListener implements Listener {

    private final EditorMenuManager editorMenuManager;

    public EditorListener(EditorMenuManager editorMenuManager) {
        this.editorMenuManager = editorMenuManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (editorMenuManager.isMainMenu(event.getInventory())) {
            event.setCancelled(true);
            editorMenuManager.handleMainClick(player, event.getRawSlot(), event);
            return;
        }
        if (editorMenuManager.isProfileMenu(event.getInventory())) {
            event.setCancelled(true);
            editorMenuManager.handleProfileListClick(player, event.getRawSlot());
            return;
        }
        if (editorMenuManager.isRewardTypeMenu(event.getInventory())) {
            event.setCancelled(true);
            editorMenuManager.handleRewardTypeClick(player, event.getRawSlot());
            return;
        }
        if (editorMenuManager.isRewardCommandsMenu(event.getInventory())) {
            event.setCancelled(true);
            editorMenuManager.handleRewardCommandsClick(player, event.getRawSlot());
            return;
        }
        if (editorMenuManager.isRewardLootMenu(event.getInventory())) {
            int slot = event.getRawSlot();
            if (slot >= 45 && slot < 54) {
                event.setCancelled(true);
                editorMenuManager.handleRewardLootClick(player, slot);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player && editorMenuManager.isRewardLootMenu(event.getInventory())) {
            editorMenuManager.captureLootMenu(player, event.getInventory());
        }
    }
}
