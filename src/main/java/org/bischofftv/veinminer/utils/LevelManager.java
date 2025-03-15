package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.database.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelManager {
  
  private final Veinminer plugin;
  private final Map<UUID, DatabaseManager.PlayerData> playerDataCache;
  private Map<Integer, Long> xpPerLevel;
  private Map<Integer, Integer> maxBlocksPerLevel;
  private int blocksPerXp;
  private boolean enabled;
  private boolean debug;
  
  public LevelManager(Veinminer plugin) {
      this.plugin = plugin;
      this.playerDataCache = new ConcurrentHashMap<>();
      loadLevelSettings();
  }
  
private void loadLevelSettings() {
    this.xpPerLevel = new HashMap<>();
    this.maxBlocksPerLevel = new HashMap<>();
    this.enabled = plugin.getConfig().getBoolean("level-system.enabled", true);
    this.blocksPerXp = plugin.getConfig().getInt("blocks-per-xp", 5);
    this.debug = plugin.getConfig().getBoolean("settings.debug", false);

    plugin.reloadConfig();
    
    plugin.getLogger().info("Loading level settings from config");
    
    ConfigurationSection xpSection = plugin.getConfig().getConfigurationSection("level-system.xp-per-level");
    if (xpSection == null) {
        xpSection = plugin.getConfig().getConfigurationSection("xp-per-level");
        plugin.getLogger().info("Using root path for xp-per-level");
    }
    
    ConfigurationSection blocksSection = plugin.getConfig().getConfigurationSection("level-system.max-blocks-per-level");
    if (blocksSection == null) {
        blocksSection = plugin.getConfig().getConfigurationSection("max-blocks-per-level");
        plugin.getLogger().info("Using root path for max-blocks-per-level");
    }
    
    if (xpSection == null) {
        plugin.getLogger().severe("Could not find xp-per-level section in config at any path!");

        plugin.getLogger().info("Attempting direct access to level values...");
        boolean foundAny = false;
        
        for (int i = 1; i <= 10; i++) {
            if (plugin.getConfig().contains("xp-per-level." + i)) {
                long xp = plugin.getConfig().getLong("xp-per-level." + i);
                xpPerLevel.put(i, xp);
                plugin.getLogger().info("Directly loaded level " + i + " with XP: " + xp);
                foundAny = true;
            }
            
            if (plugin.getConfig().contains("max-blocks-per-level." + i)) {
                int maxBlocks = plugin.getConfig().getInt("max-blocks-per-level." + i);
                maxBlocksPerLevel.put(i, maxBlocks);
                plugin.getLogger().info("Directly loaded max blocks for level " + i + ": " + maxBlocks);
            }
        }
        
        if (!foundAny) {
            plugin.getLogger().severe("Could not load any level data! Using default values.");
            useDefaultValues();
        }
        return;
    }
    
    for (String key : xpSection.getKeys(false)) {
        try {
            int level = Integer.parseInt(key);
            long xp = xpSection.getLong(key);
            xpPerLevel.put(level, xp);
            
            plugin.getLogger().info("Loaded level " + level + " with XP: " + xp);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid level number in config: " + key);
        }
    }

    if (blocksSection != null) {
        for (String key : blocksSection.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                int maxBlocks = blocksSection.getInt(key);
                maxBlocksPerLevel.put(level, maxBlocks);
                
                plugin.getLogger().info("Loaded max blocks for level " + level + ": " + maxBlocks);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid level number in config: " + key);
            }
        }
    }
    
    boolean missingLevels = false;
    for (int i = 1; i <= 10; i++) {
        if (!xpPerLevel.containsKey(i)) {
            plugin.getLogger().warning("Missing level " + i + " in config");
            missingLevels = true;
        }
    }
    
    if (missingLevels || xpPerLevel.isEmpty()) {
        plugin.getLogger().warning("Some levels are missing from config! Using default values.");
        useDefaultValues();
    } else {
        plugin.getLogger().info("Successfully loaded " + xpPerLevel.size() + " levels from config");
    }
    
    dumpConfig();
}

