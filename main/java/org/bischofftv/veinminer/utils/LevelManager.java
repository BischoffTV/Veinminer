package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class LevelManager {

    private final Veinminer plugin;
    private boolean enabled;
    private int blocksPerXp;
    private Map<Integer, Integer> xpPerLevel;
    private Map<Integer, Integer> maxBlocksPerLevel;

    // Level up effects
    private boolean effectsEnabled;
    private Sound levelUpSound;
    private float soundVolume;
    private float soundPitch;
    private boolean particlesEnabled;
    private Particle particleType;
    private int particleCount;
    private double particleOffsetX;
    private double particleOffsetY;
    private double particleOffsetZ;
    private double particleSpeed;
    private boolean potionEffectsEnabled;
    private int regenerationDuration;
    private int regenerationAmplifier;
    private int speedDuration;
    private int speedAmplifier;
    private boolean titleEnabled;
    private String titleText;
    private String subtitleText;
    private int fadeIn;
    private int stay;
    private int fadeOut;

    public LevelManager(Veinminer plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load level system configuration
     */
    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("level-system.enabled", true);
        blocksPerXp = plugin.getConfig().getInt("level-system.blocks-per-xp", 5);

        // Load XP per level
        xpPerLevel = new HashMap<>();
        ConfigurationSection xpSection = plugin.getConfig().getConfigurationSection("level-system.xp-per-level");
        if (xpSection != null) {
            for (String key : xpSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int xp = xpSection.getInt(key);
                    xpPerLevel.put(level, xp);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in xp-per-level: " + key);
                }
            }
        }

        // Load max blocks per level
        maxBlocksPerLevel = new HashMap<>();
        ConfigurationSection blocksSection = plugin.getConfig().getConfigurationSection("level-system.max-blocks-per-level");
        if (blocksSection != null) {
            for (String key : blocksSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int blocks = blocksSection.getInt(key);
                    maxBlocksPerLevel.put(level, blocks);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in max-blocks-per-level: " + key);
                }
            }
        }

        // Load level up effects
        ConfigurationSection effectsSection = plugin.getConfig().getConfigurationSection("level-system.effects");
        if (effectsSection != null) {
            effectsEnabled = effectsSection.getBoolean("enabled", true);

            // Sound effects
            levelUpSound = Sound.valueOf(effectsSection.getString("sound", "ENTITY_PLAYER_LEVELUP"));
            soundVolume = (float) effectsSection.getDouble("volume", 1.0);
            soundPitch = (float) effectsSection.getDouble("pitch", 1.0);

            // Particle effects
            ConfigurationSection particlesSection = effectsSection.getConfigurationSection("particles");
            if (particlesSection != null) {
                particlesEnabled = particlesSection.getBoolean("enabled", true);
                particleType = Particle.valueOf(particlesSection.getString("type", "TOTEM_OF_UNDYING"));
                particleCount = particlesSection.getInt("count", 50);
                particleOffsetX = particlesSection.getDouble("offset-x", 0.5);
                particleOffsetY = particlesSection.getDouble("offset-y", 1.0);
                particleOffsetZ = particlesSection.getDouble("offset-z", 0.5);
                particleSpeed = particlesSection.getDouble("speed", 0.1);
            }

            // Potion effects
            ConfigurationSection potionSection = effectsSection.getConfigurationSection("potion-effects");
            if (potionSection != null) {
                potionEffectsEnabled = potionSection.getBoolean("enabled", true);
                regenerationDuration = potionSection.getInt("regeneration-duration", 5);
                regenerationAmplifier = potionSection.getInt("regeneration-amplifier", 1);
                speedDuration = potionSection.getInt("speed-duration", 10);
                speedAmplifier = potionSection.getInt("speed-amplifier", 0);
            }

            // Title message
            ConfigurationSection titleSection = effectsSection.getConfigurationSection("title");
            if (titleSection != null) {
                titleEnabled = titleSection.getBoolean("enabled", true);
                titleText = titleSection.getString("text", "&6&lLEVEL UP!");
                subtitleText = titleSection.getString("subtitle", "&eYou are now level &6%level%");
                fadeIn = titleSection.getInt("fade-in", 10);
                stay = titleSection.getInt("stay", 70);
                fadeOut = titleSection.getInt("fade-out", 20);
            }
        }
    }

    /**
     * Check if the level system is enabled
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the maximum number of blocks that can be mined at once for a level
     * @param level The level
     * @return The maximum number of blocks
     */
    public int getMaxBlocksForLevel(int level) {
        // If level system is disabled, use the global max blocks
        if (!enabled) {
            return plugin.getConfig().getInt("settings.max-blocks", 64);
        }

        // Find the highest level that is less than or equal to the player's level
        int maxBlocks = 8; // Default value
        for (int i = 1; i <= level; i++) {
            if (maxBlocksPerLevel.containsKey(i)) {
                maxBlocks = maxBlocksPerLevel.get(i);
            }
        }

        return maxBlocks;
    }

    /**
     * Get the XP required for the next level
     * @param currentLevel The current level
     * @return The XP required for the next level
     */
    public int getXpForNextLevel(int currentLevel) {
        int nextLevel = currentLevel + 1;
        return xpPerLevel.getOrDefault(nextLevel, Integer.MAX_VALUE);
    }

    /**
     * Add blocks mined to a player's stats and potentially level them up
     * @param player The player
     * @param blocksMined The number of blocks mined
     */
    public void addBlocksMined(Player player, int blocksMined) {
        if (!enabled) {
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        // Add blocks mined
        playerData.addBlocksMined(blocksMined);

        // Calculate XP gained
        int xpGained = blocksMined / blocksPerXp;
        if (xpGained > 0) {
            addExperience(player, xpGained);
        }
    }

    /**
     * Add experience to a player and potentially level them up
     * @param player The player
     * @param experience The amount of experience to add
     */
    public void addExperience(Player player, int experience) {
        if (!enabled) {
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        // Add experience
        playerData.addExperience(experience);

        // Check for level up
        checkLevelUp(player, playerData);
    }

    /**
     * Check if a player should level up
     * @param player The player
     * @param playerData The player data
     */
    private void checkLevelUp(Player player, PlayerData playerData) {
        int currentLevel = playerData.getLevel();
        int currentXp = playerData.getExperience();

        // Get XP required for next level
        int nextLevelXp = getXpForNextLevel(currentLevel);

        // Check if player has enough XP to level up
        if (currentXp >= nextLevelXp) {
            // Level up
            playerData.setLevel(currentLevel + 1);

            // Add skill points
            playerData.addSkillPoints(1);

            // Apply level up effects
            applyLevelUpEffects(player, currentLevel + 1);

            // Send level up message
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.level.up", "%level%", String.valueOf(currentLevel + 1)));

            // Check for further level ups
            checkLevelUp(player, playerData);
        }
    }

    /**
     * Apply level up effects to a player
     * @param player The player
     * @param newLevel The new level
     */
    private void applyLevelUpEffects(Player player, int newLevel) {
        if (!effectsEnabled) {
            return;
        }

        // Play sound
        player.playSound(player.getLocation(), levelUpSound, soundVolume, soundPitch);

        // Spawn particles
        if (particlesEnabled) {
            player.getWorld().spawnParticle(
                    particleType,
                    player.getLocation().add(0, 1, 0),
                    particleCount,
                    particleOffsetX,
                    particleOffsetY,
                    particleOffsetZ,
                    particleSpeed
            );
        }

        // Apply potion effects
        if (potionEffectsEnabled) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration * 20, regenerationAmplifier));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDuration * 20, speedAmplifier));
        }

        // Show title
        if (titleEnabled) {
            String title = titleText.replace("%level%", String.valueOf(newLevel));
            String subtitle = subtitleText.replace("%level%", String.valueOf(newLevel));

            player.sendTitle(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', title),
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', subtitle),
                    fadeIn,
                    stay,
                    fadeOut
            );
        }
    }
}