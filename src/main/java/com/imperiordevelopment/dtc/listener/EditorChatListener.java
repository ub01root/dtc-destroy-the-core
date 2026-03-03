package com.imperiordevelopment.dtc.listener;

import com.imperiordevelopment.dtc.IDTCPlugin;
import com.imperiordevelopment.dtc.editor.EditorMenuManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class EditorChatListener implements Listener {

    private final IDTCPlugin plugin;
    private final EditorMenuManager editorMenuManager;

    public EditorChatListener(IDTCPlugin plugin, EditorMenuManager editorMenuManager) {
        this.plugin = plugin;
        this.editorMenuManager = editorMenuManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!editorMenuManager.isAwaitingRewardCommand(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        plugin.getServer().getScheduler().runTask(plugin, () -> editorMenuManager.handleChatCommand(event.getPlayer(), message));
    }
}

