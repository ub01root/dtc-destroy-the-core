package com.imperiordevelopment.dtc.util;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageUtil {

    private static FileConfiguration langConfig;

    private MessageUtil() {
    }

    public static void reload(JavaPlugin plugin) {
        String locale = plugin.getConfig().getString("settings.locale", "es_ES").toLowerCase();
        String fileName = locale.startsWith("en") ? "message_en.yml" : "message_es.yml";
        File file = new File(plugin.getDataFolder(), "messages" + File.separator + fileName);
        if (!file.exists()) {
            plugin.saveResource("messages/" + fileName, false);
        }
        langConfig = YamlConfiguration.loadConfiguration(file);
    }

    public static void send(CommandSender sender, FileConfiguration config, String path) {
        String msg = get(config, path);
        if (msg == null || msg.isEmpty()) {
            return;
        }
        sender.sendMessage(color(msg));
    }

    public static void broadcast(FileConfiguration config, String path) {
        String msg = get(config, path);
        if (msg == null || msg.isEmpty()) {
            return;
        }
        broadcastRaw(msg);
    }

    public static void broadcastRaw(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        Bukkit.broadcastMessage(color(message));
    }

    public static String get(FileConfiguration config, String path) {
        String key = normalizePath(path);
        String prefix = getLangString("prefix", config.getString("messages.prefix", ""));
        String msg = getLangString(key, config.getString(path, ""));
        if (msg == null) {
            msg = "";
        }
        return prefix + msg;
    }

    public static List<String> getList(FileConfiguration config, String path) {
        String key = normalizePath(path);
        List<String> list = langConfig != null ? langConfig.getStringList(key) : Collections.emptyList();
        if (list == null || list.isEmpty()) {
            return config.getStringList(path);
        }
        return list;
    }

    public static String colorize(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private static String color(String msg) {
        return colorize(msg);
    }

    private static String getLangString(String path, String fallback) {
        if (langConfig == null) {
            return fallback;
        }
        return langConfig.getString(path, fallback);
    }

    private static String normalizePath(String path) {
        if (path.startsWith("messages.")) {
            return path.substring("messages.".length());
        }
        return path;
    }
}


