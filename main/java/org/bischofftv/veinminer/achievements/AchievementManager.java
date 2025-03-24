package org.bischofftv.veinminer.achievements;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AchievementManager {

    private final Veinminer plugin;
    private boolean enabled;
    private boolean economyEnabled;
    private String giveMoneyCommand;

    // Achievement definitions from config
    private final Map<String, Map<String, Object>> achievementDefinitions;

    // Player achievement data (UUID -> Achievement ID -> Progress)
    private final Map<UUID, Map<String, Integer>> playerAchievements;

    private final Map<UUID, Set<String>> claimedAchievements = new HashMap<>();

    public AchievementManager(Veinminer plugin) {
        this.plugin = plugin;
        this.achievementDefinitions = new HashMap<>();
        this.playerAchievements = new HashMap<>();
        loadConfig();
    }

    /**
     * Load achievement system configuration
     */
    public void loadConfig() {
        loadSettings();
    }

    // Update loadSettings method to properly load achievement icons from config
    public void loadSettings() {
        ConfigurationSection achievementSection = plugin.getConfig().getConfigurationSection("achievement-system");

        if (achievementSection == null) {
            plugin.getLogger().warning("Achievement system configuration not found. Using defaults.");
            enabled = true;
            economyEnabled = true;
            giveMoneyCommand = "eco give %player% %amount%";
        } else {
            enabled = achievementSection.getBoolean("enabled", true);
            plugin.getLogger().info("Achievement system is " + (enabled ? "enabled" : "disabled"));
        }

        // Load economy settings
        ConfigurationSection economySection = plugin.getConfig().getConfigurationSection("economy");
        if (economySection != null) {
            economyEnabled = economySection.getBoolean("enabled", true);
            giveMoneyCommand = economySection.getString("give-command", "eco give %player% %amount%");
        } else {
            economyEnabled = true;
            giveMoneyCommand = "eco give %player% %amount%";
        }

        // Load achievement definitions
        achievementDefinitions.clear();
        ConfigurationSection achievementsSection = plugin.getConfig().getConfigurationSection("achievements");
        if (achievementsSection != null) {
            plugin.getLogger().info("Loading achievements from config.yml...");
            for (String achievementId : achievementsSection.getKeys(false)) {
                ConfigurationSection achievementConfig = achievementsSection.getConfigurationSection(achievementId);
                if (achievementConfig != null) {
                    Map<String, Object> achievementData = new HashMap<>();

                    // Basic achievement data
                    achievementData.put("name", achievementConfig.getString("name", "Unknown Achievement"));
                    achievementData.put("description", achievementConfig.getString("description", "No description"));
                    achievementData.put("type", achievementConfig.getString("type", "UNKNOWN"));
                    achievementData.put("amount", achievementConfig.getInt("amount", 1));

                    // Type-specific data
                    if (achievementConfig.getString("type", "").equals("BLOCK_MINE")) {
                        achievementData.put("block", achievementConfig.getString("block", "STONE"));
                    }

                    // Icon setting
                    if (achievementConfig.contains("icon")) {
                        achievementData.put("icon", achievementConfig.getString("icon"));
                    }

                    // Rewards
                    ConfigurationSection rewardsSection = achievementConfig.getConfigurationSection("rewards");
                    if (rewardsSection != null) {
                        Map<String, Object> rewards = new HashMap<>();

                        if (rewardsSection.contains("money")) {
                            rewards.put("money", rewardsSection.getDouble("money"));
                        }

                        if (rewardsSection.contains("items")) {
                            rewards.put("items", rewardsSection.getStringList("items"));
                        }

                        achievementData.put("rewards", rewards);
                    }

                    achievementDefinitions.put(achievementId, achievementData);
                    plugin.getLogger().info("Loaded achievement: " + achievementId + " - " + achievementData.get("name"));
                }
            }
        }

        plugin.getLogger().info("Loaded " + achievementDefinitions.size() + " achievement definitions.");

        // Update database schema if needed - this ensures new achievements will be properly tracked
        plugin.getDatabaseManager().updateAchievementSchema();
    }

    /**
     * Clear all player achievements from memory (not from database)
     */
    public void clearPlayerAchievements() {
        playerAchievements.clear();
    }

    /**
     * Check if the achievement system is enabled
     * @return True if the achievement system is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get player achievements
     * @param player The player
     * @return A map of achievement IDs to progress
     */
    public Map<String, Integer> getPlayerAchievements(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Check if player achievements are already loaded
        if (!playerAchievements.containsKey(playerUUID)) {
            loadPlayerAchievements(player);
        }

        return playerAchievements.getOrDefault(playerUUID, new HashMap<>());
    }

    /**
     * Check if an achievement has been claimed by a player
     * @param player The player
     * @param achievementId The achievement ID
     * @return True if the achievement has been claimed, false otherwise
     */
    public boolean hasClaimedAchievement(Player player, String achievementId) {
        UUID playerUUID = player.getUniqueId();
        if (!claimedAchievements.containsKey(playerUUID)) {
            claimedAchievements.put(playerUUID, new HashSet<>());
        }
        return claimedAchievements.get(playerUUID).contains(achievementId);
    }

    /**
     * Mark an achievement as claimed by a player
     * @param player The player
     * @param achievementId The achievement ID
     */
    public void markAchievementClaimed(Player player, String achievementId) {
        UUID playerUUID = player.getUniqueId();
        if (!claimedAchievements.containsKey(playerUUID)) {
            claimedAchievements.put(playerUUID, new HashSet<>());
        }
        claimedAchievements.get(playerUUID).add(achievementId);

        // Save to database
        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            if (connection != null) {
                String sql = "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + "achievements " +
                        "SET claimed = ? WHERE uuid = ? AND achievement_id = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setBoolean(1, true);
                statement.setString(2, playerUUID.toString());
                statement.setString(3, achievementId);
                statement.executeUpdate();
                statement.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save claimed achievement: " + e.getMessage());
        }
    }

    /**
     * Load claimed achievements for a player
     * @param player The player
     */
    public void loadClaimedAchievements(Player player) {
        UUID playerUUID = player.getUniqueId();
        Set<String> claimed = new HashSet<>();

        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            if (connection != null) {
                String sql = "SELECT achievement_id FROM " + plugin.getDatabaseManager().getTablePrefix() +
                        "achievements WHERE uuid = ? AND claimed = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, playerUUID.toString());
                statement.setBoolean(2, true);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    claimed.add(resultSet.getString("achievement_id"));
                }

                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load claimed achievements: " + e.getMessage());
        }

        claimedAchievements.put(playerUUID, claimed);
    }

    /**
     * Load player achievements from the database
     * @param player The player
     */
    public void loadPlayerAchievements(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Create empty achievement map
        Map<String, Integer> achievements = new HashMap<>();

        // Try to load from database
        try {
            Map<String, Integer> dbAchievements = plugin.getDatabaseManager().loadPlayerAchievements(player);
            achievements.putAll(dbAchievements);
            plugin.getLogger().info("Loaded " + dbAchievements.size() + " achievements for player " + player.getName());

            // Also load claimed achievements
            loadClaimedAchievements(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load achievements for " + player.getName() + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        // Store player achievements
        playerAchievements.put(playerUUID, achievements);
    }

    /**
     * Save player achievements to the database
     * @param player The player
     */
    public void savePlayerAchievements(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Check if player achievements are loaded
        if (!playerAchievements.containsKey(playerUUID)) {
            return;
        }

        Map<String, Integer> achievements = playerAchievements.get(playerUUID);

        // Try to save to database
        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            if (connection != null) {
                for (Map.Entry<String, Integer> entry : achievements.entrySet()) {
                    String achievementId = entry.getKey();
                    int progress = entry.getValue();

                    String sql = "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + "achievements " +
                            "(uuid, achievement_id, progress, completed) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "progress = ?, completed = ?";

                    PreparedStatement statement = connection.prepareStatement(sql);

                    // Get required amount for completion
                    int requiredAmount = 1;
                    if (achievementDefinitions.containsKey(achievementId)) {
                        requiredAmount = (int) achievementDefinitions.get(achievementId).get("amount");
                    }

                    boolean completed = progress >= requiredAmount;

                    // Insert values
                    statement.setString(1, playerUUID.toString());
                    statement.setString(2, achievementId);
                    statement.setInt(3, progress);
                    statement.setBoolean(4, completed);

                    // Update values
                    statement.setInt(5, progress);
                    statement.setBoolean(6, completed);

                    statement.executeUpdate();
                    statement.close();

                    // Notify other servers about the update
                    plugin.getDatabaseManager().notifyAchievementUpdate(player, achievementId, progress);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save achievements for " + player.getName() + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Save all player achievements
     */
    public void saveAllAchievements() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            savePlayerAchievements(player);
        }
    }

    /**
     * Update block mine progress for a player
     * @param player The player
     * @param blockType The block type
     * @param amount The amount to add
     */
    public void updateBlockMineProgress(Player player, Material blockType, int amount) {
        if (!enabled) {
            return;
        }

        UUID playerUUID = player.getUniqueId();

        // Check if player achievements are loaded
        if (!playerAchievements.containsKey(playerUUID)) {
            loadPlayerAchievements(player);
        }

        Map<String, Integer> achievements = playerAchievements.get(playerUUID);

        // Find matching achievements
        for (Map.Entry<String, Map<String, Object>> entry : achievementDefinitions.entrySet()) {
            String achievementId = entry.getKey();
            Map<String, Object> achievementData = entry.getValue();

            // Check if this is a block mine achievement
            if (achievementData.get("type").equals("BLOCK_MINE")) {
                String blockString = (String) achievementData.get("block");
                String[] blocks = blockString.split(",");

                // Check if the block type matches
                for (String block : blocks) {
                    if (blockType.name().equals(block.trim())) {
                        // Update progress
                        int currentProgress = achievements.getOrDefault(achievementId, 0);
                        int requiredAmount = (int) achievementData.get("amount");
                        // Only increment if not already completed
                        if (currentProgress < requiredAmount) {
                            int newProgress = Math.min(currentProgress + amount, requiredAmount);
                            achievements.put(achievementId, newProgress);

                            // Check if achievement is completed
                            if (currentProgress < requiredAmount && newProgress >= requiredAmount) {
                                // Achievement completed
                                completeAchievement(player, achievementId, achievementData);
                            }
                        } else {
                            // Already completed, no need to update progress
                            achievements.put(achievementId, requiredAmount);
                        }

                        break;
                    }
                }
            }
        }
    }

    /**
     * Update total blocks progress for a player
     * @param player The player
     * @param amount The amount to add
     */
    public void updateTotalBlocksProgress(Player player, int amount) {
        if (!enabled) {
            return;
        }

        UUID playerUUID = player.getUniqueId();

        // Check if player achievements are loaded
        if (!playerAchievements.containsKey(playerUUID)) {
            loadPlayerAchievements(player);
        }

        Map<String, Integer> achievements = playerAchievements.get(playerUUID);

        // Find matching achievements
        for (Map.Entry<String, Map<String, Object>> entry : achievementDefinitions.entrySet()) {
            String achievementId = entry.getKey();
            Map<String, Object> achievementData = entry.getValue();

            // Check if this is a total blocks achievement
            if (achievementData.get("type").equals("TOTAL_BLOCKS")) {
                // Update progress
                int currentProgress = achievements.getOrDefault(achievementId, 0);
                int requiredAmount = (int) achievementData.get("amount");
                // Only increment if not already completed
                if (currentProgress < requiredAmount) {
                    int newProgress = Math.min(currentProgress + amount, requiredAmount);
                    achievements.put(achievementId, newProgress);

                    // Check if achievement is completed
                    if (currentProgress < requiredAmount && newProgress >= requiredAmount) {
                        // Achievement completed
                        completeAchievement(player, achievementId, achievementData);
                    }
                } else {
                    // Already completed, no need to update progress
                    achievements.put(achievementId, requiredAmount);
                }
            }
        }
    }

    /**
     * Update level progress for a player
     * @param player The player
     * @param level The player's level
     */
    public void updateLevelProgress(Player player, int level) {
        if (!enabled) {
            return;
        }

        UUID playerUUID = player.getUniqueId();

        // Check if player achievements are loaded
        if (!playerAchievements.containsKey(playerUUID)) {
            loadPlayerAchievements(player);
        }

        Map<String, Integer> achievements = playerAchievements.get(playerUUID);

        // Find matching achievements
        for (Map.Entry<String, Map<String, Object>> entry : achievementDefinitions.entrySet()) {
            String achievementId = entry.getKey();
            Map<String, Object> achievementData = entry.getValue();

            // Check if this is a level achievement
            if (achievementData.get("type").equals("LEVEL")) {
                int requiredLevel = (int) achievementData.get("amount");

                // Check if the player has reached the required level
                if (level >= requiredLevel) {
                    // Update progress (set to required amount)
                    int currentProgress = achievements.getOrDefault(achievementId, 0);
                    if (currentProgress < requiredLevel) {
                        achievements.put(achievementId, requiredLevel);

                        // Achievement completed
                        completeAchievement(player, achievementId, achievementData);
                    }
                }
            }
        }
    }

    /**
     * Update skill master progress for a player
     * @param player The player
     */
    public void updateSkillMasterProgress(Player player) {
        if (!enabled || !plugin.getSkillManager().isEnabled()) {
            return;
        }

        UUID playerUUID = player.getUniqueId();

        // Check if player achievements are loaded
        if (!playerAchievements.containsKey(playerUUID)) {
            loadPlayerAchievements(player);
        }

        Map<String, Integer> achievements = playerAchievements.get(playerUUID);

        // Get player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        // Check if all skills are maxed out
        int maxSkillLevel = plugin.getSkillManager().getMaxSkillLevel();
        if (playerData.getEfficiencyLevel() >= maxSkillLevel &&
                playerData.getLuckLevel() >= maxSkillLevel &&
                playerData.getEnergyLevel() >= maxSkillLevel) {

            // Find skill master achievements
            for (Map.Entry<String, Map<String, Object>> entry : achievementDefinitions.entrySet()) {
                String achievementId = entry.getKey();
                Map<String, Object> achievementData = entry.getValue();

                // Check if this is a skill master achievement
                if (achievementData.get("type").equals("SKILL_MASTER")) {
                    // Update progress (set to 1)
                    int currentProgress = achievements.getOrDefault(achievementId, 0);
                    if (currentProgress < 1) {
                        achievements.put(achievementId, 1);

                        // Achievement completed
                        completeAchievement(player, achievementId, achievementData);
                    }
                }
            }
        }
    }

    /**
     * Complete an achievement for a player
     * @param player The player
     * @param achievementId The achievement ID
     * @param achievementData The achievement data
     */
    private void completeAchievement(Player player, String achievementId, Map<String, Object> achievementData) {
        String name = (String) achievementData.get("name");

        // Send achievement completion message
        String message = plugin.getMessageManager().formatMessage("messages.achievements.unlocked")
                .replace("%achievement%", name);
        player.sendMessage(message);

        // Don't give rewards automatically - player must claim them
    }

    /**
     * Claim rewards for a completed achievement
     * @param player The player
     * @param achievementId The achievement ID
     * @return True if rewards were claimed, false otherwise
     */
    public boolean claimAchievementRewards(Player player, String achievementId) {
        UUID playerUUID = player.getUniqueId();

        // Check if player has completed the achievement
        if (!playerAchievements.containsKey(playerUUID)) {
            return false;
        }

        Map<String, Integer> achievements = playerAchievements.get(playerUUID);
        if (!achievements.containsKey(achievementId)) {
            return false;
        }

        // Check if the achievement is completed
        Map<String, Object> achievementData = achievementDefinitions.get(achievementId);
        if (achievementData == null) {
            return false;
        }

        int progress = achievements.get(achievementId);
        int requiredAmount = (int) achievementData.get("amount");

        if (progress < requiredAmount) {
            return false;
        }

        // Check if already claimed
        if (hasClaimedAchievement(player, achievementId)) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.achievements.already-claimed"));
            return false;
        }

        // Give rewards
        if (achievementData.containsKey("rewards")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rewards = (Map<String, Object>) achievementData.get("rewards");

            // Money reward
            if (economyEnabled && rewards.containsKey("money")) {
                double money = (double) rewards.get("money");

                // Execute command to give money
                String command = giveMoneyCommand
                        .replace("%player%", player.getName())
                        .replace("%amount%", String.valueOf(money));

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

                // Send reward message
                String rewardMessage = plugin.getMessageManager().formatMessage("messages.achievements.reward")
                        .replace("%reward%", money + " money");
                player.sendMessage(rewardMessage);
            }

            // Item rewards
            if (rewards.containsKey("items")) {
                @SuppressWarnings("unchecked")
                List<String> items = (List<String>) rewards.get("items");

                for (String item : items) {
                    String[] parts = item.split(":");
                    String itemName = parts[0];
                    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                    try {
                        Material material = Material.valueOf(itemName);
                        ItemStack itemStack = new ItemStack(material, amount);

                        // Add to player's inventory or drop on ground if inventory is full
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
                        for (ItemStack leftoverItem : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
                        }

                        // Send reward message
                        String rewardMessage = plugin.getMessageManager().formatMessage("messages.achievements.reward")
                                .replace("%reward%", amount + "x " + formatItemName(itemName));
                        player.sendMessage(rewardMessage);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid item in achievement reward: " + itemName);
                    }
                }
            }
        }

        // Mark as claimed
        markAchievementClaimed(player, achievementId);

        // Send claimed message
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.achievements.claimed"));

        return true;
    }

    /**
     * Format an item name to be more readable
     * @param itemName The item name
     * @return The formatted item name
     */
    private String formatItemName(String itemName) {
        String[] words = itemName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    // Add a method to reload achievements
    public void reloadAchievements() {
        // Clear current achievement definitions
        achievementDefinitions.clear();

        // Clear player achievements from memory to force reload from database
        playerAchievements.clear();
        claimedAchievements.clear();

        // Reload from config
        loadSettings();

        // Update database schema to add new achievements
        plugin.getDatabaseManager().updateAchievementSchema();

        // Reload achievement data for online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            loadPlayerAchievements(player);
        }

        plugin.getLogger().info("Achievements reloaded. Total achievements: " + achievementDefinitions.size());
    }

    // Add a method to get the icon material for an achievement
    public Material getAchievementIcon(String achievementId) {
        if (achievementDefinitions.containsKey(achievementId)) {
            Map<String, Object> achievementData = achievementDefinitions.get(achievementId);
            if (achievementData.containsKey("icon")) {
                String iconName = (String) achievementData.get("icon");
                try {
                    return Material.valueOf(iconName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material for achievement icon: " + iconName);
                }
            }

            // If no specific icon or invalid, use type-based icon
            String type = (String) achievementData.get("type");
            if (type != null) {
                return plugin.getAchievementGUI().getTypeIcon(type);
            }
        }

        // Default fallback
        return Material.PAPER;
    }

    /**
     * Get achievement definitions
     * @return The achievement definitions
     */
    public Map<String, Map<String, Object>> getAchievementDefinitions() {
        return achievementDefinitions;
    }
}