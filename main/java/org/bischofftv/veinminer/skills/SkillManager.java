package org.bischofftv.veinminer.skills;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class SkillManager {

    private final Veinminer plugin;
    private boolean enabled;
    private int maxSkillLevel;
    private boolean resetCostEnabled;
    private double resetCost;
    private String resetCommand;

    // Skill boost values
    private Map<Integer, Double> efficiencyBoost;
    private Map<Integer, Double> luckEnhancement;
    private Map<Integer, Double> energyConservation;

    public SkillManager(Veinminer plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load skill system configuration
     */
    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("skill-system.enabled", true);
        maxSkillLevel = plugin.getConfig().getInt("skill-system.max-skill-level", 5);

        // Load reset settings
        ConfigurationSection resetSection = plugin.getConfig().getConfigurationSection("reset");
        if (resetSection != null) {
            resetCostEnabled = resetSection.getBoolean("cost-enabled", true);
            resetCost = resetSection.getDouble("cost", 1000.0);
            resetCommand = resetSection.getString("command", "eco take %player% %amount%");
        }

        // Load efficiency boost values
        efficiencyBoost = new HashMap<>();
        ConfigurationSection efficiencySection = plugin.getConfig().getConfigurationSection("efficiency-boost");
        if (efficiencySection != null) {
            for (String key : efficiencySection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key.replace("level-", ""));
                    double value = efficiencySection.getDouble(key);
                    efficiencyBoost.put(level, value);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in efficiency-boost: " + key);
                }
            }
        }

        // Load luck enhancement values
        luckEnhancement = new HashMap<>();
        ConfigurationSection luckSection = plugin.getConfig().getConfigurationSection("luck-enhancement");
        if (luckSection != null) {
            for (String key : luckSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key.replace("level-", ""));
                    double value = luckSection.getDouble(key);
                    luckEnhancement.put(level, value);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in luck-enhancement: " + key);
                }
            }
        }

        // Load energy conservation values
        energyConservation = new HashMap<>();
        ConfigurationSection energySection = plugin.getConfig().getConfigurationSection("energy-conservation");
        if (energySection != null) {
            for (String key : energySection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key.replace("level-", ""));
                    double value = energySection.getDouble(key);
                    energyConservation.put(level, value);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in energy-conservation: " + key);
                }
            }
        }
    }

    /**
     * Check if the skill system is enabled
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the maximum skill level
     * @return The maximum skill level
     */
    public int getMaxSkillLevel() {
        return maxSkillLevel;
    }

    /**
     * Get the efficiency boost for a level
     * @param level The level
     * @return The efficiency boost percentage
     */
    public double getEfficiencyBoost(int level) {
        return efficiencyBoost.getOrDefault(level, 0.0);
    }

    /**
     * Get the luck enhancement for a level
     * @param level The level
     * @return The luck enhancement percentage
     */
    public double getLuckEnhancement(int level) {
        return luckEnhancement.getOrDefault(level, 0.0);
    }

    /**
     * Get the energy conservation for a level
     * @param level The level
     * @return The energy conservation percentage
     */
    public double getEnergyConservation(int level) {
        return energyConservation.getOrDefault(level, 0.0);
    }

    /**
     * Upgrade a skill for a player
     * @param player The player
     * @param skillType The skill type (efficiency, luck, energy)
     * @return True if the skill was upgraded, false otherwise
     */
    public boolean upgradeSkill(Player player, String skillType) {
        if (!enabled) {
            return false;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }

        // Check if player has skill points
        if (playerData.getSkillPoints() <= 0) {
            return false;
        }

        // Get current skill level
        int currentLevel = 0;
        switch (skillType.toLowerCase()) {
            case "efficiency":
                currentLevel = playerData.getEfficiencyLevel();
                break;
            case "luck":
                currentLevel = playerData.getLuckLevel();
                break;
            case "energy":
                currentLevel = playerData.getEnergyLevel();
                break;
            default:
                return false;
        }

        // Check if skill is already at max level
        if (currentLevel >= maxSkillLevel) {
            return false;
        }

        // Upgrade skill
        switch (skillType.toLowerCase()) {
            case "efficiency":
                playerData.setEfficiencyLevel(currentLevel + 1);
                break;
            case "luck":
                playerData.setLuckLevel(currentLevel + 1);
                break;
            case "energy":
                playerData.setEnergyLevel(currentLevel + 1);
                break;
        }

        // Use skill point
        playerData.useSkillPoints(1);

        return true;
    }

    /**
     * Reset all skills for a player
     * @param player The player
     * @return True if skills were reset, false otherwise
     */
    public boolean resetSkills(Player player) {
        if (!enabled) {
            return false;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }

        // Check if player has any skills to reset
        if (playerData.getEfficiencyLevel() == 0 && playerData.getLuckLevel() == 0 && playerData.getEnergyLevel() == 0) {
            return false;
        }

        // Calculate total skill points to refund
        int totalSkillPoints = playerData.getEfficiencyLevel() + playerData.getLuckLevel() + playerData.getEnergyLevel();

        // Reset skills
        playerData.setEfficiencyLevel(0);
        playerData.setLuckLevel(0);
        playerData.setEnergyLevel(0);

        // Refund skill points
        playerData.addSkillPoints(totalSkillPoints);

        return true;
    }

    /**
     * Get the efficiency level for a player
     * @param player The player
     * @return The efficiency level
     */
    public int getEfficiencyLevel(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return 0;
        }
        return playerData.getEfficiencyLevel();
    }

    /**
     * Get the luck level for a player
     * @param player The player
     * @return The luck level
     */
    public int getLuckLevel(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return 0;
        }
        return playerData.getLuckLevel();
    }

    /**
     * Get the energy level for a player
     * @param player The player
     * @return The energy level
     */
    public int getEnergyLevel(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return 0;
        }
        return playerData.getEnergyLevel();
    }
}