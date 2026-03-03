package com.imperiordevelopment.dtc.manager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.imperiordevelopment.dtc.IDTCPlugin;
import com.imperiordevelopment.dtc.storage.FlatfileStatsStore;
import com.imperiordevelopment.dtc.storage.SqliteStatsStore;
import com.imperiordevelopment.dtc.storage.StatsStore;
import org.bukkit.configuration.file.FileConfiguration;

public final class StatsManager {

    private final StatsStore store;

    public StatsManager(IDTCPlugin plugin, FileConfiguration databaseConfig) {
        String type = databaseConfig.getString("database.type", "FLATFILE");
        if ("SQLITE".equalsIgnoreCase(type)) {
            store = new SqliteStatsStore(plugin, databaseConfig);
        } else {
            store = new FlatfileStatsStore(plugin, databaseConfig);
        }
    }

    public void addDamage(UUID uuid, String name, double amount) {
        store.addDamage(uuid, name, amount);
    }

    public double getDamage(String name) {
        return store.getDamage(name);
    }

    public List<Map.Entry<String, Double>> getTop(int size) {
        return store.getTop(size);
    }

    public void flushSave() {
        store.flushSave();
    }
}


