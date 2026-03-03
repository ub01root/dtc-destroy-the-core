package com.imperiordevelopment.dtc.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.imperiordevelopment.dtc.IDTCPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class SqliteStatsStore implements StatsStore {

    private final String url;

    public SqliteStatsStore(IDTCPlugin plugin, FileConfiguration databaseConfig) {
        String fileName = databaseConfig.getString("database.sqlite.file", "dtc.db");
        File file = new File(plugin.getDataFolder(), fileName);
        this.url = "jdbc:sqlite:" + file.getAbsolutePath();
        init();
    }

    @Override
    public void addDamage(UUID uuid, String name, double amount) {
        String sql = "INSERT INTO dtc_stats(uuid, name, damage) VALUES(?,?,?) "
            + "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, damage=damage+excluded.damage";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    @Override
    public double getDamage(String name) {
        if (name == null) {
            return 0.0;
        }
        String sql = "SELECT damage FROM dtc_stats WHERE name=? ORDER BY damage DESC LIMIT 1";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("damage");
                }
            }
        } catch (Exception ignored) {
        }
        return 0.0;
    }

    @Override
    public List<Map.Entry<String, Double>> getTop(int size) {
        List<Map.Entry<String, Double>> list = new ArrayList<>();
        String sql = "SELECT name, damage FROM dtc_stats WHERE name IS NOT NULL "
            + "ORDER BY damage DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    double damage = rs.getDouble("damage");
                    list.add(Map.entry(name, damage));
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    @Override
    public void flushSave() {
        // SQLite writes are immediate.
    }

    private void init() {
        String sql = "CREATE TABLE IF NOT EXISTS dtc_stats ("
            + "uuid TEXT PRIMARY KEY,"
            + "name TEXT,"
            + "damage REAL"
            + ")";
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (Exception ignored) {
        }
    }
}


