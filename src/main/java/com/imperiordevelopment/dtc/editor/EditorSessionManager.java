package com.imperiordevelopment.dtc.editor;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public final class EditorSessionManager {

    private final Set<UUID> waitingRewardCommand;
    private final Map<UUID, List<String>> profileSlotsByPlayer;
    private final Map<UUID, List<String>> pendingRewardCommands;
    private final Map<UUID, List<ItemStack>> pendingRewardItems;

    public EditorSessionManager() {
        this.waitingRewardCommand = new HashSet<>();
        this.profileSlotsByPlayer = new HashMap<>();
        this.pendingRewardCommands = new HashMap<>();
        this.pendingRewardItems = new HashMap<>();
    }

    public void setWaitingRewardCommand(UUID uuid, boolean waiting) {
        if (waiting) {
            waitingRewardCommand.add(uuid);
        } else {
            waitingRewardCommand.remove(uuid);
        }
    }

    public boolean isWaitingRewardCommand(UUID uuid) {
        return waitingRewardCommand.contains(uuid);
    }

    public void setProfileSlots(UUID uuid, List<String> profiles) {
        profileSlotsByPlayer.put(uuid, profiles);
    }

    public List<String> getProfileSlots(UUID uuid) {
        return profileSlotsByPlayer.get(uuid);
    }

    public void setPendingRewardCommands(UUID uuid, List<String> commands) {
        pendingRewardCommands.put(uuid, new ArrayList<>(commands));
    }

    public List<String> getPendingRewardCommands(UUID uuid) {
        return pendingRewardCommands.get(uuid);
    }

    public void setPendingRewardItems(UUID uuid, List<ItemStack> items) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack item : items) {
            copy.add(item.clone());
        }
        pendingRewardItems.put(uuid, copy);
    }

    public List<ItemStack> getPendingRewardItems(UUID uuid) {
        return pendingRewardItems.get(uuid);
    }

    public void clearPendingRewards(UUID uuid) {
        pendingRewardCommands.remove(uuid);
        pendingRewardItems.remove(uuid);
    }
}
