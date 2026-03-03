package com.imperiordevelopment.dtc.storage;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.imperiordevelopment.dtc.IDTCPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class FlatfileStatsStore implements StatsStore {

    private final IDTCPlugin plugin;
    private final File file;
    private final FileConfiguration data;
    private final Map<UUID, Double> byUuid;
    private final Map<String, Double> byName;
    private final Map<UUID, String> nameByUuid;
    private boolean dirty;

    public FlatfileStatsStore(IDTCPlugin plugin, FileConfiguration databaseConfig) {
        this.plugin = plugin;
        String folderName = databaseConfig.getString("flatfile.folder", "data");
        String fileName = databaseConfig.getString("flatfile.file", "stats.yml");
        File folder = new File(plugin.getDataFolder(), folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.file = new File(folder, fileName);
        this.data = YamlConfiguration.loadConfiguration(file);
        this.byUuid = new HashMap<>();
        this.byName = new HashMap<>();
        this.nameByUuid = new HashMap<>();
        load();
    }

    @Override
    public void addDamage(UUID uuid, String name, double amount) {
        byUuid.merge(uuid, amount, Double::sum);
        if (name != null) {
            byName.merge(name, amount, Double::sum);
            nameByUuid.put(uuid, name);
        }
        dirty = true;
    }

    @Override
    public double getDamage(String name) {
        if (name == null) {
            return 0.0;
        }
        return byName.getOrDefault(name, 0.0);
    }

    @Override
    public List<Map.Entry<String, Double>> getTop(int size) {
        if (byName.isEmpty()) {
            return Collections.emptyList();
        }
        return byName.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(size)
            .collect(Collectors.toList());
    }

    @Override
    public void flushSave() {
        if (!dirty) {
            return;
        }
        dirty = false;
        if (!plugin.isEnabled()) {
            save();
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    private void load() {
        if (data.isConfigurationSection("players")) {
            for (String key : data.getConfigurationSection("players").getKeys(false)) {
                double dmg = data.getDouble("players." + key + ".damage", 0.0);
                String name = data.getString("players." + key + ".name", "");
                try {
                    UUID uuid = UUID.fromString(key);
                    byUuid.put(uuid, dmg);
                    if (name != null && !name.isEmpty()) {
                        byName.put(name, dmg);
                        nameByUuid.put(uuid, name);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private void save() {
        data.set("players", null);
        for (Map.Entry<UUID, Double> entry : byUuid.entrySet()) {
            String key = entry.getKey().toString();
            data.set("players." + key + ".damage", entry.getValue());
            data.set("players." + key + ".name", nameByUuid.getOrDefault(entry.getKey(), "Unknown"));
        }
        try {
            data.save(file);
        } catch (IOException ignored) {
        }
    }
}


