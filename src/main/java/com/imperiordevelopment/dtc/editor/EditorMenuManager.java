package com.imperiordevelopment.dtc.editor;

import com.imperiordevelopment.dtc.IDTCPlugin;
import com.imperiordevelopment.dtc.manager.EventManager;
import com.imperiordevelopment.dtc.util.MessageUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class EditorMenuManager {

    private static final String PROFILE_TITLE = "Select DTC";
    private static final String MAIN_TITLE = "iDTC Editor";
    private static final String REWARD_TYPE_TITLE = "Reward Editor";
    private static final String REWARD_COMMANDS_TITLE = "Rewards - Commands";
    private static final String REWARD_LOOT_TITLE = "Rewards - Loot";

    private final IDTCPlugin plugin;
    private final EventManager eventManager;
    private final EditorSessionManager sessionManager;

    public EditorMenuManager(IDTCPlugin plugin, EventManager eventManager, EditorSessionManager sessionManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.sessionManager = sessionManager;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(new EditorInventoryHolder(EditorMenuType.MAIN), 45, MAIN_TITLE);
        inv.setItem(11, button(eventManager.isRunning() ? Material.RED_WOOL : Material.LIME_WOOL,
            eventManager.isRunning() ? "&cStop Event" : "&aStart Event"));
        inv.setItem(13, button(Material.NETHER_STAR, "&bMax Health: &f" + (int) eventManager.getConfiguredMaxHealth(),
            "&7Left click: +10", "&7Right click: -10"));
        inv.setItem(15, button(Material.IRON_PICKAXE, "&bBreak Amount: &f" + trimDecimal(eventManager.getConfiguredBreakAmount()),
            "&7Left click: +1", "&7Right click: -1"));
        inv.setItem(21, button(eventManager.isUseRegionCheck() ? Material.LIME_DYE : Material.GRAY_DYE,
            "&eRegion Check: " + (eventManager.isUseRegionCheck() ? "&aON" : "&cOFF"),
            "&7Click to toggle"));
        inv.setItem(23, button(Material.CHEST, "&6Reward Editor"));
        inv.setItem(40, button(Material.EMERALD, "&aSave & Reload"));
        player.openInventory(inv);
    }

    public void openProfileList(Player player) {
        Inventory inv = Bukkit.createInventory(new EditorInventoryHolder(EditorMenuType.PROFILE_LIST), 54, PROFILE_TITLE);
        List<String> profiles = eventManager.listCoreProfiles();
        String current = eventManager.getCurrentCoreName();
        List<String> displayed = new ArrayList<>();
        int slot = 0;
        for (String profile : profiles) {
            if (slot >= 45) {
                break;
            }
            Material material = profile.equalsIgnoreCase(current) ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
            inv.setItem(slot, button(material, "&e" + profile, "&7Click to edit this DTC profile."));
            displayed.add(profile);
            slot++;
        }
        inv.setItem(49, button(Material.ENDER_EYE, "&eCurrent: &f" + current));
        sessionManager.setProfileSlots(player.getUniqueId(), displayed);
        player.openInventory(inv);
    }

    public void openRewardType(Player player) {
        Inventory inv = Bukkit.createInventory(new EditorInventoryHolder(EditorMenuType.REWARD_TYPE), 27, REWARD_TYPE_TITLE);
        RewardMode mode = getRewardMode();
        inv.setItem(11, button(Material.PAPER, "&bCOMMAND", "&7Open command rewards editor."));
        inv.setItem(13, button(Material.COMPARATOR, "&eMode: &f" + mode.name(), "&7Click to change: NONE/COMMAND/LOOT"));
        inv.setItem(15, button(Material.CHEST, "&dLOOT", "&7Open loot rewards editor."));
        inv.setItem(22, button(Material.BARRIER, "&cBack"));
        player.openInventory(inv);
    }

    public void openRewardCommands(Player player) {
        ensurePendingRewardsLoaded(player);
        Inventory inv = Bukkit.createInventory(new EditorInventoryHolder(EditorMenuType.REWARD_COMMANDS), 54, REWARD_COMMANDS_TITLE);
        List<String> commands = getPendingCommands(player);
        for (int i = 0; i < commands.size() && i < 36; i++) {
            String command = commands.get(i);
            inv.setItem(i, button(Material.PAPER, "&f" + (i + 1) + ". &eCommand", "&7" + command, "&cClick to remove"));
        }
        inv.setItem(45, button(Material.LIME_CONCRETE, "&aAdd Command", "&7Write in chat, example:", "&7give {player} command_block 1"));
        inv.setItem(47, button(Material.EMERALD, "&aSave & Apply"));
        inv.setItem(49, button(Material.BARRIER, "&cBack"));
        inv.setItem(53, button(Material.LAVA_BUCKET, "&cClear All Commands"));
        player.openInventory(inv);
    }

    public void openRewardLoot(Player player) {
        ensurePendingRewardsLoaded(player);
        Inventory inv = Bukkit.createInventory(new EditorInventoryHolder(EditorMenuType.REWARD_LOOT), 54, REWARD_LOOT_TITLE);
        List<ItemStack> items = getPendingItems(player);
        for (int i = 0; i < items.size() && i < 45; i++) {
            inv.setItem(i, items.get(i));
        }
        inv.setItem(45, button(Material.BARRIER, "&cBack"));
        inv.setItem(47, button(Material.EMERALD, "&aSave & Apply"));
        inv.setItem(49, button(Material.BOOK, "&ePut loot items in slots 1-45", "&7Then click Save & Apply"));
        inv.setItem(53, button(Material.LAVA_BUCKET, "&cClear All Loot"));
        player.openInventory(inv);
    }

    public void handleMainClick(Player player, int rawSlot, InventoryClickEvent event) {
        switch (rawSlot) {
            case 11:
                if (eventManager.isRunning()) {
                    eventManager.stop(true);
                } else {
                    eventManager.start(player);
                }
                openMain(player);
                break;
            case 13:
                if (event.getClick().isLeftClick()) {
                    eventManager.setConfiguredMaxHealth(eventManager.getConfiguredMaxHealth() + 10.0);
                } else if (event.getClick().isRightClick()) {
                    eventManager.setConfiguredMaxHealth(eventManager.getConfiguredMaxHealth() - 10.0);
                }
                MessageUtil.send(player, plugin.getConfig(), "messages.editor-max-health-updated");
                openMain(player);
                break;
            case 15:
                if (event.getClick().isLeftClick()) {
                    eventManager.setConfiguredBreakAmount(eventManager.getConfiguredBreakAmount() + 1.0);
                } else if (event.getClick().isRightClick()) {
                    eventManager.setConfiguredBreakAmount(eventManager.getConfiguredBreakAmount() - 1.0);
                }
                MessageUtil.send(player, plugin.getConfig(), "messages.editor-break-amount-updated");
                openMain(player);
                break;
            case 21:
                eventManager.setUseRegionCheck(!eventManager.isUseRegionCheck());
                MessageUtil.send(player, plugin.getConfig(), "messages.editor-region-check-updated");
                openMain(player);
                break;
            case 23:
                openRewardType(player);
                break;
            case 40:
                plugin.saveConfig();
                plugin.reloadConfig();
                MessageUtil.reload(plugin);
                eventManager.reloadFromConfig();
                MessageUtil.send(player, plugin.getConfig(), "messages.editor-saved");
                openMain(player);
                break;
            default:
                break;
        }
    }

    public void handleProfileListClick(Player player, int rawSlot) {
        List<String> profiles = sessionManager.getProfileSlots(player.getUniqueId());
        if (profiles == null || profiles.isEmpty()) {
            return;
        }
        if (rawSlot < 0 || rawSlot >= profiles.size()) {
            return;
        }
        String selected = profiles.get(rawSlot);
        if (!eventManager.setCurrentCoreProfile(selected)) {
            MessageUtil.send(player, plugin.getConfig(), "messages.editor-profile-select-failed");
            return;
        }
        String msg = MessageUtil.get(plugin.getConfig(), "messages.editor-profile-selected").replace("%name%", selected);
        player.sendMessage(MessageUtil.colorize(msg));
        openMain(player);
    }

    public void handleRewardTypeClick(Player player, int rawSlot) {
        RewardMode mode = getRewardMode();
        switch (rawSlot) {
            case 11:
                if (mode != RewardMode.COMMAND) {
                    MessageUtil.send(player, plugin.getConfig(), "messages.editor-reward-mode-deny");
                    return;
                }
                openRewardCommands(player);
                break;
            case 13:
                RewardMode next = mode.next();
                setRewardMode(next);
                MessageUtil.send(player, plugin.getConfig(), "messages.editor-mode-changed");
                openRewardType(player);
                break;
            case 15:
                if (mode != RewardMode.LOOT) {
                    MessageUtil.send(player, plugin.getConfig(), "messages.editor-reward-mode-deny");
                    return;
                }
                openRewardLoot(player);
                break;
            case 22:
                openMain(player);
                break;
            default:
                break;
        }
    }

    public void handleRewardCommandsClick(Player player, int rawSlot) {
        ensurePendingRewardsLoaded(player);
        List<String> commands = new ArrayList<>(getPendingCommands(player));
        if (rawSlot >= 0 && rawSlot < 36 && rawSlot < commands.size()) {
            commands.remove(rawSlot);
            sessionManager.setPendingRewardCommands(player.getUniqueId(), commands);
            MessageUtil.send(player, plugin.getConfig(), "messages.editor-command-removed");
            openRewardCommands(player);
            return;
        }
        switch (rawSlot) {
            case 45:
                sessionManager.setWaitingRewardCommand(player.getUniqueId(), true);
                player.closeInventory();
                MessageUtil.send(player, plugin.getConfig(), "messages.editor-command-prompt");
                break;
            case 47:
                applyRewardChanges(player);
                MessageUtil.send(player, plugin.getConfig(), "messages.editor-rewards-saved");
                openRewardCommands(player);
                break;
            case 49:
                openRewardType(player);
                break;
            case 53:
                sessionManager.setPendingRewardCommands(player.getUniqueId(), new ArrayList<>());
                MessageUtil.send(player, plugin.getConfig(), "messages.editor-commands-cleared");
                openRewardCommands(player);
                break;
            default:
                break;
        }
    }

    public void handleRewardLootClick(Player player, int rawSlot) {
        if (rawSlot == 45) {
            player.closeInventory();
            openRewardType(player);
            return;
        }
        if (rawSlot == 47) {
            applyRewardChanges(player);
            MessageUtil.send(player, plugin.getConfig(), "messages.editor-rewards-saved");
            openRewardLoot(player);
            return;
        }
        if (rawSlot == 53) {
            sessionManager.setPendingRewardItems(player.getUniqueId(), new ArrayList<>());
            MessageUtil.send(player, plugin.getConfig(), "messages.editor-items-cleared");
            openRewardLoot(player);
        }
    }

    public boolean isMainMenu(Inventory inv) {
        return isMenu(inv, EditorMenuType.MAIN);
    }

    public boolean isProfileMenu(Inventory inv) {
        return isMenu(inv, EditorMenuType.PROFILE_LIST);
    }

    public boolean isRewardTypeMenu(Inventory inv) {
        return isMenu(inv, EditorMenuType.REWARD_TYPE);
    }

    public boolean isRewardCommandsMenu(Inventory inv) {
        return isMenu(inv, EditorMenuType.REWARD_COMMANDS);
    }

    public boolean isRewardLootMenu(Inventory inv) {
        return isMenu(inv, EditorMenuType.REWARD_LOOT);
    }

    public void handleChatCommand(Player player, String message) {
        ensurePendingRewardsLoaded(player);
        if ("cancel".equalsIgnoreCase(message)) {
            sessionManager.setWaitingRewardCommand(player.getUniqueId(), false);
            MessageUtil.send(player, plugin.getConfig(), "messages.editor-command-cancelled");
            openRewardCommands(player);
            return;
        }
        List<String> commands = new ArrayList<>(getPendingCommands(player));
        commands.add(message);
        sessionManager.setPendingRewardCommands(player.getUniqueId(), commands);
        sessionManager.setWaitingRewardCommand(player.getUniqueId(), false);
        MessageUtil.send(player, plugin.getConfig(), "messages.editor-command-added");
        openRewardCommands(player);
    }

    public boolean isAwaitingRewardCommand(Player player) {
        return sessionManager.isWaitingRewardCommand(player.getUniqueId());
    }

    public void captureLootMenu(Player player, Inventory inv) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        sessionManager.setPendingRewardItems(player.getUniqueId(), items);
    }

    private void setRewardMode(RewardMode mode) {
        eventManager.setRewardMode(mode);
    }

    private RewardMode getRewardMode() {
        return eventManager.getRewardMode();
    }

    private void ensurePendingRewardsLoaded(Player player) {
        if (sessionManager.getPendingRewardCommands(player.getUniqueId()) == null) {
            sessionManager.setPendingRewardCommands(player.getUniqueId(), eventManager.getRewardCommands());
        }
        if (sessionManager.getPendingRewardItems(player.getUniqueId()) == null) {
            sessionManager.setPendingRewardItems(player.getUniqueId(), eventManager.getRewardItems());
        }
    }

    private List<String> getPendingCommands(Player player) {
        List<String> commands = sessionManager.getPendingRewardCommands(player.getUniqueId());
        return commands != null ? commands : new ArrayList<>();
    }

    private List<ItemStack> getPendingItems(Player player) {
        List<ItemStack> items = sessionManager.getPendingRewardItems(player.getUniqueId());
        return items != null ? items : new ArrayList<>();
    }

    private void applyRewardChanges(Player player) {
        ensurePendingRewardsLoaded(player);
        eventManager.setRewardCommands(new ArrayList<>(getPendingCommands(player)));
        eventManager.setRewardItems(new ArrayList<>(getPendingItems(player)));
    }

    private boolean isMenu(Inventory inv, EditorMenuType type) {
        return inv != null
            && inv.getHolder() instanceof EditorInventoryHolder
            && ((EditorInventoryHolder) inv.getHolder()).getMenuType() == type;
    }

    private ItemStack button(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.colorize(name));
        if (lore != null && lore.length > 0) {
            meta.setLore(Arrays.stream(lore).map(MessageUtil::colorize).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private String trimDecimal(double value) {
        if (Math.floor(value) == value) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }
}
