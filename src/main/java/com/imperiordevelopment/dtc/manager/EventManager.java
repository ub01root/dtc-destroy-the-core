package com.imperiordevelopment.dtc.manager;

import com.imperiordevelopment.dtc.IDTCPlugin;
import com.imperiordevelopment.dtc.editor.RewardMode;
import com.imperiordevelopment.dtc.hook.WorldGuardHook;
import com.imperiordevelopment.dtc.storage.DtcProfileStore;
import com.imperiordevelopment.dtc.util.MessageUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.ZonedDateTime;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class EventManager {

    private final IDTCPlugin plugin;
    private final StatsManager statsManager;
    private final DtcProfileStore profileStore;
    private boolean running;
    private double health;
    private double maxHealth;
    private double breakAmount;
    private BukkitTask intervalTask;
    private BukkitTask autoStopTask;
    private BukkitTask autoStartTask;
    private final Map<UUID, Double> damageByPlayer;
    private final Map<String, Double> damageByName;
    private final Set<Integer> announcedThresholds;
    private final Map<UUID, Long> lastDamageAt;
    private WorldGuardHook worldGuardHook;
    private String regionId;
    private boolean useRegionCheck;
    private boolean autoStartEnabled;
    private long autoStartIntervalMillis;
    private long nextAutoStartAtMillis;
    private boolean autoStartAlignToClockHour;
    private int autoStartMinute;

    public EventManager(IDTCPlugin plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.profileStore = new DtcProfileStore(plugin);
        this.damageByPlayer = new HashMap<>();
        this.damageByName = new HashMap<>();
        this.announcedThresholds = new HashSet<>();
        this.lastDamageAt = new HashMap<>();
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook();
        }
        profileStore.migrateFromLegacyConfig(plugin.getConfig());
        ensureCurrentProfileSelected();
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        YamlConfiguration profile = currentProfile();
        maxHealth = profile.getDouble("max-health", 200.0);
        breakAmount = profile.getDouble("break-amount", 1.0);
        regionId = profile.getString("region-id", "");
        useRegionCheck = profile.getBoolean("use-region-check", false);

        FileConfiguration config = plugin.getConfig();
        autoStartEnabled = config.getBoolean("event.auto-start.enabled", true);
        long hours = Math.max(1L, config.getLong("event.auto-start.interval-hours", 5L));
        autoStartIntervalMillis = hours * 60L * 60L * 1000L;
        autoStartAlignToClockHour = config.getBoolean("event.auto-start.align-to-clock-hour", true);
        autoStartMinute = Math.max(0, Math.min(59, config.getInt("event.auto-start.minute", 0)));
        scheduleAutoStart();
    }

    public boolean isRunning() {
        return running;
    }

    public void start(CommandSender sender) {
        if (running && !plugin.getConfig().getBoolean("event.allow-start-while-running", false)) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.event-already-running");
            return;
        }
        if (!hasCoreConfigured()) {
            sender.sendMessage("Core is not configured. Use /idtc create <name> and wand-click.");
            return;
        }
        running = true;
        health = maxHealth;
        damageByPlayer.clear();
        damageByName.clear();
        announcedThresholds.clear();
        Location core = getCoreLocation();
        if (core != null) {
            core.getBlock().setType(getCoreBlockType());
        }
        MessageUtil.broadcast(plugin.getConfig(), "messages.event-started");
        scheduleBroadcasts();
        scheduleAutoStop();
    }

    public void stop(boolean broadcast) {
        if (!running) {
            if (broadcast) {
                MessageUtil.broadcast(plugin.getConfig(), "messages.event-not-running");
            }
            return;
        }
        running = false;
        if (intervalTask != null) {
            intervalTask.cancel();
            intervalTask = null;
        }
        if (autoStopTask != null) {
            autoStopTask.cancel();
            autoStopTask = null;
        }
        if (broadcast) {
            MessageUtil.broadcast(plugin.getConfig(), "messages.event-stopped");
        }
        setCoreToBedrock();
        statsManager.flushSave();
    }

    public void shutdown() {
        stop(false);
        if (autoStartTask != null) {
            autoStartTask.cancel();
            autoStartTask = null;
        }
    }

    public void setCore(Location location) {
        YamlConfiguration profile = currentProfile();
        profile.set("world", location.getWorld().getName());
        profile.set("x", location.getBlockX());
        profile.set("y", location.getBlockY());
        profile.set("z", location.getBlockZ());
        profile.set("active-block", location.getBlock().getType().name());
        profile.set("block-to-break", location.getBlock().getType().name());
        saveCurrentProfile(profile);
    }

    public boolean isCoreBlock(Location location) {
        Location core = getCoreLocation();
        if (core == null) {
            return false;
        }
        return core.getWorld().equals(location.getWorld())
            && core.getBlockX() == location.getBlockX()
            && core.getBlockY() == location.getBlockY()
            && core.getBlockZ() == location.getBlockZ();
    }

    public Location getCoreLocation() {
        YamlConfiguration profile = currentProfile();
        String worldName = profile.getString("world", "");
        if (worldName == null || worldName.isEmpty() || Bukkit.getWorld(worldName) == null) {
            return null;
        }
        int x = profile.getInt("x", Integer.MIN_VALUE);
        int y = profile.getInt("y", Integer.MIN_VALUE);
        int z = profile.getInt("z", Integer.MIN_VALUE);
        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
            return null;
        }
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public boolean hasCoreConfigured() {
        return getCoreLocation() != null;
    }

    public Material getCoreBlockType() {
        YamlConfiguration profile = currentProfile();
        String materialName = profile.getString("active-block",
            profile.getString("block-to-break", "RAW_GOLD_BLOCK"));
        Material material = Material.matchMaterial(materialName);
        return material != null ? material : Material.RAW_GOLD_BLOCK;
    }

    public Material getInactiveCoreBlockType() {
        String materialName = currentProfile().getString("inactive-block", "BEDROCK");
        Material material = Material.matchMaterial(materialName);
        return material != null ? material : Material.BEDROCK;
    }

    public boolean createCoreProfileAt(String name, Location location) {
        String key = normalizeProfileName(name);
        if (key.isBlank()) {
            return false;
        }
        YamlConfiguration template = currentProfile();
        List<ItemStack> templateItems = getRewardItems();
        boolean created = profileStore.createProfileFromLocation(key, location, plugin.getConfig(), template, templateItems);
        if (!created) {
            return false;
        }
        plugin.getConfig().set("core.current-name", key);
        plugin.saveConfig();
        reloadFromConfig();
        return true;
    }

    public boolean profileExists(String name) {
        String key = normalizeProfileName(name);
        return !key.isBlank() && profileStore.profileExists(key);
    }

    public boolean deleteCoreProfile(String name) {
        String key = normalizeProfileName(name);
        if (key.isBlank()) {
            return false;
        }
        if (!profileStore.deleteProfile(key)) {
            return false;
        }
        if (getCurrentCoreName().equalsIgnoreCase(key)) {
            List<String> left = listCoreProfiles();
            String next = left.isEmpty() ? "default" : left.get(0);
            if (!profileStore.profileExists(next)) {
                profileStore.ensureProfileExists(next, plugin.getConfig());
            }
            plugin.getConfig().set("core.current-name", next);
            plugin.saveConfig();
        }
        reloadFromConfig();
        return true;
    }

    public List<String> listCoreProfiles() {
        return profileStore.listProfiles();
    }

    public String getCurrentCoreName() {
        return normalizeProfileName(plugin.getConfig().getString("core.current-name", "default"));
    }

    public boolean setCurrentCoreProfile(String name) {
        String key = normalizeProfileName(name);
        if (key.isBlank() || !profileStore.profileExists(key)) {
            return false;
        }
        plugin.getConfig().set("core.current-name", key);
        plugin.saveConfig();
        reloadFromConfig();
        return true;
    }

    public void handleDamage(Player player, Location location) {
        if (!running) {
            return;
        }
        if (!isAllowedLocation(location)) {
            return;
        }
        health = Math.max(0.0, health - breakAmount);
        damageByPlayer.merge(player.getUniqueId(), breakAmount, Double::sum);
        damageByName.merge(player.getName(), breakAmount, Double::sum);
        statsManager.addDamage(player.getUniqueId(), player.getName(), breakAmount);
        sendDamageMessage(player);
        checkThresholdBroadcasts();
        if (health <= 0.0) {
            destroyCore(player);
        }
    }

    public void recordDamageTick(Player player) {
        lastDamageAt.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean hasRecentDamage(Player player) {
        Long last = lastDamageAt.get(player.getUniqueId());
        if (last == null) {
            return false;
        }
        return System.currentTimeMillis() - last < 150;
    }

    private void destroyCore(Player player) {
        Location core = getCoreLocation();
        if (core != null) {
            core.getBlock().setType(getInactiveCoreBlockType());
        }
        String message = MessageUtil.get(plugin.getConfig(), "messages.core-destroyed");
        message = message.replace("%player%", player.getName());
        MessageUtil.broadcastRaw(message);
        runRewards(player.getName());
        announceTop();
        runTopRewards();
        stop(false);
    }

    public void runRewards(String playerName) {
        if (!plugin.getConfig().getBoolean("rewards.enabled", true)) {
            return;
        }
        RewardMode mode = getRewardMode();
        if (mode == RewardMode.COMMAND) {
            for (String cmd : getRewardCommands()) {
                String finalCmd = cmd.replace("%player%", playerName).replace("{player}", playerName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }
        }
        if (mode == RewardMode.LOOT) {
            Player winner = Bukkit.getPlayerExact(playerName);
            if (winner != null && winner.isOnline()) {
                for (ItemStack item : getRewardItems()) {
                    winner.getInventory().addItem(item.clone());
                }
            }
        }
    }

    public double getTotalDamage(String playerName) {
        return statsManager.getDamage(playerName);
    }

    public double getEventDamage(String playerName) {
        return damageByName.getOrDefault(playerName, 0.0);
    }

    private void sendDamageMessage(Player player) {
        String msg = MessageUtil.get(plugin.getConfig(), "messages.damage-update");
        if (msg == null || msg.isEmpty()) {
            return;
        }
        double eventDamage = getEventDamage(player.getName());
        msg = msg.replace("%damage%", String.valueOf((int) eventDamage));
        msg = msg.replace("%health%", String.valueOf((int) Math.ceil(health)));
        msg = msg.replace("%max%", String.valueOf((int) Math.ceil(maxHealth)));
        player.sendMessage(MessageUtil.colorize(msg));
    }

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getConfiguredMaxHealth() {
        return currentProfile().getDouble("max-health", 200.0);
    }

    public double getConfiguredBreakAmount() {
        return currentProfile().getDouble("break-amount", 1.0);
    }

    public boolean isUseRegionCheck() {
        return currentProfile().getBoolean("use-region-check", false);
    }

    public void setConfiguredMaxHealth(double value) {
        double safe = Math.max(1.0, value);
        YamlConfiguration profile = currentProfile();
        profile.set("max-health", safe);
        saveCurrentProfile(profile);
        maxHealth = safe;
        if (!running) {
            health = safe;
        }
    }

    public void setConfiguredBreakAmount(double value) {
        double safe = Math.max(0.1, value);
        YamlConfiguration profile = currentProfile();
        profile.set("break-amount", safe);
        saveCurrentProfile(profile);
        breakAmount = safe;
    }

    public void setUseRegionCheck(boolean value) {
        YamlConfiguration profile = currentProfile();
        profile.set("use-region-check", value);
        saveCurrentProfile(profile);
        useRegionCheck = value;
    }

    public int getHealthPercent() {
        if (maxHealth <= 0.0) {
            return 0;
        }
        return (int) Math.round((health / maxHealth) * 100.0);
    }

    public String getNextAutoStartFormatted() {
        if (running) {
            return plugin.getConfig().getString("placeholders.next-start-while-running", "EN CURSO");
        }
        if (!autoStartEnabled || autoStartIntervalMillis <= 0L) {
            return "desactivado";
        }
        long remaining = Math.max(0L, nextAutoStartAtMillis - System.currentTimeMillis());
        return formatDuration(remaining);
    }

    public void sendTop(CommandSender sender) {
        int size = plugin.getConfig().getInt("event.top-size", 3);
        List<Map.Entry<String, Double>> top = statsManager.getTop(size);
        if (top.isEmpty()) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.top-empty");
            return;
        }
        sender.sendMessage(MessageUtil.colorize(MessageUtil.get(plugin.getConfig(), "messages.top-title")));
        int pos = 1;
        for (Map.Entry<String, Double> entry : top) {
            String line = MessageUtil.get(plugin.getConfig(), "messages.top-line");
            line = line.replace("%position%", String.valueOf(pos));
            line = line.replace("%player%", entry.getKey());
            line = line.replace("%damage%", String.valueOf(entry.getValue().intValue()));
            sender.sendMessage(MessageUtil.colorize(line));
            pos++;
        }
    }

    public RewardMode getRewardMode() {
        return RewardMode.fromString(currentProfile().getString("rewards.mode", "NONE"));
    }

    public void setRewardMode(RewardMode mode) {
        YamlConfiguration profile = currentProfile();
        profile.set("rewards.mode", mode.name());
        saveCurrentProfile(profile);
    }

    public List<String> getRewardCommands() {
        return new ArrayList<>(currentProfile().getStringList("rewards.commands"));
    }

    public void setRewardCommands(List<String> commands) {
        YamlConfiguration profile = currentProfile();
        profile.set("rewards.commands", new ArrayList<>(commands));
        saveCurrentProfile(profile);
    }

    public List<ItemStack> getRewardItems() {
        return profileStore.loadRewardItems(getCurrentCoreName());
    }

    public void setRewardItems(List<ItemStack> items) {
        profileStore.saveRewardItems(getCurrentCoreName(), items);
    }

    private void scheduleBroadcasts() {
        if (!plugin.getConfig().getBoolean("broadcasts.enabled", true)) {
            return;
        }
        int intervalSeconds = plugin.getConfig().getInt("broadcasts.interval-seconds", 30);
        if (intervalSeconds <= 0) {
            return;
        }
        intervalTask = new BukkitRunnable() {
            @Override
            public void run() {
                String msg = MessageUtil.get(plugin.getConfig(), "messages.health-remaining");
                msg = msg.replace("%health%", String.valueOf((int) Math.ceil(health)));
                msg = msg.replace("%max%", String.valueOf((int) Math.ceil(maxHealth)));
                MessageUtil.broadcastRaw(msg);
            }
        }.runTaskTimer(plugin, 20L * intervalSeconds, 20L * intervalSeconds);
    }

    private void scheduleAutoStop() {
        int minutes = plugin.getConfig().getInt("event.auto-stop-minutes", 0);
        if (minutes <= 0) {
            return;
        }
        autoStopTask = new BukkitRunnable() {
            @Override
            public void run() {
                stop(true);
            }
        }.runTaskLater(plugin, 20L * 60L * minutes);
    }

    private void scheduleAutoStart() {
        if (autoStartTask != null) {
            autoStartTask.cancel();
            autoStartTask = null;
        }
        if (!autoStartEnabled || autoStartIntervalMillis <= 0L) {
            return;
        }
        nextAutoStartAtMillis = calculateNextAutoStartMillis();
        autoStartTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now < nextAutoStartAtMillis) {
                    return;
                }
                if (!running) {
                    start(Bukkit.getConsoleSender());
                }
                nextAutoStartAtMillis = calculateNextAutoStartMillis();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkThresholdBroadcasts() {
        if (!plugin.getConfig().getBoolean("broadcasts.health-remaining-enabled", true)) {
            return;
        }
        List<Integer> thresholds = plugin.getConfig().getIntegerList("broadcasts.health-remaining-thresholds");
        if (thresholds == null) {
            thresholds = Collections.emptyList();
        }
        double percent = (health / maxHealth) * 100.0;
        for (Integer threshold : thresholds) {
            if (threshold == null) {
                continue;
            }
            if (percent <= threshold && !announcedThresholds.contains(threshold)) {
                announcedThresholds.add(threshold);
                String msg = MessageUtil.get(plugin.getConfig(), "messages.health-remaining");
                msg = msg.replace("%health%", String.valueOf((int) Math.ceil(health)));
                msg = msg.replace("%max%", String.valueOf((int) Math.ceil(maxHealth)));
                MessageUtil.broadcastRaw(msg);
            }
        }
    }

    private boolean isAllowedLocation(Location location) {
        if (!useRegionCheck) {
            return true;
        }
        if (regionId == null || regionId.isEmpty()) {
            return true;
        }
        if (worldGuardHook == null) {
            return true;
        }
        if (currentProfile().getBoolean("allow-break-outside-region", false)) {
            return true;
        }
        return worldGuardHook.isInRegion(location, regionId);
    }

    private void setCoreToBedrock() {
        Location core = getCoreLocation();
        if (core != null) {
            core.getBlock().setType(getInactiveCoreBlockType());
        }
    }

    private void announceTop() {
        if (!plugin.getConfig().getBoolean("event.announce-top", true)) {
            return;
        }
        int size = plugin.getConfig().getInt("event.top-size", 3);
        List<Map.Entry<String, Double>> top = damageByName.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(size)
            .toList();
        if (top.isEmpty()) {
            MessageUtil.broadcast(plugin.getConfig(), "messages.top-empty");
            return;
        }
        List<String> custom = MessageUtil.getList(plugin.getConfig(), "messages.top-broadcast");
        if (custom != null && !custom.isEmpty()) {
            String n1 = top.size() >= 1 ? top.get(0).getKey() : "-";
            String n2 = top.size() >= 2 ? top.get(1).getKey() : "-";
            String n3 = top.size() >= 3 ? top.get(2).getKey() : "-";
            String d1 = top.size() >= 1 ? String.valueOf(top.get(0).getValue().intValue()) : "0";
            String d2 = top.size() >= 2 ? String.valueOf(top.get(1).getValue().intValue()) : "0";
            String d3 = top.size() >= 3 ? String.valueOf(top.get(2).getValue().intValue()) : "0";
            for (String line : custom) {
                String out = line
                    .replace("%top_1_name%", n1)
                    .replace("%top_2_name%", n2)
                    .replace("%top_3_name%", n3)
                    .replace("%top_1_damage%", d1)
                    .replace("%top_2_damage%", d2)
                    .replace("%top_3_damage%", d3);
                Bukkit.broadcastMessage(MessageUtil.colorize(out));
            }
            return;
        }
        MessageUtil.broadcast(plugin.getConfig(), "messages.top-title");
        int pos = 1;
        for (Map.Entry<String, Double> entry : top) {
            String line = MessageUtil.get(plugin.getConfig(), "messages.top-line");
            line = line.replace("%position%", String.valueOf(pos));
            line = line.replace("%player%", entry.getKey());
            line = line.replace("%damage%", String.valueOf(entry.getValue().intValue()));
            MessageUtil.broadcastRaw(line);
            pos++;
        }
    }

    private void runTopRewards() {
        int size = plugin.getConfig().getInt("event.top-size", 3);
        List<Map.Entry<String, Double>> top = damageByName.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(size)
            .toList();
        for (int i = 0; i < top.size(); i++) {
            String player = top.get(i).getKey();
            List<String> commands = plugin.getConfig().getStringList("rewards.top-" + (i + 1));
            for (String cmd : commands) {
                String finalCmd = cmd.replace("%winner_" + (i + 1) + "%", player);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }
        }
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    private long calculateNextAutoStartMillis() {
        if (!autoStartAlignToClockHour) {
            return System.currentTimeMillis() + autoStartIntervalMillis;
        }
        long intervalHours = Math.max(1L, autoStartIntervalMillis / (60L * 60L * 1000L));
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime candidate = now.withSecond(0).withNano(0).withMinute(autoStartMinute);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusHours(1).withMinute(autoStartMinute);
        }
        while ((candidate.getHour() % intervalHours) != 0) {
            candidate = candidate.plusHours(1).withMinute(autoStartMinute);
        }
        return candidate.toInstant().toEpochMilli();
    }

    private String normalizeProfileName(String value) {
        return profileStore.normalizeName(value);
    }

    private void ensureCurrentProfileSelected() {
        String current = normalizeProfileName(plugin.getConfig().getString("core.current-name", "default"));
        if (current.isBlank()) {
            current = "default";
        }
        if (!profileStore.profileExists(current)) {
            profileStore.ensureProfileExists(current, plugin.getConfig());
        }
        plugin.getConfig().set("core.current-name", current);
        plugin.saveConfig();
    }

    private YamlConfiguration currentProfile() {
        ensureCurrentProfileSelected();
        return profileStore.loadProfileConfig(getCurrentCoreName(), plugin.getConfig());
    }

    private void saveCurrentProfile(YamlConfiguration configuration) {
        profileStore.saveProfileConfig(getCurrentCoreName(), configuration);
    }
}
