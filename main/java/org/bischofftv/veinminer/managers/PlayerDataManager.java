package org.bischofftv.veinminer.managers;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final Veinminer plugin;
    private final Map<UUID, PlayerData> playerDataMap;

    public PlayerDataManager(Veinminer plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public boolean isVeinMinerEnabled(Player player) {
        PlayerData data = getPlayerData(player.getUniqueId());
        return data != null && data.isVeinMinerEnabled();
    }

    public void setVeinMinerEnabled(Player player, boolean enabled) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setVeinMinerEnabled(enabled);
        }
    }

    public boolean isToolEnabled(Player player, String toolType) {
        PlayerData data = getPlayerData(player.getUniqueId());
        return data != null && data.isToolEnabled(toolType);
    }

    public void setToolEnabled(Player player, String toolType, boolean enabled) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setToolEnabled(toolType, enabled);
        }
    }

    public void savePlayerSettings(Player player) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        if (playerData != null) {
            plugin.getDatabaseManager().savePlayerData(player.getUniqueId(), playerData);
        }
    }

    /**
     * Save all player data
     */
    public void saveAllData() {
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            plugin.getDatabaseManager().savePlayerData(entry.getKey(), entry.getValue());
        }
    }

    public int getPlayerLevel(Player player) {
        PlayerData data = getPlayerData(player.getUniqueId());
        return data != null ? data.getLevel() : 1;
    }

    public void setPlayerLevel(Player player, int level) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setLevel(level);
        }
    }

    public int getPlayerExperience(Player player) {
        PlayerData data = getPlayerData(player.getUniqueId());
        return data != null ? data.getExperience() : 0;
    }

    public void setPlayerExperience(Player player, int experience) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setExperience(experience);
        }
    }

    public void addPlayerExperience(Player player, int amount) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            int currentExp = data.getExperience();
            data.setExperience(currentExp + amount);
        }
    }

    public void loadPlayerData(Player player) {
        if (player == null) {
            plugin.getLogger().warning("Attempted to load player data for null player");
            return;
        }

        try {
            // Load player data from database
            PlayerData data = plugin.getDatabaseManager().loadPlayerData(player.getUniqueId());
            
            if (data == null) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] No existing data found for player " + player.getName() + ", creating new data");
                }
                // Create new player data if none exists
                data = new PlayerData(player.getUniqueId(), player.getName());
                
                // Initialize default tool settings
                data.setToolEnabled("PICKAXE", true);
                data.setToolEnabled("AXE", true);
                data.setToolEnabled("SHOVEL", true);
                data.setToolEnabled("HOE", true);
                
                // Save the new player data to database
                plugin.getDatabaseManager().savePlayerData(player.getUniqueId(), data);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Created and saved new data for player " + player.getName());
                }
            } else if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Successfully loaded existing data for player " + player.getName());
            }
            
            playerDataMap.put(player.getUniqueId(), data);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player data for " + player.getName() + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            
            // Create temporary data to prevent null pointer exceptions
            PlayerData tempData = new PlayerData(player.getUniqueId(), player.getName());
            playerDataMap.put(player.getUniqueId(), tempData);
            
            // Notify the player
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.error.player-data-load-failed"));
        }
    }

    public void savePlayerData(Player player) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        if (playerData != null) {
            plugin.getDatabaseManager().savePlayerData(player.getUniqueId(), playerData);
        }
    }

    public void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public void forceReloadPlayerData(Player player) {
        playerDataMap.remove(player.getUniqueId());
        loadPlayerData(player);
    }
} 