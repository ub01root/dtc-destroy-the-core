package com.imperiordevelopment.dtc;

import com.imperiordevelopment.dtc.command.DtcCommand;
import com.imperiordevelopment.dtc.editor.EditorMenuManager;
import com.imperiordevelopment.dtc.editor.EditorSessionManager;
import com.imperiordevelopment.dtc.editor.WandService;
import com.imperiordevelopment.dtc.listener.CoreListener;
import com.imperiordevelopment.dtc.listener.EditorChatListener;
import com.imperiordevelopment.dtc.listener.EditorListener;
import com.imperiordevelopment.dtc.listener.WandListener;
import com.imperiordevelopment.dtc.manager.EventManager;
import com.imperiordevelopment.dtc.manager.StatsManager;
import com.imperiordevelopment.dtc.placeholder.DtcPlaceholderExpansion;
import com.imperiordevelopment.dtc.util.MessageUtil;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class IDTCPlugin extends JavaPlugin {

    private EventManager eventManager;
    private StatsManager statsManager;
    private DtcPlaceholderExpansion placeholderExpansion;
    private FileConfiguration databaseConfig;
    private WandService wandService;
    private EditorMenuManager editorMenuManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("database.yml");
        saveResourceIfMissing("messages/message_es.yml");
        saveResourceIfMissing("messages/message_en.yml");
        loadDatabaseConfig();
        MessageUtil.reload(this);

        statsManager = new StatsManager(this, databaseConfig);
        eventManager = new EventManager(this, statsManager);
        wandService = new WandService(this);
        editorMenuManager = new EditorMenuManager(this, eventManager, new EditorSessionManager());

        DtcCommand command = new DtcCommand(this, eventManager, wandService, editorMenuManager);
        if (getCommand("idtc") != null) {
            getCommand("idtc").setExecutor(command);
            getCommand("idtc").setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(new CoreListener(eventManager), this);
        getServer().getPluginManager().registerEvents(new WandListener(this, eventManager, wandService), this);
        getServer().getPluginManager().registerEvents(new EditorListener(editorMenuManager), this);
        getServer().getPluginManager().registerEvents(new EditorChatListener(this, editorMenuManager), this);

        if (getConfig().getBoolean("placeholders.enabled", true) && isPlaceholderApiAvailable()) {
            placeholderExpansion = new DtcPlaceholderExpansion(this, eventManager);
            placeholderExpansion.register();
        }

        getLogger().info("iDTC enabled");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (statsManager != null) {
            statsManager.flushSave();
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        getLogger().info("iDTC disabled");
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public FileConfiguration getDatabaseConfig() {
        return databaseConfig;
    }

    private void loadDatabaseConfig() {
        File file = new File(getDataFolder(), "database.yml");
        databaseConfig = YamlConfiguration.loadConfiguration(file);
    }

    private void saveResourceIfMissing(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
    }

    private boolean isPlaceholderApiAvailable() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}