private void useDefaultValues() {
    xpPerLevel.put(1, 0L);
    xpPerLevel.put(2, 100L);
    xpPerLevel.put(3, 250L);
    xpPerLevel.put(4, 500L);
    xpPerLevel.put(5, 1000L);
    xpPerLevel.put(6, 2000L);
    xpPerLevel.put(7, 4000L);
    xpPerLevel.put(8, 8000L);
    xpPerLevel.put(9, 16000L);
    xpPerLevel.put(10, 32000L);
    
    // Default max blocks values
    maxBlocksPerLevel.put(1, 8);
    maxBlocksPerLevel.put(2, 16);
    maxBlocksPerLevel.put(3, 24);
    maxBlocksPerLevel.put(4, 32);
    maxBlocksPerLevel.put(5, 48);
    maxBlocksPerLevel.put(6, 64);
    maxBlocksPerLevel.put(7, 96);
    maxBlocksPerLevel.put(8, 128);
    maxBlocksPerLevel.put(9, 192);
    maxBlocksPerLevel.put(10, 256);
    
    plugin.getLogger().info("Loaded default values for levels and max blocks");
}
  
  public void reloadLevelSettings() {
      loadLevelSettings();
  }
  
  public boolean isEnabled() {
      return enabled;
  }
  
  public void cachePlayerData(UUID uuid, DatabaseManager.PlayerData playerData) {
      playerDataCache.put(uuid, playerData);
  }
  
  public DatabaseManager.PlayerData getPlayerData(UUID uuid) {
      return playerDataCache.get(uuid);
  }
  
  public void removePlayerData(UUID uuid) {
      playerDataCache.remove(uuid);
  }
  
  public int getMaxBlocks(Player player) {
      if (!enabled) {
          return plugin.getConfig().getInt("settings.max-blocks", 64);
      }
      
      UUID uuid = player.getUniqueId();
      if (!playerDataCache.containsKey(uuid)) {
          return getMaxBlocksForLevel(1);
      }
      
      int level = playerDataCache.get(uuid).getLevel();
      return getMaxBlocksForLevel(level);
  }
  
  public int getMaxBlocksForLevel(int level) {
      int maxBlocks = maxBlocksPerLevel.getOrDefault(1, 8); 
      
      for (Map.Entry<Integer, Integer> entry : maxBlocksPerLevel.entrySet()) {
          if (entry.getKey() <= level && entry.getValue() > maxBlocks) {
              maxBlocks = entry.getValue();
          }
      }
      
      return maxBlocks;
  }
  
  public void addBlocksMined(Player player, int blocksMined) {
      if (!enabled) {
          return;
      }
      
      UUID uuid = player.getUniqueId();
      if (!playerDataCache.containsKey(uuid)) {
          return;
      }
      
      DatabaseManager.PlayerData playerData = playerDataCache.get(uuid);
      int oldBlocksMined = playerData.getBlocksMined();
      playerData.setBlocksMined(oldBlocksMined + blocksMined);
      
      int oldTotalXpBlocks = oldBlocksMined / blocksPerXp;
      int newTotalXpBlocks = playerData.getBlocksMined() / blocksPerXp;
      int xpGain = newTotalXpBlocks - oldTotalXpBlocks;
      
      if (debug) {
          plugin.debug("[Debug] Player: " + player.getName() + 
                                ", Blocks Mined: " + blocksMined + 
                                ", Old Total: " + oldBlocksMined + 
                                ", New Total: " + playerData.getBlocksMined() + 
                                ", XP Gain: " + xpGain);
      }
      
      if (xpGain > 0) {
          addXp(player, xpGain);
      }
      
      plugin.getDatabaseManager().savePlayerData(playerData);
  }
  
  public void addXp(Player player, long xpToAdd) { 
    if (!enabled || xpToAdd <= 0) {
        return;
    }
    
    UUID uuid = player.getUniqueId();
    if (!playerDataCache.containsKey(uuid)) {
        return;
    }
    
    DatabaseManager.PlayerData playerData = playerDataCache.get(uuid);
    int currentLevel = playerData.getLevel();
    long currentXp = playerData.getXp();
    long newXp = currentXp + xpToAdd;
    playerData.setXp(newXp);
    
    if (debug) {
        plugin.debug("[Debug] Player: " + player.getName() + 
                              ", Current Level: " + currentLevel + 
                              ", Current XP: " + currentXp + 
                              ", New XP: " + newXp);
    }

    int maxLevel = getMaxLevel();
    boolean leveledUp = false;
    int newLevel = currentLevel;
    
    if (currentLevel < maxLevel) {
        while (currentLevel < maxLevel && newXp >= getXpForLevel(currentLevel + 1)) {
            currentLevel++;
            leveledUp = true;
            newLevel = currentLevel;

            playerData.setLevel(currentLevel);
            
            int maxBlocks = getMaxBlocksForLevel(currentLevel);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("level", String.valueOf(currentLevel));
            placeholders.put("max_blocks", String.valueOf(maxBlocks));
            player.sendMessage(plugin.getMessageManager().getMessage("messages.level-up", placeholders));
            
            if (debug) {
                plugin.debug("[Debug] Player: " + player.getName() +
                                      ", Level Up! New Level: " + currentLevel +
                                      ", Max Blocks: " + maxBlocks);
            }
        }
    }
    
    plugin.getDatabaseManager().savePlayerData(playerData);
    
    if (leveledUp && plugin.getAchievementManager().isEnabled()) {
        plugin.getAchievementManager().updateLevelProgress(player, newLevel);
        
        if (debug) {
            plugin.debug("[Debug] Checked level achievements after leveling up to " + newLevel);
        }
    }
}
  
  public long getXpForLevel(int level) {
      return xpPerLevel.getOrDefault(level, 0L);
  }
  
  public Map<Integer, Long> getXpPerLevel() {
    return new HashMap<>(xpPerLevel);
}

