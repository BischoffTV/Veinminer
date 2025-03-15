package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.database.DatabaseManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {
    
    private final Veinminer plugin;
    private final Map<UUID, DatabaseManager.PlayerSettings> playerSettingsCache;
    
    public PlayerDataManager(Veinminer plugin) {
        this.plugin = plugin;
        this.playerSettingsCache = new HashMap<>();
    }
    
    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        CompletableFuture<DatabaseManager.PlayerData> dataFuture = plugin.getDatabaseManager().loadPlayerData(player);
        CompletableFuture<DatabaseManager.PlayerSettings> settingsFuture = plugin.getDatabaseManager().loadPlayerSettings(uuid);
        
        dataFuture.thenAcceptAsync(playerData -> {
            plugin.getLevelManager().cachePlayerData(uuid, playerData);
        });
        
        settingsFuture.thenAcceptAsync(settings -> {
            playerSettingsCache.put(uuid, settings);
        });
    }
    
    public void unloadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        savePlayerData(player);
        
        plugin.getLevelManager().removePlayerData(uuid);
        playerSettingsCache.remove(uuid);
    }
    
    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (plugin.getLevelManager().getPlayerData(uuid) != null) {
            plugin.getDatabaseManager().savePlayerData(plugin.getLevelManager().getPlayerData(uuid));
        }
        
        if (playerSettingsCache.containsKey(uuid)) {
            plugin.getDatabaseManager().savePlayerSettings(playerSettingsCache.get(uuid));
        }
    }
    
    public void saveAllData() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            savePlayerData(player);
        }
    }
    
    public DatabaseManager.PlayerSettings getPlayerSettings(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (playerSettingsCache.containsKey(uuid)) {
            return playerSettingsCache.get(uuid);
        }
        
        try {
            DatabaseManager.PlayerSettings settings = plugin.getDatabaseManager().loadPlayerSettings(uuid).get();
            playerSettingsCache.put(uuid, settings);
            return settings;
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading player settings for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            DatabaseManager.PlayerSettings defaultSettings = new DatabaseManager.PlayerSettings(uuid, true, true, true, true, true);
            playerSettingsCache.put(uuid, defaultSettings);
            return defaultSettings;
        }
    }
    
    public void updatePlayerSettings(Player player, DatabaseManager.PlayerSettings settings) {
        UUID uuid = player.getUniqueId();
        playerSettingsCache.put(uuid, settings);
        plugin.getDatabaseManager().savePlayerSettings(settings);
    }
    
    public boolean isVeinMinerEnabled(Player player) {
        return getPlayerSettings(player).isEnabled();
    }
    
    public void setVeinMinerEnabled(Player player, boolean enabled) {
        DatabaseManager.PlayerSettings settings = getPlayerSettings(player);
        settings.setEnabled(enabled);
        updatePlayerSettings(player, settings);
    }
    
    public boolean isToolEnabled(Player player, String toolType) {
        return getPlayerSettings(player).isToolEnabled(toolType);
    }
    
    public void setToolEnabled(Player player, String toolType, boolean enabled) {
        DatabaseManager.PlayerSettings settings = getPlayerSettings(player);
        settings.setToolEnabled(toolType, enabled);
        updatePlayerSettings(player, settings);
    }
}
