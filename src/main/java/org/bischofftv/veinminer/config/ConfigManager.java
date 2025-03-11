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

        // Load allowed blocks
        allowedBlocks = new HashSet<>();
        List<String> blockList = config.getStringList("settings.allowed-blocks");
        for (String blockName : blockList) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                allowedBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in config: " + blockName);
            }
        }
    }

    public void reloadConfig() {
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
}

