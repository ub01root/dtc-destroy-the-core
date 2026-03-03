package com.imperiordevelopment.dtc.storage;

import com.imperiordevelopment.dtc.editor.RewardMode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class DtcProfileStore {

    private static final String DTCS_DIR = "dtcs";
    private static final String DTC_PREFIX = "dtc_";
    private static final String ITEMS_DIR = "items";
    private static final String ITEMS_FILE = "reward_items.yml";

    private final JavaPlugin plugin;
    private final File dtcsFolder;

    public DtcProfileStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dtcsFolder = new File(plugin.getDataFolder(), DTCS_DIR);
        if (!dtcsFolder.exists()) {
            dtcsFolder.mkdirs();
        }
    }

    public void migrateFromLegacyConfig(FileConfiguration mainConfig) {
        if (!listProfiles().isEmpty()) {
            return;
        }

        RewardMode legacyMode = RewardMode.fromString(mainConfig.getString("rewards.mode", "NONE"));
        List<String> legacyCommands = mainConfig.getStringList("rewards.commands");
        List<ItemStack> legacyItems = readLegacyItems(mainConfig);

        ConfigurationSection coresSection = mainConfig.getConfigurationSection("cores");
        if (coresSection != null && !coresSection.getKeys(false).isEmpty()) {
            for (String rawName : coresSection.getKeys(false)) {
                String name = normalizeName(rawName);
                if (name.isBlank()) {
                    continue;
                }
                ConfigurationSection section = coresSection.getConfigurationSection(rawName);
                if (section == null) {
                    continue;
                }
                YamlConfiguration profile = createDefaultProfileConfig(mainConfig);
                profile.set("world", section.getString("world", mainConfig.getString("core.world", "world")));
                profile.set("x", section.getInt("x", Integer.MIN_VALUE));
                profile.set("y", section.getInt("y", Integer.MIN_VALUE));
                profile.set("z", section.getInt("z", Integer.MIN_VALUE));
                profile.set("region-id", section.getString("region-id", mainConfig.getString("core.region-id", "")));
                profile.set("use-region-check", section.getBoolean("use-region-check",
                    mainConfig.getBoolean("core.use-region-check", false)));
                profile.set("active-block", section.getString("active-block", mainConfig.getString("core.active-block", "RAW_GOLD_BLOCK")));
                profile.set("inactive-block", section.getString("inactive-block", mainConfig.getString("core.inactive-block", "BEDROCK")));
                profile.set("block-to-break", section.getString("block-to-break", mainConfig.getString("core.block-to-break", "RAW_GOLD_BLOCK")));
                profile.set("max-health", section.getDouble("max-health", mainConfig.getDouble("core.max-health", 200.0)));
                profile.set("break-amount", section.getDouble("break-amount", mainConfig.getDouble("core.break-amount", 1.0)));
                profile.set("allow-break-outside-region", section.getBoolean("allow-break-outside-region",
                    mainConfig.getBoolean("core.allow-break-outside-region", false)));
                profile.set("rewards.mode", legacyMode.name());
                profile.set("rewards.commands", new ArrayList<>(legacyCommands));
                saveProfileConfig(name, profile);
                saveRewardItems(name, legacyItems);
            }
            return;
        }

        String fallback = normalizeName(mainConfig.getString("core.current-name", "default"));
        if (fallback.isBlank()) {
            fallback = "default";
        }
        ensureProfileExists(fallback, mainConfig);
        YamlConfiguration profile = loadProfileConfig(fallback, mainConfig);
        profile.set("rewards.mode", legacyMode.name());
        profile.set("rewards.commands", new ArrayList<>(legacyCommands));
        saveProfileConfig(fallback, profile);
        saveRewardItems(fallback, legacyItems);
    }

    public void ensureProfileExists(String profileName, FileConfiguration mainConfig) {
        String name = normalizeName(profileName);
        if (name.isBlank() || profileExists(name)) {
            return;
        }
        saveProfileConfig(name, createDefaultProfileConfig(mainConfig));
        saveRewardItems(name, Collections.emptyList());
    }

    public boolean profileExists(String profileName) {
        String name = normalizeName(profileName);
        if (name.isBlank()) {
            return false;
        }
        return getProfileFile(name).exists();
    }

    public YamlConfiguration loadProfileConfig(String profileName, FileConfiguration mainConfig) {
        String name = normalizeName(profileName);
        ensureProfileExists(name, mainConfig);
        return YamlConfiguration.loadConfiguration(getProfileFile(name));
    }

    public void saveProfileConfig(String profileName, FileConfiguration configuration) {
        String name = normalizeName(profileName);
        if (name.isBlank()) {
            return;
        }
        File folder = getProfileFolder(name);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = getProfileFile(name);
        try {
            configuration.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save DTC profile '" + name + "': " + ex.getMessage());
        }
    }

    public boolean createProfileFromLocation(String profileName, Location location, FileConfiguration mainConfig,
                                             YamlConfiguration templateProfile, List<ItemStack> templateItems) {
        String name = normalizeName(profileName);
        if (name.isBlank() || profileExists(name)) {
            return false;
        }
        YamlConfiguration profile = createDefaultProfileConfig(mainConfig);
        if (templateProfile != null) {
            profile.set("region-id", templateProfile.getString("region-id", profile.getString("region-id", "")));
            profile.set("use-region-check", templateProfile.getBoolean("use-region-check", profile.getBoolean("use-region-check", false)));
            profile.set("inactive-block", templateProfile.getString("inactive-block", profile.getString("inactive-block", "BEDROCK")));
            profile.set("max-health", templateProfile.getDouble("max-health", profile.getDouble("max-health", 200.0)));
            profile.set("break-amount", templateProfile.getDouble("break-amount", profile.getDouble("break-amount", 1.0)));
            profile.set("allow-break-outside-region",
                templateProfile.getBoolean("allow-break-outside-region", profile.getBoolean("allow-break-outside-region", false)));
            profile.set("rewards.mode", RewardMode.fromString(templateProfile.getString("rewards.mode", "NONE")).name());
            profile.set("rewards.commands", new ArrayList<>(templateProfile.getStringList("rewards.commands")));
        }

        profile.set("world", location.getWorld().getName());
        profile.set("x", location.getBlockX());
        profile.set("y", location.getBlockY());
        profile.set("z", location.getBlockZ());
        profile.set("active-block", location.getBlock().getType().name());
        profile.set("block-to-break", location.getBlock().getType().name());
        saveProfileConfig(name, profile);
        saveRewardItems(name, templateItems != null ? templateItems : Collections.emptyList());
        return true;
    }

    public List<String> listProfiles() {
        if (!dtcsFolder.exists()) {
            return Collections.emptyList();
        }
        File[] folders = dtcsFolder.listFiles(File::isDirectory);
        if (folders == null || folders.length == 0) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (File folder : folders) {
            String folderName = folder.getName();
            if (!folderName.startsWith(DTC_PREFIX)) {
                continue;
            }
            String profile = normalizeName(folderName.substring(DTC_PREFIX.length()));
            if (!profile.isBlank()) {
                names.add(profile);
            }
        }
        Collections.sort(names);
        return names;
    }

    public boolean deleteProfile(String profileName) {
        String name = normalizeName(profileName);
        if (name.isBlank()) {
            return false;
        }
        File folder = getProfileFolder(name);
        if (!folder.exists()) {
            return false;
        }
        return deleteRecursively(folder);
    }

    @SuppressWarnings("unchecked")
    public List<ItemStack> loadRewardItems(String profileName) {
        String name = normalizeName(profileName);
        if (name.isBlank()) {
            return Collections.emptyList();
        }
        File itemsFile = getItemsFile(name);
        if (!itemsFile.exists()) {
            return Collections.emptyList();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        List<?> raw = config.getList("items", Collections.emptyList());
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> items = new ArrayList<>();
        for (Object obj : raw) {
            if (obj instanceof ItemStack stack) {
                items.add(stack);
            }
        }
        return items;
    }

    public void saveRewardItems(String profileName, List<ItemStack> items) {
        String name = normalizeName(profileName);
        if (name.isBlank()) {
            return;
        }
        File itemsFolder = getItemsFolder(name);
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
        }
        YamlConfiguration config = new YamlConfiguration();
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack item : items) {
            copy.add(item.clone());
        }
        config.set("items", copy);
        try {
            config.save(getItemsFile(name));
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save reward items for DTC '" + name + "': " + ex.getMessage());
        }
    }

    public String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9_-]", "");
    }

    private YamlConfiguration createDefaultProfileConfig(FileConfiguration mainConfig) {
        YamlConfiguration profile = new YamlConfiguration();
        profile.set("world", mainConfig.getString("core.world", "world"));
        profile.set("x", Integer.MIN_VALUE);
        profile.set("y", Integer.MIN_VALUE);
        profile.set("z", Integer.MIN_VALUE);
        profile.set("region-id", mainConfig.getString("core.region-id", ""));
        profile.set("use-region-check", mainConfig.getBoolean("core.use-region-check", false));
        profile.set("active-block", mainConfig.getString("core.active-block", Material.RAW_GOLD_BLOCK.name()));
        profile.set("inactive-block", mainConfig.getString("core.inactive-block", Material.BEDROCK.name()));
        profile.set("block-to-break", mainConfig.getString("core.block-to-break", Material.RAW_GOLD_BLOCK.name()));
        profile.set("max-health", mainConfig.getDouble("core.max-health", 200.0));
        profile.set("break-amount", mainConfig.getDouble("core.break-amount", 1.0));
        profile.set("allow-break-outside-region", mainConfig.getBoolean("core.allow-break-outside-region", false));
        profile.set("rewards.mode", RewardMode.fromString(mainConfig.getString("rewards.mode", "NONE")).name());
        profile.set("rewards.commands", new ArrayList<>(mainConfig.getStringList("rewards.commands")));
        return profile;
    }

    @SuppressWarnings("unchecked")
    private List<ItemStack> readLegacyItems(FileConfiguration mainConfig) {
        List<?> raw = mainConfig.getList("rewards.items", Collections.emptyList());
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> items = new ArrayList<>();
        for (Object obj : raw) {
            if (obj instanceof ItemStack stack) {
                items.add(stack);
            }
        }
        return items;
    }

    private File getProfileFolder(String profileName) {
        return new File(dtcsFolder, DTC_PREFIX + normalizeName(profileName));
    }

    private File getProfileFile(String profileName) {
        String name = normalizeName(profileName);
        return new File(getProfileFolder(name), DTC_PREFIX + name + ".yml");
    }

    private File getItemsFolder(String profileName) {
        return new File(getProfileFolder(profileName), ITEMS_DIR);
    }

    private File getItemsFile(String profileName) {
        return new File(getItemsFolder(profileName), ITEMS_FILE);
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
}
