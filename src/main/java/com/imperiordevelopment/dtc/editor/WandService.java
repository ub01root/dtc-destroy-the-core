package com.imperiordevelopment.dtc.editor;

import com.imperiordevelopment.dtc.IDTCPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class WandService {

    private final NamespacedKey wandKey;
    private final Map<UUID, String> pendingCreateByPlayer;

    public WandService(IDTCPlugin plugin) {
        this.wandKey = new NamespacedKey(plugin, "idtc_wand");
        this.pendingCreateByPlayer = new HashMap<>();
    }

    public void giveWand(Player player) {
        player.getInventory().addItem(createWand());
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public void setPendingCreate(Player player, String name) {
        pendingCreateByPlayer.put(player.getUniqueId(), name);
    }

    public String getPendingCreate(Player player) {
        return pendingCreateByPlayer.get(player.getUniqueId());
    }

    public void clearPendingCreate(Player player) {
        pendingCreateByPlayer.remove(player.getUniqueId());
    }

    private ItemStack createWand() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&liDTC Wand"));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public void consumeOneWand(Player player, EquipmentSlot hand) {
        ItemStack current = hand == EquipmentSlot.OFF_HAND
            ? player.getInventory().getItemInOffHand()
            : player.getInventory().getItemInMainHand();
        if (!isWand(current)) {
            return;
        }
        int amount = current.getAmount();
        if (amount <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else {
            current.setAmount(amount - 1);
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(current);
            } else {
                player.getInventory().setItemInMainHand(current);
            }
        }
    }
}
