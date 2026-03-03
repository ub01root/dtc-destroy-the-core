package com.imperiordevelopment.dtc.placeholder;

import com.imperiordevelopment.dtc.IDTCPlugin;
import com.imperiordevelopment.dtc.manager.EventManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class DtcPlaceholderExpansion extends PlaceholderExpansion {

    private final IDTCPlugin plugin;
    private final EventManager eventManager;

    public DtcPlaceholderExpansion(IDTCPlugin plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @Override
    public String getIdentifier() {
        return "idtc";
    }

    @Override
    public String getAuthor() {
        return "IkariDevelopment";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return "";
        }
        switch (params.toLowerCase()) {
            case "running":
                return eventManager.isRunning() ? "true" : "false";
            case "health":
                return String.valueOf((int) Math.ceil(eventManager.getHealth()));
            case "max_health":
                return String.valueOf((int) Math.ceil(eventManager.getMaxHealth()));
            case "health_percent":
                return String.valueOf(eventManager.getHealthPercent());
            case "damage_total":
                if (player == null || player.getName() == null) {
                    return "0";
                }
                return String.valueOf(eventManager.getTotalDamage(player.getName()));
            case "next_start":
                return eventManager.getNextAutoStartFormatted();
            default:
                return "";
        }
    }
}


