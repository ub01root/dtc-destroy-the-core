package com.imperiordevelopment.dtc.listener;

import com.imperiordevelopment.dtc.IDTCPlugin;
import com.imperiordevelopment.dtc.editor.WandService;
import com.imperiordevelopment.dtc.manager.EventManager;
import com.imperiordevelopment.dtc.util.MessageUtil;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class WandListener implements Listener {

    private final IDTCPlugin plugin;
    private final EventManager eventManager;
    private final WandService wandService;

    public WandListener(IDTCPlugin plugin, EventManager eventManager, WandService wandService) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.wandService = wandService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!wandService.isWand(item)) {
            return;
        }

        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block target = event.getClickedBlock() != null ? event.getClickedBlock() : event.getPlayer().getTargetBlockExact(6);
        if (target == null) {
            MessageUtil.send(event.getPlayer(), plugin.getConfig(), "messages.wand-invalid-target");
            return;
        }

        event.setCancelled(true);
        String pendingCreate = wandService.getPendingCreate(event.getPlayer());
        if (pendingCreate != null && !pendingCreate.isBlank()) {
            boolean created = eventManager.createCoreProfileAt(pendingCreate, target.getLocation());
            if (!created) {
                MessageUtil.send(event.getPlayer(), plugin.getConfig(), "messages.create-failed");
                return;
            }
            wandService.clearPendingCreate(event.getPlayer());
            wandService.consumeOneWand(event.getPlayer(), event.getHand() == null ? EquipmentSlot.HAND : event.getHand());
            String createdMsg = MessageUtil.get(plugin.getConfig(), "messages.create-success")
                .replace("%name%", eventManager.getCurrentCoreName());
            event.getPlayer().sendMessage(MessageUtil.colorize(createdMsg));
            String confirmMsg = MessageUtil.get(plugin.getConfig(), "messages.create-confirmed-by-wand");
            event.getPlayer().sendMessage(MessageUtil.colorize(confirmMsg));
        } else {
            eventManager.setCore(target.getLocation());
        }
        String msg = MessageUtil.get(plugin.getConfig(), "messages.wand-core-selected");
        msg = msg.replace("%x%", String.valueOf(target.getX()));
        msg = msg.replace("%y%", String.valueOf(target.getY()));
        msg = msg.replace("%z%", String.valueOf(target.getZ()));
        msg = msg.replace("%block%", target.getType().name());
        event.getPlayer().sendMessage(MessageUtil.colorize(msg));
    }
}
