package org.bischofftv.veinminer.config;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final Veinminer plugin;
    private FileConfiguration config;

    // Config values
    private int maxBlocks;
    private boolean useDurabilityMultiplier;
    private double durabilityMultiplier;
    private boolean useHungerMultiplier;
    private double hungerMultiplier;
    private boolean enableDiscordLogging;
    private String discordWebhookUrl;
    private Set<Material> allowedBlocks;
    private int autoSaveInterval;

    public ConfigManager(Veinminer plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();

        // Reload config
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load values from config
        maxBlocks = config.getInt("settings.max-blocks", 64);
        useDurabilityMultiplier = config.getBoolean("settings.use-durability-multiplier", true);
        durabilityMultiplier = config.getDouble("settings.durability-multiplier", 1.0);
        useHungerMultiplier = config.getBoolean("settings.use-hunger-multiplier", true);
        hungerMultiplier = config.getDouble("settings.hunger-multiplier", 0.1);
        enableDiscordLogging = config.getBoolean("logging.enable-discord-logging", false);
        discordWebhookUrl = config.getString("logging.discord-webhook-url", "");
        autoSaveInterval = config.getInt("settings.auto-save-interval", 15);

        // Add a new setting for database logging level
        if (!config.isSet("database.reduce-logging")) {
            config.set("database.reduce-logging", true);
            plugin.saveConfig();
        }

        // Load allowed blocks
        allowedBlocks = new HashSet<>();
        List<String> blockList = config.getStringList("allowed-blocks");

        // If the list is empty, try the settings.allowed-blocks path
        if (blockList.isEmpty()) {
            blockList = config.getStringList("settings.allowed-blocks");
        }

        if (blockList.isEmpty()) {
            plugin.getLogger().warning("No allowed blocks found in config! Using default ore blocks.");
            // Add default ore blocks
            allowedBlocks.add(Material.COAL_ORE);
            allowedBlocks.add(Material.DEEPSLATE_COAL_ORE);
            allowedBlocks.add(Material.IRON_ORE);
            allowedBlocks.add(Material.DEEPSLATE_IRON_ORE);
            allowedBlocks.add(Material.GOLD_ORE);
            allowedBlocks.add(Material.DEEPSLATE_GOLD_ORE);
            allowedBlocks.add(Material.DIAMOND_ORE);
            allowedBlocks.add(Material.DEEPSLATE_DIAMOND_ORE);
        } else {
            for (String blockName : blockList) {
                try {
                    Material material = Material.valueOf(blockName.toUpperCase());
                    allowedBlocks.add(material);
                    plugin.getLogger().info("Added allowed block: " + material.name());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in config: " + blockName);
                }
            }
        }

        plugin.getLogger().info("Loaded " + allowedBlocks.size() + " allowed blocks");

        // Load GUI settings
        if (!config.isSet("gui.show-about")) {
            config.set("gui.show-about", true);
            plugin.saveConfig();
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

    // Add the missing reload method
    public void reload() {
        plugin.reloadConfig();
        loadConfig();
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public boolean isUseDurabilityMultiplier() {
        return useDurabilityMultiplier;
    }

    public double getDurabilityMultiplier() {
        return durabilityMultiplier;
    }

    public boolean isUseHungerMultiplier() {
        return useHungerMultiplier;
    }

    public double getHungerMultiplier() {
        return hungerMultiplier;
    }

    public boolean isEnableDiscordLogging() {
        return enableDiscordLogging;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    public Set<Material> getAllowedBlocks() {
        return allowedBlocks;
    }

    public boolean isAllowedBlock(Material material) {
        return allowedBlocks.contains(material);
    }

    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    // Add a getter for the new config option
    public boolean isReduceDatabaseLogging() {
        return plugin.getConfig().getBoolean("database.reduce-logging", true);
    }

    // Debug method to print all allowed blocks
    public void dumpAllowedBlocks() {
        plugin.getLogger().info("=== Allowed Blocks ===");
        for (Material material : allowedBlocks) {
            plugin.getLogger().info("- " + material.name());
        }
    }
}