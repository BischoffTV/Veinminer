package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class LevelManager {

    private final Veinminer plugin;
    private boolean enabled;
    private int blocksPerXp;
    private final Map<Integer, Integer> xpPerLevel;
    private final Map<Integer, Integer> maxBlocksPerLevel;
    private int maxLevel;

    // Level up effects
    private boolean effectsEnabled;
    private String soundEffect;
    private float soundVolume;
    private float soundPitch;

    // Particles
    private boolean particlesEnabled;
    private String particleType;
    private int particleCount;
    private double particleOffsetX;
    private double particleOffsetY;
    private double particleOffsetZ;
    private double particleSpeed;

    // Potion effects
    private boolean potionEffectsEnabled;
    private int regenerationDuration;
    private int regenerationAmplifier;
    private int speedDuration;
    private int speedAmplifier;

    // Title message
    private boolean titleEnabled;
    private String titleText;
    private String subtitleText;
    private int fadeIn;
    private int stay;
    private int fadeOut;

    public LevelManager(Veinminer plugin) {
        this.plugin = plugin;
        this.xpPerLevel = new HashMap<>();
        this.maxBlocksPerLevel = new HashMap<>();
        loadConfig();
    }

    /**
     * Load level system configuration
     */
    public void loadConfig() {
        ConfigurationSection levelSection = plugin.getConfig().getConfigurationSection("level-system");

        if (levelSection == null) {
            plugin.getLogger().warning("Level system configuration not found. Using defaults.");
            enabled = true;
            blocksPerXp = 5;
            maxLevel = 10;

            // Set default XP per level
            xpPerLevel.clear();
            xpPerLevel.put(1, 0);
            xpPerLevel.put(2, 100);
            xpPerLevel.put(3, 250);
            xpPerLevel.put(4, 500);
            xpPerLevel.put(5, 1000);
            xpPerLevel.put(6, 2000);
            xpPerLevel.put(7, 4000);
            xpPerLevel.put(8, 8000);
            xpPerLevel.put(9, 16000);
            xpPerLevel.put(10, 32000);

            // Set default max blocks per level
            maxBlocksPerLevel.clear();
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

            // Set default effects
            effectsEnabled = true;
            soundEffect = "ENTITY_PLAYER_LEVELUP";
            soundVolume = 1.0f;
            soundPitch = 1.0f;

            // Set default particles
            particlesEnabled = true;
            particleType = "TOTEM_OF_UNDYING";
            particleCount = 50;
            particleOffsetX = 0.5;
            particleOffsetY = 1.0;
            particleOffsetZ = 0.5;
            particleSpeed = 0.1;

            // Set default potion effects
            potionEffectsEnabled = true;
            regenerationDuration = 5;
            regenerationAmplifier = 1;
            speedDuration = 10;
            speedAmplifier = 0;

            // Set default title message
            titleEnabled = true;
            titleText = "&6&lLEVEL UP!";
            subtitleText = "&eYou are now level &6%level%";
            fadeIn = 10;
            stay = 70;
            fadeOut = 20;

            return;
        }

        enabled = levelSection.getBoolean("enabled", true);
        blocksPerXp = levelSection.getInt("blocks-per-xp", 5);

        // Load XP per level
        xpPerLevel.clear();
        ConfigurationSection xpSection = levelSection.getConfigurationSection("xp-per-level");
        if (xpSection != null) {
            plugin.getLogger().info("Loading XP per level from config.yml...");
            for (String key : xpSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int xp = xpSection.getInt(key);
                    xpPerLevel.put(level, xp);
                    plugin.getLogger().info("Loaded level " + level + " with XP: " + xp);

                    // Track the highest level
                    if (level > maxLevel) {
                        maxLevel = level;
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in xp-per-level: " + key);
                }
            }
            plugin.getLogger().info("Loaded " + xpPerLevel.size() + " level XP values.");
        } else {
            plugin.getLogger().warning("No xp-per-level section found in config. Using defaults.");
            // Set default XP per level
            xpPerLevel.put(1, 0);
            xpPerLevel.put(2, 100);
            xpPerLevel.put(3, 250);
            xpPerLevel.put(4, 500);
            xpPerLevel.put(5, 1000);
            xpPerLevel.put(6, 2000);
            xpPerLevel.put(7, 4000);
            xpPerLevel.put(8, 8000);
            xpPerLevel.put(9, 16000);
            xpPerLevel.put(10, 32000);
            maxLevel = 10;
        }

        // Load max blocks per level
        maxBlocksPerLevel.clear();
        ConfigurationSection blocksSection = levelSection.getConfigurationSection("max-blocks-per-level");
        if (blocksSection != null) {
            plugin.getLogger().info("Loading max blocks per level from config.yml...");
            for (String key : blocksSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int blocks = blocksSection.getInt(key);
                    maxBlocksPerLevel.put(level, blocks);
                    plugin.getLogger().info("Loaded level " + level + " with max blocks: " + blocks);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in max-blocks-per-level: " + key);
                }
            }
            plugin.getLogger().info("Loaded " + maxBlocksPerLevel.size() + " max blocks per level values.");
        } else {
            plugin.getLogger().warning("No max-blocks-per-level section found in config. Using defaults.");
            // Set default max blocks per level
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
        }

        // Load level up effects
        ConfigurationSection effectsSection = levelSection.getConfigurationSection("effects");
        if (effectsSection != null) {
            effectsEnabled = effectsSection.getBoolean("enabled", true);
            soundEffect = effectsSection.getString("sound", "ENTITY_PLAYER_LEVELUP");
            soundVolume = (float) effectsSection.getDouble("volume", 1.0);
            soundPitch = (float) effectsSection.getDouble("pitch", 1.0);

            // Load particle effects
            ConfigurationSection particlesSection = effectsSection.getConfigurationSection("particles");
            if (particlesSection != null) {
                particlesEnabled = particlesSection.getBoolean("enabled", true);
                particleType = particlesSection.getString("type", "TOTEM_OF_UNDYING");
                particleCount = particlesSection.getInt("count", 50);
                particleOffsetX = particlesSection.getDouble("offset-x", 0.5);
                particleOffsetY = particlesSection.getDouble("offset-y", 1.0);
                particleOffsetZ = particlesSection.getDouble("offset-z", 0.5);
                particleSpeed = particlesSection.getDouble("speed", 0.1);
            } else {
                particlesEnabled = true;
                particleType = "TOTEM_OF_UNDYING";
                particleCount = 50;
                particleOffsetX = 0.5;
                particleOffsetY = 1.0;
                particleOffsetZ = 0.5;
                particleSpeed = 0.1;
            }

            // Load potion effects
            ConfigurationSection potionSection = effectsSection.getConfigurationSection("potion-effects");
            if (potionSection != null) {
                potionEffectsEnabled = potionSection.getBoolean("enabled", true);
                regenerationDuration = potionSection.getInt("regeneration-duration", 5);
                regenerationAmplifier = potionSection.getInt("regeneration-amplifier", 1);
                speedDuration = potionSection.getInt("speed-duration", 10);
                speedAmplifier = potionSection.getInt("speed-amplifier", 0);
            } else {
                potionEffectsEnabled = true;
                regenerationDuration = 5;
                regenerationAmplifier = 1;
                speedDuration = 10;
                speedAmplifier = 0;
            }

            // Load title message
            ConfigurationSection titleSection = effectsSection.getConfigurationSection("title");
            if (titleSection != null) {
                titleEnabled = titleSection.getBoolean("enabled", true);
                titleText = titleSection.getString("text", "&6&lLEVEL UP!");
                subtitleText = titleSection.getString("subtitle", "&eYou are now level &6%level%");
                fadeIn = titleSection.getInt("fade-in", 10);
                stay = titleSection.getInt("stay", 70);
                fadeOut = titleSection.getInt("fade-out", 20);
            } else {
                titleEnabled = true;
                titleText = "&6&lLEVEL UP!";
                subtitleText = "&eYou are now level &6%level%";
                fadeIn = 10;
                stay = 70;
                fadeOut = 20;
            }
        } else {
            effectsEnabled = true;
            soundEffect = "ENTITY_PLAYER_LEVELUP";
            soundVolume = 1.0f;
            soundPitch = 1.0f;

            particlesEnabled = true;
            particleType = "TOTEM_OF_UNDYING";
            particleCount = 50;
            particleOffsetX = 0.5;
            particleOffsetY = 1.0;
            particleOffsetZ = 0.5;
            particleSpeed = 0.1;

            potionEffectsEnabled = true;
            regenerationDuration = 5;
            regenerationAmplifier = 1;
            speedDuration = 10;
            speedAmplifier = 0;

            titleEnabled = true;
            titleText = "&6&lLEVEL UP!";
            subtitleText = "&eYou are now level &6%level%";
            fadeIn = 10;
            stay = 70;
            fadeOut = 20;
        }
    }

    /**
     * Reload level settings from config
     */
    public void reloadLevelSettings() {
        loadConfig();
    }

    /**
     * Check if the level system is enabled
     * @return True if the level system is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the maximum level
     * @return The maximum level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Get the XP required for a level
     * @param level The level
     * @return The XP required for the level
     */
    public int getXpForLevel(int level) {
        return xpPerLevel.getOrDefault(level, level * 1000);
    }

    /**
     * Get the XP required for the next level
     * @param level The current level
     * @return The XP required for the next level
     */
    public int getXpForNextLevel(int level) {
        return getXpForLevel(level + 1);
    }

    /**
     * Get the maximum blocks that can be mined at a level
     * @param level The level
     * @return The maximum blocks that can be mined
     */
    public int getMaxBlocksForLevel(int level) {
        return maxBlocksPerLevel.getOrDefault(level, level * 8);
    }

    /**
     * Get the maximum blocks that can be mined by a player
     * @param player The player
     * @return The maximum blocks that can be mined
     */
    public int getMaxBlocks(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return getMaxBlocksForLevel(1);
        }

        return getMaxBlocksForLevel(playerData.getLevel());
    }

    /**
     * Add blocks mined to a player's statistics
     * @param player The player
     * @param amount The number of blocks mined
     */
    public void addBlocksMined(Player player, int amount) {
        if (!enabled) {
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        // Add blocks mined
        playerData.addBlocksMined(amount);

        // Calculate XP gained
        int xpGained = amount / blocksPerXp;
        if (xpGained > 0) {
            // Add XP
            int oldLevel = playerData.getLevel();
            playerData.addExperience(xpGained);

            // Check for level up
            checkLevelUp(player, playerData, oldLevel);
        }
    }

    /**
     * Check if a player has leveled up
     * @param player The player
     * @param playerData The player data
     * @param oldLevel The old level
     */
    private void checkLevelUp(Player player, PlayerData playerData, int oldLevel) {
        int currentXp = playerData.getExperience();
        int currentLevel = playerData.getLevel();

        // Check if player has enough XP for the next level
        while (currentLevel < maxLevel && currentXp >= getXpForNextLevel(currentLevel)) {
            currentLevel++;
        }

        // If level has changed, update player data and apply effects
        if (currentLevel > oldLevel) {
            playerData.setLevel(currentLevel);

            // Apply level up effects
            if (effectsEnabled) {
                applyLevelUpEffects(player, currentLevel);
            }

            // Send level up message
            String message = plugin.getMessageManager().formatMessage("messages.level.up")
                    .replace("%level%", String.valueOf(currentLevel));
            player.sendMessage(message);
        }
    }

    /**
     * Apply level up effects to a player
     * @param player The player
     * @param level The new level
     */
    private void applyLevelUpEffects(Player player, int level) {
        // Play sound effect
        try {
            Sound sound = Sound.valueOf(soundEffect);
            player.playSound(player.getLocation(), sound, soundVolume, soundPitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound effect: " + soundEffect);
        }

        // Show particles
        if (particlesEnabled) {
            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleType);
                player.getWorld().spawnParticle(
                        particle,
                        player.getLocation().add(0, 1, 0),
                        particleCount,
                        particleOffsetX,
                        particleOffsetY,
                        particleOffsetZ,
                        particleSpeed
                );
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle type: " + particleType);
            }
        }

        // Apply potion effects
        if (potionEffectsEnabled) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    regenerationDuration * 20,
                    regenerationAmplifier
            ));

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    speedDuration * 20,
                    speedAmplifier
            ));
        }

        // Show title message
        if (titleEnabled) {
            String title = titleText.replace("%level%", String.valueOf(level));
            String subtitle = subtitleText.replace("%level%", String.valueOf(level));

            title = title.replace("&", "§");
            subtitle = subtitle.replace("&", "§");

            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Set a player's level
     * @param player The player
     * @param level The level
     */
    public void setPlayerLevel(Player player, int level) {
        if (!enabled) {
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        // Set level
        playerData.setLevel(level);

        // Set XP to the minimum for this level
        playerData.setExperience(getXpForLevel(level));

        // Save player data
        plugin.getPlayerDataManager().savePlayerData(player);
    }

    /**
     * Get the XP per level map
     * @return The XP per level map
     */
    public Map<Integer, Integer> getXpPerLevel() {
        return xpPerLevel;
    }
}