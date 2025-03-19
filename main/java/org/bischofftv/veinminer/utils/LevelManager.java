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
import org.bischofftv.veinminer.database.PlayerData;

public class LevelManager {

    private final Veinminer plugin;
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<Integer, Long> levelXpRequirements;
    private int blocksPerXp;
    private boolean enabled;
    private boolean debug;
    private final int maxLevel;
    private Map<Integer, Integer> maxBlocksPerLevel;
    // Entferne die doppelten Methoden und definiere xpPerLevel als Feld
    private Map<Integer, Long> xpPerLevel;

    public LevelManager(Veinminer plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.levelXpRequirements = new HashMap<>();
        this.maxLevel = plugin.getConfig().getInt("leveling.max-level", 100);
        loadLevelSettings();
        // Load XP requirements for each level
        //loadXpRequirements();
    }

    // Replace the loadLevelSettings method with this version that looks for the correct paths
    private void loadLevelSettings() {
        this.xpPerLevel = new HashMap<>();
        this.maxBlocksPerLevel = new HashMap<>();
        this.enabled = plugin.getConfig().getBoolean("level-system.enabled", true);
        this.blocksPerXp = plugin.getConfig().getInt("blocks-per-xp", 5);
        this.debug = plugin.getConfig().getBoolean("settings.debug", false);

        // Force reload the config to ensure we have fresh data
        plugin.reloadConfig();

        plugin.getLogger().info("Loading level settings from config");

        // Try both possible paths for the configuration sections
        ConfigurationSection xpSection = plugin.getConfig().getConfigurationSection("level-system.xp-per-level");
        if (xpSection == null) {
            // Try the root path instead
            xpSection = plugin.getConfig().getConfigurationSection("xp-per-level");
            plugin.getLogger().info("Using root path for xp-per-level");
        }

        ConfigurationSection blocksSection = plugin.getConfig().getConfigurationSection("level-system.max-blocks-per-level");
        if (blocksSection == null) {
            // Try the root path instead
            blocksSection = plugin.getConfig().getConfigurationSection("max-blocks-per-level");
            plugin.getLogger().info("Using root path for max-blocks-per-level");
        }

        if (xpSection == null) {
            plugin.getLogger().severe("Could not find xp-per-level section in config at any path!");

            // Try direct access to the values
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

        // Load XP values
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

        // Load max blocks values
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

        // Verify that we loaded all levels
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

        // Dump the loaded configuration for debugging
        dumpConfig();
    }

    private void useDefaultValues() {
        // Default XP values
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

    public void cachePlayerData(UUID uuid, PlayerData playerData) {
        playerDataCache.put(uuid, playerData);
    }

    public PlayerData getPlayerData(UUID uuid) {
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

        PlayerData playerData = playerDataCache.get(uuid);
        int oldBlocksMined = playerData.getBlocksMined();
        playerData.setBlocksMined(oldBlocksMined + blocksMined);

        // Calculate XP gain
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

        // Save to database
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

        PlayerData playerData = playerDataCache.get(uuid);
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

        // Check for level up
        int maxLevel = getMaxLevel();
        boolean leveledUp = false;
        int newLevel = currentLevel;

        if (currentLevel < maxLevel) {
            while (currentLevel < maxLevel && newXp >= getXpForLevel(currentLevel + 1)) {
                currentLevel++;
                leveledUp = true;
                newLevel = currentLevel;

                // Set the new level
                playerData.setLevel(currentLevel);

                // Send level-up message
                int maxBlocks = getMaxBlocksForLevel(currentLevel);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("level", String.valueOf(currentLevel));
                placeholders.put("max_blocks", String.valueOf(maxBlocks));
                player.sendMessage(plugin.getMessageManager().getMessage("messages.level-up", placeholders));

                // Apply level up effects
                applyLevelUpEffects(player);

                if (debug) {
                    plugin.debug("[Debug] Player: " + player.getName() +
                            ", Level Up! New Level: " + currentLevel +
                            ", Max Blocks: " + maxBlocks);
                }
            }
        }

        // Save to database
        plugin.getDatabaseManager().savePlayerData(playerData);

        // Trigger achievement check if player leveled up
        if (leveledUp && plugin.getAchievementManager().isEnabled()) {
            plugin.getAchievementManager().updateLevelProgress(player, newLevel);

            if (debug) {
                plugin.debug("[Debug] Checked level achievements after leveling up to " + newLevel);
            }
        }
    }

    //public long getXpForLevel(int level) {
    //    return xpPerLevel.getOrDefault(level, 0L);
    //}

    // Füge die fehlende getXpForLevel-Methode hinzu
    public long getXpForLevel(int level) {
        return xpPerLevel.getOrDefault(level, 0L);
    }


    public Map<Integer, Long> getXpPerLevel() {
        return new HashMap<>(xpPerLevel);
    }

    // Replace the getMaxLevel method with this simpler version
    public int getMaxLevel() {
        if (xpPerLevel.isEmpty()) {
            plugin.getLogger().severe("XP per level map is empty! Using default max level of 10.");
            return 10; // Default to 10 if the map is empty
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

    /**
     * Sets a player's level to the specified level.
     *
     * @param player The player to set the level for
     * @param level The level to set
     * @return true if successful, false otherwise
     */
    public boolean setPlayerLevel(Player player, int level) {
        if (!enabled) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        if (!playerDataCache.containsKey(uuid)) {
            // Try to load player data if not in cache
            try {
                PlayerData playerData = plugin.getDatabaseManager().loadPlayerData(player).get();
                cachePlayerData(uuid, playerData);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load player data for " + player.getName() + ": " + e.getMessage());
                return false;
            }
        }

        if (!playerDataCache.containsKey(uuid)) {
            return false;
        }

        PlayerData playerData = playerDataCache.get(uuid);

        // Set the new level
        playerData.setLevel(level);

        // Set XP to the minimum required for this level
        long xpForLevel = getXpForLevel(level);
        playerData.setXp(xpForLevel);

        // Save to database
        plugin.getDatabaseManager().savePlayerData(playerData);

        // Trigger achievement check for the new level
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

    // Add this method to help with debugging
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

/* private void loadXpRequirements() {
        // Default formula: level^2 * 100
        for (int level = 1; level <= maxLevel; level++) {
            String customXp = plugin.getConfig().getString("leveling.levels." + level + ".xp");
            long xpRequired;

            if (customXp != null) {
                try {
                    xpRequired = Long.parseLong(customXp);
                } catch (NumberFormatException e) {
                    xpRequired = (long) (Math.pow(level, 2) * 100);
                    plugin.getLogger().warning("Invalid XP value for level " + level + ": " + customXp + ". Using default: " + xpRequired);
                }
            } else {
                xpRequired = (long) (Math.pow(level, 2) * 100);
            }

            levelXpRequirements.put(level, xpRequired);
        }
    }



    public void addPlayerXp(Player player, long xpAmount) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = getPlayerData(uuid);

        if (playerData == null) {
            // Load player data if not cached
            try {
                playerData = plugin.getDatabaseManager().loadPlayerData(player).get();
                cachePlayerData(uuid, playerData);
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading player data for " + player.getName() + ": " + e.getMessage());
                return;
            }
        }

        // Add XP
        long newXp = playerData.getXp() + xpAmount;
        playerData.setXp(newXp);

        // Check for level up
        int currentLevel = playerData.getLevel();
        if (currentLevel < maxLevel) {
            int newLevel = currentLevel;

            // Check if player can level up multiple times
            while (newLevel < maxLevel && newXp >= getXpForLevel(newLevel + 1)) {
                newLevel++;
            }

            // Level up if needed
            if (newLevel > currentLevel) {
                playerData.setLevel(newLevel);

                // Send level up message
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("level", String.valueOf(newLevel));
                player.sendMessage(plugin.getMessageManager().getMessage("messages.level.level-up", placeholders));

                // Apply level up effects if configured
                if (plugin.getConfig().getBoolean("leveling.effects.enabled", true)) {
                    // TODO: Add level up effects
                }
            }
        }

        // Save player data
        plugin.getDatabaseManager().savePlayerData(playerData);
    }


    public void setPlayerLevel(Player player, int level) {
        if (level < 1 || level > maxLevel) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerData playerData = getPlayerData(uuid);

        if (playerData == null) {
            // Load player data if not cached
            try {
                playerData = plugin.getDatabaseManager().loadPlayerData(player).get();
                cachePlayerData(uuid, playerData);
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading player data for " + player.getName() + ": " + e.getMessage());
                return;
            }
        }

        // Set level and reset XP
        playerData.setLevel(level);
        playerData.setXp(0);

        // Save player data
        plugin.getDatabaseManager().savePlayerData(playerData);

        // Send message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("level", String.valueOf(level));
        player.sendMessage(plugin.getMessageManager().getMessage("messages.level.level-set", placeholders));
    }

    public long getXpForLevel(int level) {
        return levelXpRequirements.getOrDefault(level, (long) (Math.pow(level, 2) * 100));
    }


}
*/

    /**
     * Applies visual and sound effects when a player levels up
     *
     * @param player The player who leveled up
     */
    private void applyLevelUpEffects(Player player) {
        // Check if effects are enabled in config
        if (!plugin.getConfig().getBoolean("level-system.effects.enabled", true)) {
            return;
        }

        try {
            // Play sound effect
            String soundName = plugin.getConfig().getString("level-system.effects.sound", "ENTITY_PLAYER_LEVELUP");
            float volume = (float) plugin.getConfig().getDouble("level-system.effects.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("level-system.effects.pitch", 1.0);

            try {
                // Try to parse the sound name as an enum
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);

                if (debug) {
                    plugin.debug("[Debug] Played level up sound: " + soundName);
                }
            } catch (IllegalArgumentException e) {
                // If the sound name is invalid, use a default sound
                plugin.getLogger().warning("Invalid sound name in config: " + soundName + ". Using default sound.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, volume, pitch);
            }

            // Spawn particles
            boolean spawnParticles = plugin.getConfig().getBoolean("level-system.effects.particles.enabled", true);
            if (spawnParticles) {
                String particleName = plugin.getConfig().getString("level-system.effects.particles.type", "TOTEM_OF_UNDYING");
                int count = plugin.getConfig().getInt("level-system.effects.particles.count", 50);
                double offsetX = plugin.getConfig().getDouble("level-system.effects.particles.offset-x", 0.5);
                double offsetY = plugin.getConfig().getDouble("level-system.effects.particles.offset-y", 1.0);
                double offsetZ = plugin.getConfig().getDouble("level-system.effects.particles.offset-z", 0.5);
                double speed = plugin.getConfig().getDouble("level-system.effects.particles.speed", 0.1);

                try {
                    // Try to parse the particle name as an enum
                    org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName);
                    player.getWorld().spawnParticle(
                            particle,
                            player.getLocation().add(0, 1, 0), // Spawn at player's head level
                            count,
                            offsetX,
                            offsetY,
                            offsetZ,
                            speed
                    );

                    if (debug) {
                        plugin.debug("[Debug] Spawned level up particles: " + particleName);
                    }
                } catch (IllegalArgumentException e) {
                    // If the particle name is invalid, use a default particle
                    plugin.getLogger().warning("Invalid particle name in config: " + particleName + ". Using default particle.");
                    player.getWorld().spawnParticle(
                            org.bukkit.Particle.TOTEM_OF_UNDYING,
                            player.getLocation().add(0, 1, 0),
                            count,
                            offsetX,
                            offsetY,
                            offsetZ,
                            speed
                    );
                }
            }

            // Apply potion effects
            boolean applyPotionEffects = plugin.getConfig().getBoolean("level-system.effects.potion-effects.enabled", true);
            if (applyPotionEffects) {
                // Apply regeneration effect
                int regenDuration = plugin.getConfig().getInt("level-system.effects.potion-effects.regeneration-duration", 5);
                int regenAmplifier = plugin.getConfig().getInt("level-system.effects.potion-effects.regeneration-amplifier", 1);

                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.REGENERATION,
                        regenDuration * 20, // Convert seconds to ticks
                        regenAmplifier
                ));

                // Apply speed effect
                int speedDuration = plugin.getConfig().getInt("level-system.effects.potion-effects.speed-duration", 10);
                int speedAmplifier = plugin.getConfig().getInt("level-system.effects.potion-effects.speed-amplifier", 0);

                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SPEED,
                        speedDuration * 20, // Convert seconds to ticks
                        speedAmplifier
                ));

                if (debug) {
                    plugin.debug("[Debug] Applied level up potion effects");
                }
            }

            // Send title message
            boolean showTitle = plugin.getConfig().getBoolean("level-system.effects.title.enabled", true);
            if (showTitle) {
                String title = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("level-system.effects.title.text", "&6&lLEVEL UP!"));
                String subtitle = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("level-system.effects.title.subtitle", "&eYou are now level &6%level%")
                                .replace("%level%", String.valueOf(player.getLevel())));

                int fadeIn = plugin.getConfig().getInt("level-system.effects.title.fade-in", 10);
                int stay = plugin.getConfig().getInt("level-system.effects.title.stay", 70);
                int fadeOut = plugin.getConfig().getInt("level-system.effects.title.fade-out", 20);

                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);

                if (debug) {
                    plugin.debug("[Debug] Sent level up title");
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error applying level up effects: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
        }
    }
}