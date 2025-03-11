package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.database.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelManager {

    private final Veinminer plugin;
    private final Map<UUID, DatabaseManager.PlayerData> playerDataCache;
    private Map<Integer, Integer> xpPerLevel;
    private Map<Integer, Integer> maxBlocksPerLevel;
    private int blocksPerXp;
    private boolean enabled;
    private boolean debug;

    public LevelManager(Veinminer plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
        loadLevelSettings();
    }

    public void loadLevelSettings() {
        this.xpPerLevel = new HashMap<>();
        this.maxBlocksPerLevel = new HashMap<>();
        this.enabled = plugin.getConfig().getBoolean("level-system.enabled", true);
        this.blocksPerXp = plugin.getConfig().getInt("level-system.blocks-per-xp", 5);
        this.debug = plugin.getConfig().getBoolean("settings.debug", false);

        // Load XP requirements for each level
        if (plugin.getConfig().getConfigurationSection("level-system.xp-per-level") != null) {
            for (String key : plugin.getConfig().getConfigurationSection("level-system.xp-per-level").getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int xp = plugin.getConfig().getInt("level-system.xp-per-level." + key);
                    xpPerLevel.put(level, xp);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in config: " + key);
                }
            }
        }

        // Load max blocks for each level
        if (plugin.getConfig().getConfigurationSection("level-system.max-blocks-per-level") != null) {
            for (String key : plugin.getConfig().getConfigurationSection("level-system.max-blocks-per-level").getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int maxBlocks = plugin.getConfig().getInt("level-system.max-blocks-per-level." + key);
                    maxBlocksPerLevel.put(level, maxBlocks);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in config: " + key);
                }
            }
        }
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
            return getMaxBlocksForLevel(1); // Default to level 1 if data not loaded yet
        }

        int level = playerDataCache.get(uuid).getLevel();
        return getMaxBlocksForLevel(level);
    }

    public int getMaxBlocksForLevel(int level) {
        // Find the highest level that doesn't exceed the player's level
        int maxBlocks = maxBlocksPerLevel.getOrDefault(1, 8); // Default to level 1 value

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

        // Calculate XP gain
        int oldTotalXpBlocks = oldBlocksMined / blocksPerXp;
        int newTotalXpBlocks = playerData.getBlocksMined() / blocksPerXp;
        int xpGain = newTotalXpBlocks - oldTotalXpBlocks;

        if (debug) {
            plugin.getLogger().info("[Debug] Player: " + player.getName() +
                    ", Blocks Mined: " + blocksMined +
                    ", Old Total: " + oldBlocksMined +
                    ", New Total: " + playerData.getBlocksMined() +
                    ", XP Gain: " + xpGain);
        }

        if (xpGain > 0) {
            addXp(player, xpGain);
        }

        // Save to database
        plugin.getDatabaseManager().savePlayerData(playerData);
    }

    public void addXp(Player player, int xp) {
        if (!enabled || xp <= 0) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!playerDataCache.containsKey(uuid)) {
            return;
        }

        DatabaseManager.PlayerData playerData = playerDataCache.get(uuid);
        int currentLevel = playerData.getLevel();
        int currentXp = playerData.getXp();
        int newXp = currentXp + xp;
        playerData.setXp(newXp);

        if (debug) {
            plugin.getLogger().info("[Debug] Player: " + player.getName() +
                    ", Current Level: " + currentLevel +
                    ", Current XP: " + currentXp +
                    ", New XP: " + newXp);
        }

        // Check for level up
        int maxLevel = getMaxLevel();
        if (currentLevel < maxLevel) {
            int nextLevelXp = getXpForLevel(currentLevel + 1);

            if (newXp >= nextLevelXp) {
                // Level up!
                int newLevel = currentLevel + 1;

                // Check for multiple level ups
                while (newLevel < maxLevel && newXp >= getXpForLevel(newLevel + 1)) {
                    newLevel++;
                }

                playerData.setLevel(newLevel);

                // Send level up message
                int maxBlocks = getMaxBlocksForLevel(newLevel);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("level", String.valueOf(newLevel));
                placeholders.put("max_blocks", String.valueOf(maxBlocks));
                player.sendMessage(plugin.getMessageManager().getMessage("messages.level-up", placeholders));

                if (debug) {
                    plugin.getLogger().info("[Debug] Player: " + player.getName() +
                            ", Level Up! New Level: " + newLevel +
                            ", Max Blocks: " + maxBlocks);
                }
            }
        }

        // Save to database
        plugin.getDatabaseManager().savePlayerData(playerData);
    }

    private int getXpForLevel(int level) {
        return xpPerLevel.getOrDefault(level, Integer.MAX_VALUE);
    }

    public int getMaxLevel() {
        int maxLevel = 1;
        for (int level : xpPerLevel.keySet()) {
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
        return maxLevel;
    }

    public int getXpForNextLevel(int currentLevel) {
        return xpPerLevel.getOrDefault(currentLevel + 1, Integer.MAX_VALUE);
    }
}

