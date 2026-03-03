package com.imperiordevelopment.dtc.editor;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class EditorInventoryHolder implements InventoryHolder {

    private final EditorMenuType menuType;

    public EditorInventoryHolder(EditorMenuType menuType) {
        this.menuType = menuType;
    }

    public EditorMenuType getMenuType() {
        return menuType;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