public int getMaxLevel() {
    if (xpPerLevel.isEmpty()) {
        plugin.getLogger().severe("XP per level map is empty! Using default max level of 10.");
        return 10;
    }
    
    int maxLevel = 1;
    for (int level : xpPerLevel.keySet()) {
        if (level > maxLevel) {
            maxLevel = level;
        }
    }
    
    plugin.getLogger().info("Max level determined to be: " + maxLevel);
    return maxLevel;
}

  public long getXpForNextLevel(int currentLevel) {
      return xpPerLevel.getOrDefault(currentLevel + 1, Long.MAX_VALUE);
  }
  
  public boolean setPlayerLevel(Player player, int level) {
    if (!enabled) {
        return false;
    }
    
    UUID uuid = player.getUniqueId();
    if (!playerDataCache.containsKey(uuid)) {
        try {
            DatabaseManager.PlayerData playerData = plugin.getDatabaseManager().loadPlayerData(player).get();
            cachePlayerData(uuid, playerData);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player data for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    if (!playerDataCache.containsKey(uuid)) {
        return false;
    }
    
    DatabaseManager.PlayerData playerData = playerDataCache.get(uuid);
    
    playerData.setLevel(level);

    long xpForLevel = getXpForLevel(level);
    playerData.setXp(xpForLevel);
    
    plugin.getDatabaseManager().savePlayerData(playerData);
    
    if (plugin.getAchievementManager().isEnabled()) {
        plugin.getAchievementManager().updateLevelProgress(player, level);
        
        if (debug) {
            plugin.debug("[Debug] Checked level achievements after setting level to " + level);
        }
    }
    
    if (debug) {
        plugin.debug("[Debug] Admin set level for " + player.getName() + 
                              " to Level: " + level + 
                              ", XP: " + xpForLevel);
    }
    
    return true;
}

public void dumpConfig() {
    plugin.getLogger().info("=== Current Configuration ===");
    plugin.getLogger().info("Level system enabled: " + enabled);
    plugin.getLogger().info("Blocks per XP: " + blocksPerXp);
    plugin.getLogger().info("Debug mode: " + debug);
    
    plugin.getLogger().info("=== XP Per Level ===");
    for (Map.Entry<Integer, Long> entry : xpPerLevel.entrySet()) {
        plugin.getLogger().info("Level " + entry.getKey() + ": " + entry.getValue() + " XP");
    }
    
    plugin.getLogger().info("=== Max Blocks Per Level ===");
    for (Map.Entry<Integer, Integer> entry : maxBlocksPerLevel.entrySet()) {
        plugin.getLogger().info("Level " + entry.getKey() + ": " + entry.getValue() + " blocks");
    }
}
}
