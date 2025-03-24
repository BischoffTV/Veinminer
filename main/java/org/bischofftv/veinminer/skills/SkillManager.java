package org.bischofftv.veinminer.skills;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SkillManager {

    private final Veinminer plugin;
    private final Random random = new Random();
    private boolean enabled;
    private int maxSkillLevel;
    private boolean resetCostEnabled;
    private double resetCost;
    private String resetCostCommand;

    // Skill effect values per level
    private final Map<Integer, Double> efficiencyBoostValues = new HashMap<>();
    private final Map<Integer, Double> luckEnhancementValues = new HashMap<>();
    private final Map<Integer, Double> energyConservationValues = new HashMap<>();

    public SkillManager(Veinminer plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection skillSection = plugin.getConfig().getConfigurationSection("skill-system");

        if (skillSection == null) {
            plugin.getLogger().warning("Skill system configuration not found. Using defaults.");
            enabled = true;
            maxSkillLevel = 5;
            resetCostEnabled = true;
            resetCost = 1000.0;
            resetCostCommand = "eco take %player% %amount%";

            // Set default values
            setDefaultSkillValues();
            return;
        }

        enabled = skillSection.getBoolean("enabled", true);
        maxSkillLevel = skillSection.getInt("max-skill-level", 5);

        // Load reset cost settings
        ConfigurationSection resetSection = skillSection.getConfigurationSection("reset");
        if (resetSection != null) {
            resetCostEnabled = resetSection.getBoolean("cost-enabled", true);
            resetCost = resetSection.getDouble("cost", 1000.0);
            resetCostCommand = resetSection.getString("command", "eco take %player% %amount%");
        } else {
            resetCostEnabled = true;
            resetCost = 1000.0;
            resetCostCommand = "eco take %player% %amount%";
        }

        // Load skill values
        loadSkillValues(skillSection);
    }

    private void setDefaultSkillValues() {
        // Efficiency Boost: Chance to reduce durability consumption
        efficiencyBoostValues.put(1, 10.0); // 10% chance
        efficiencyBoostValues.put(2, 20.0); // 20% chance
        efficiencyBoostValues.put(3, 30.0); // 30% chance
        efficiencyBoostValues.put(4, 40.0); // 40% chance
        efficiencyBoostValues.put(5, 50.0); // 50% chance

        // Luck Enhancement: Chance to get additional drops
        luckEnhancementValues.put(1, 5.0);  // 5% chance
        luckEnhancementValues.put(2, 10.0); // 10% chance
        luckEnhancementValues.put(3, 15.0); // 15% chance
        luckEnhancementValues.put(4, 20.0); // 20% chance
        luckEnhancementValues.put(5, 25.0); // 25% chance

        // Energy Conservation: Chance to reduce hunger consumption
        energyConservationValues.put(1, 20.0); // 20% chance
        energyConservationValues.put(2, 35.0); // 35% chance
        energyConservationValues.put(3, 50.0); // 50% chance
        energyConservationValues.put(4, 65.0); // 65% chance
        energyConservationValues.put(5, 80.0); // 80% chance
    }

    private void loadSkillValues(ConfigurationSection skillSection) {
        // Clear existing values
        efficiencyBoostValues.clear();
        luckEnhancementValues.clear();
        energyConservationValues.clear();

        // Load Efficiency Boost values
        ConfigurationSection efficiencySection = skillSection.getConfigurationSection("efficiency-boost");
        if (efficiencySection != null) {
            for (int i = 1; i <= maxSkillLevel; i++) {
                double value = efficiencySection.getDouble("level-" + i, i * 10.0);
                efficiencyBoostValues.put(i, value);
            }
        } else {
            for (int i = 1; i <= maxSkillLevel; i++) {
                efficiencyBoostValues.put(i, i * 10.0);
            }
        }

        // Load Luck Enhancement values
        ConfigurationSection luckSection = skillSection.getConfigurationSection("luck-enhancement");
        if (luckSection != null) {
            for (int i = 1; i <= maxSkillLevel; i++) {
                double value = luckSection.getDouble("level-" + i, i * 5.0);
                luckEnhancementValues.put(i, value);
            }
        } else {
            for (int i = 1; i <= maxSkillLevel; i++) {
                luckEnhancementValues.put(i, i * 5.0);
            }
        }

        // Load Energy Conservation values
        ConfigurationSection energySection = skillSection.getConfigurationSection("energy-conservation");
        if (energySection != null) {
            for (int i = 1; i <= maxSkillLevel; i++) {
                double value = energySection.getDouble("level-" + i, 20.0 + (i - 1) * 15.0);
                energyConservationValues.put(i, value);
            }
        } else {
            for (int i = 1; i <= maxSkillLevel; i++) {
                energyConservationValues.put(i, 20.0 + (i - 1) * 15.0);
            }
        }
    }

    /**
     * Get the available skill points for a player
     * @param player The player
     * @return The number of available skill points
     */
    public int getAvailableSkillPoints(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return 0;
        }

        int level = playerData.getLevel();
        int usedPoints = playerData.getEfficiencyLevel() + playerData.getLuckLevel() + playerData.getEnergyLevel();

        // Each level gives one skill point
        return level - usedPoints;
    }

    /**
     * Upgrade a skill for a player
     * @param player The player
     * @param skillType The skill type to upgrade
     * @return True if the skill was upgraded, false otherwise
     */
    public boolean upgradeSkill(Player player, SkillType skillType) {
        if (!enabled) {
            return false;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }

        // Check if player has available skill points
        if (getAvailableSkillPoints(player) <= 0) {
            return false;
        }

        // Check if skill is already at max level
        int currentLevel = getSkillLevel(playerData, skillType);
        if (currentLevel >= maxSkillLevel) {
            return false;
        }

        // Upgrade the skill
        setSkillLevel(playerData, skillType, currentLevel + 1);

        // Save player data
        plugin.getPlayerDataManager().savePlayerData(player);

        return true;
    }

    // Update the resetSkills method to check if player has enough money
    public boolean resetSkills(Player player) {
        if (!enabled) {
            return false;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }

        // Check if player has any skill points to reset
        if (playerData.getEfficiencyLevel() == 0 && playerData.getLuckLevel() == 0 && playerData.getEnergyLevel() == 0) {
            return false;
        }

        // If reset cost is enabled, check if player has enough money
        if (resetCostEnabled && resetCost > 0) {
            // Check player's money balance before executing command
            double playerBalance = getPlayerBalance(player);

            if (playerBalance < resetCost) {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.not-enough-money",
                        "%cost%", String.valueOf(resetCost),
                        "%balance%", String.valueOf(playerBalance)));
                return false;
            }

            // Execute command to take money
            String command = resetCostCommand
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(resetCost));

            boolean success = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            if (!success) {
                return false;
            }
        }

        // Reset all skills
        playerData.setEfficiencyLevel(0);
        playerData.setLuckLevel(0);
        playerData.setEnergyLevel(0);

        // Save player data
        plugin.getPlayerDataManager().savePlayerData(player);

        return true;
    }

    // Add a method to get player balance using vault or essentials
    private double getPlayerBalance(Player player) {
        // First try to use Vault if available
        try {
            Plugin vaultPlugin = plugin.getServer().getPluginManager().getPlugin("Vault");
            if (vaultPlugin != null && vaultPlugin.isEnabled()) {
                // Try to get Vault's economy service through reflection to avoid direct dependency
                Class<?> vaultClass = Class.forName("net.milkbowl.vault.economy.Economy");
                Object economy = plugin.getServer().getServicesManager()
                        .getRegistration(vaultClass).getProvider();

                if (economy != null) {
                    // Use reflection to call getBalance method
                    Method getBalanceMethod = economy.getClass().getMethod("getBalance", String.class);
                    Object result = getBalanceMethod.invoke(economy, player.getName());
                    if (result instanceof Number) {
                        return ((Number) result).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player balance via Vault: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        // If Vault failed, try Essentials
        try {
            Plugin essentialsPlugin = plugin.getServer().getPluginManager().getPlugin("Essentials");
            if (essentialsPlugin != null && essentialsPlugin.isEnabled()) {
                // Use reflection to get Essentials user and money
                Class<?> essentialsClass = Class.forName("com.earth2me.essentials.Essentials");
                Method getPluginMethod = essentialsClass.getMethod("getPlugin");
                Object essentialsInstance = getPluginMethod.invoke(null);

                Method getUserMethod = essentialsClass.getMethod("getUser", String.class);
                Object userInstance = getUserMethod.invoke(essentialsInstance, player.getName());

                Class<?> userClass = Class.forName("com.earth2me.essentials.User");
                Method getMoneyMethod = userClass.getMethod("getMoney");
                Object moneyObj = getMoneyMethod.invoke(userInstance);

                if (moneyObj instanceof Number) {
                    return ((Number) moneyObj).doubleValue();
                } else if (moneyObj != null) {
                    // Try to parse as double if it's not directly a number
                    return Double.parseDouble(moneyObj.toString());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player balance via Essentials: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        // If all else fails, assume player has enough money
        plugin.getLogger().warning("Could not determine player balance, assuming sufficient funds for skill reset");
        return Double.MAX_VALUE;
    }

    /**
     * Check if a player should get an efficiency boost
     * @param player The player
     * @return True if the player gets an efficiency boost, false otherwise
     */
    public boolean shouldGetEfficiencyBoost(Player player) {
        if (!enabled) {
            return false;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }

        int level = playerData.getEfficiencyLevel();
        if (level <= 0) {
            return false;
        }

        double chance = efficiencyBoostValues.getOrDefault(level, 0.0);
        return random.nextDouble() * 100 < chance;
    }

    /**
     * Check if a player should get a luck enhancement
     * @param player The player
     * @return True if the player gets a luck enhancement, false otherwise
     */
    public boolean shouldGetLuckEnhancement(Player player) {
        if (!enabled) {
            return false;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }

        int level = playerData.getLuckLevel();
        if (level <= 0) {
            return false;
        }

        double chance = luckEnhancementValues.getOrDefault(level, 0.0);
        return random.nextDouble() * 100 < chance;
    }

    /**
     * Check if a player should get an energy conservation
     * @param player The player
     * @return True if the player gets an energy conservation, false otherwise
     */
    public boolean shouldGetEnergyConservation(Player player) {
        if (!enabled) {
            return false;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }

        int level = playerData.getEnergyLevel();
        if (level <= 0) {
            return false;
        }

        double chance = energyConservationValues.getOrDefault(level, 0.0);
        return random.nextDouble() * 100 < chance;
    }

    /**
     * Get the skill level for a player
     * @param playerData The player data
     * @param skillType The skill type
     * @return The skill level
     */
    private int getSkillLevel(PlayerData playerData, SkillType skillType) {
        switch (skillType) {
            case EFFICIENCY:
                return playerData.getEfficiencyLevel();
            case LUCK:
                return playerData.getLuckLevel();
            case ENERGY:
                return playerData.getEnergyLevel();
            default:
                return 0;
        }
    }

    /**
     * Set the skill level for a player
     * @param playerData The player data
     * @param skillType The skill type
     * @param level The new level
     */
    private void setSkillLevel(PlayerData playerData, SkillType skillType, int level) {
        switch (skillType) {
            case EFFICIENCY:
                playerData.setEfficiencyLevel(level);
                break;
            case LUCK:
                playerData.setLuckLevel(level);
                break;
            case ENERGY:
                playerData.setEnergyLevel(level);
                break;
        }
    }

    /**
     * Get the skill effect value for a player
     * @param player The player
     * @param skillType The skill type
     * @return The skill effect value
     */
    public double getSkillEffectValue(Player player, SkillType skillType) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return 0.0;
        }

        int level = getSkillLevel(playerData, skillType);
        if (level <= 0) {
            return 0.0;
        }

        switch (skillType) {
            case EFFICIENCY:
                return efficiencyBoostValues.getOrDefault(level, 0.0);
            case LUCK:
                return luckEnhancementValues.getOrDefault(level, 0.0);
            case ENERGY:
                return energyConservationValues.getOrDefault(level, 0.0);
            default:
                return 0.0;
        }
    }

    /**
     * Get the maximum skill level
     * @return The maximum skill level
     */
    public int getMaxSkillLevel() {
        return maxSkillLevel;
    }

    /**
     * Check if the skill system is enabled
     * @return True if the skill system is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the reset cost
     * @return The reset cost
     */
    public double getResetCost() {
        return resetCost;
    }

    /**
     * Check if reset cost is enabled
     * @return True if reset cost is enabled, false otherwise
     */
    public boolean isResetCostEnabled() {
        return resetCostEnabled;
    }

    /**
     * Enum for skill types
     */
    public enum SkillType {
        EFFICIENCY,
        LUCK,
        ENERGY
    }
}