package org.bischofftv.veinminer.achievements;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AchievementManager {

    private final Veinminer plugin;
    private boolean enabled;
    private Map<String, Map<String, Object>> achievementDefinitions;
    private Map<UUID, Map<String, Integer>> playerAchievements;
    private Map<UUID, Map<String, Boolean>> claimedRewards;
    private Map<String, Material> customIcons;
    private boolean economyEnabled;
    private String giveCommand;

    // Achievement notification settings
    private boolean notifyInChat;
    private boolean notifyWithSound;
    private Sound notificationSound;
    private float soundVolume;
    private float soundPitch;
    private boolean notifyWithTitle;
    private String titleText;
    private String subtitleText;
    private int fadeIn;
    private int stay;
    private int fadeOut;

    // Discord webhook settings
    private boolean discordEnabled;
    private String discordWebhookUrl;
    private String discordMessage;

    // Achievement cache for performance
    private Map<String, List<String>> achievementsByType;
    private Map<String, List<String>> achievementsByBlock;

    public AchievementManager(Veinminer plugin) {
        this.plugin = plugin;
        this.achievementDefinitions = new HashMap<>();
        this.playerAchievements = new ConcurrentHashMap<>();
        this.claimedRewards = new ConcurrentHashMap<>();
        this.customIcons = new HashMap<>();
        this.achievementsByType = new HashMap<>();
        this.achievementsByBlock = new HashMap<>();
        loadConfig();
    }

    /**
     * Load achievement system configuration
     */
    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("achievement-system.enabled", true);

        // Load custom icons
        customIcons.clear();
        ConfigurationSection iconsSection = plugin.getConfig().getConfigurationSection("icons");
        if (iconsSection != null) {
            for (String key : iconsSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(iconsSection.getString(key));
                    customIcons.put(key, material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in icons: " + iconsSection.getString(key));
                }
            }
        }

        // Load economy settings
        ConfigurationSection economySection = plugin.getConfig().getConfigurationSection("economy");
        if (economySection != null) {
            economyEnabled = economySection.getBoolean("enabled", true);
            giveCommand = economySection.getString("give-command", "eco give %player% %amount%");
        }

        // Load notification settings
        ConfigurationSection notificationSection = plugin.getConfig().getConfigurationSection("achievement-system.notifications");
        if (notificationSection != null) {
            notifyInChat = notificationSection.getBoolean("chat", true);

            // Sound settings
            notifyWithSound = notificationSection.getBoolean("sound.enabled", true);
            try {
                notificationSound = Sound.valueOf(notificationSection.getString("sound.type", "ENTITY_PLAYER_LEVELUP"));
            } catch (IllegalArgumentException e) {
                notificationSound = Sound.ENTITY_PLAYER_LEVELUP;
                plugin.getLogger().warning("Invalid sound in achievement notifications: " + notificationSection.getString("sound.type"));
            }
            soundVolume = (float) notificationSection.getDouble("sound.volume", 1.0);
            soundPitch = (float) notificationSection.getDouble("sound.pitch", 1.0);

            // Title settings
            notifyWithTitle = notificationSection.getBoolean("title.enabled", true);
            titleText = notificationSection.getString("title.text", "&6&lACHIEVEMENT UNLOCKED!");
            subtitleText = notificationSection.getString("title.subtitle", "&e%achievement%");
            fadeIn = notificationSection.getInt("title.fade-in", 10);
            stay = notificationSection.getInt("title.stay", 70);
            fadeOut = notificationSection.getInt("title.fade-out", 20);
        }

        // Load Discord webhook settings
        discordEnabled = plugin.getConfig().getBoolean("logging.enable-discord-logging", false);
        discordWebhookUrl = plugin.getConfig().getString("logging.discord-webhook-url", "");
        discordMessage = plugin.getConfig().getString("logging.discord-achievement-message",
                ":trophy: **%player%** has unlocked the achievement **%achievement%**!");

        // Load achievement definitions
        achievementDefinitions.clear();
        achievementsByType.clear();
        achievementsByBlock.clear();

        ConfigurationSection achievementsSection = plugin.getConfig().getConfigurationSection("achievements");
        if (achievementsSection != null) {
            for (String achievementId : achievementsSection.getKeys(false)) {
                ConfigurationSection achievementSection = achievementsSection.getConfigurationSection(achievementId);
                if (achievementSection != null) {
                    Map<String, Object> achievement = new HashMap<>();
                    achievement.put("id", achievementId);
                    achievement.put("name", achievementSection.getString("name", "Unknown Achievement"));
                    achievement.put("description", achievementSection.getString("description", "No description"));

                    String type = achievementSection.getString("type", "UNKNOWN");
                    achievement.put("type", type);

                    // Add to type cache
                    achievementsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(achievementId);

                    // Handle block types for BLOCK_MINE achievements
                    String blockTypes = achievementSection.getString("block", "");
                    achievement.put("block", blockTypes);

                    if (type.equals("BLOCK_MINE") && !blockTypes.isEmpty()) {
                        for (String blockType : blockTypes.split(",")) {
                            achievementsByBlock.computeIfAbsent(blockType, k -> new ArrayList<>()).add(achievementId);
                        }
                    }

                    achievement.put("amount", achievementSection.getInt("amount", 1));

                    // Load rewards
                    ConfigurationSection rewardsSection = achievementSection.getConfigurationSection("rewards");
                    if (rewardsSection != null) {
                        Map<String, Object> rewards = new HashMap<>();
                        rewards.put("money", rewardsSection.getDouble("money", 0));
                        rewards.put("items", rewardsSection.getStringList("items"));
                        rewards.put("commands", rewardsSection.getStringList("commands"));
                        achievement.put("rewards", rewards);
                    }

                    achievementDefinitions.put(achievementId, achievement);
                }
            }
        }

        // Debug log
        if (plugin.isDebugMode()) {
            plugin.debug("Loaded " + achievementDefinitions.size() + " achievement definitions");
            plugin.debug("Achievement types: " + achievementsByType.keySet());
        }
    }

    /**
     * Check if the achievement system is enabled
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the achievement definitions
     * @return The achievement definitions
     */
    public Map<String, Map<String, Object>> getAchievementDefinitions() {
        return achievementDefinitions;
    }

    /**
     * Load player achievements from the database
     * @param player The player
     */
    public void loadPlayerAchievements(Player player) {
        UUID uuid = player.getUniqueId();

        // Create empty maps for this player
        Map<String, Integer> achievements = new HashMap<>();
        Map<String, Boolean> claimed = new HashMap<>();

        // Try to load from database
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().checkConnection()) {
            try {
                Connection connection = plugin.getDatabaseManager().getConnection();
                String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "achievements WHERE uuid = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, uuid.toString());

                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String achievementId = resultSet.getString("achievement_id");
                    int progress = resultSet.getInt("progress");
                    boolean completed = resultSet.getBoolean("completed");
                    boolean rewardClaimed = resultSet.getBoolean("reward_claimed");

                    achievements.put(achievementId, progress);
                    claimed.put(achievementId, rewardClaimed);
                }

                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load achievements for " + player.getName() + ": " + e.getMessage());
            }
        }

        // Add to maps
        playerAchievements.put(uuid, achievements);
        claimedRewards.put(uuid, claimed);

        // Check for any new achievements that might have been added
        for (String achievementId : achievementDefinitions.keySet()) {
            if (!achievements.containsKey(achievementId)) {
                achievements.put(achievementId, 0);
                claimed.put(achievementId, false);
            }
        }

        // Debug log
        if (plugin.isDebugMode()) {
            plugin.debug("Loaded " + achievements.size() + " achievements for " + player.getName());
        }
    }

    /**
     * Save player achievements to the database
     * @param uuid The player UUID
     */
    public void savePlayerAchievements(UUID uuid) {
        Map<String, Integer> achievements = playerAchievements.get(uuid);
        Map<String, Boolean> claimed = claimedRewards.get(uuid);

        if (achievements == null || claimed == null) {
            return;
        }

        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().checkConnection()) {
            try {
                Connection connection = plugin.getDatabaseManager().getConnection();

                // First delete existing achievements for this player
                String deleteSql = "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + "achievements WHERE uuid = ?";
                PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                deleteStatement.setString(1, uuid.toString());
                deleteStatement.executeUpdate();
                deleteStatement.close();

                // Then insert new achievements
                String insertSql = "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() +
                        "achievements (uuid, achievement_id, progress, completed, reward_claimed) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement insertStatement = connection.prepareStatement(insertSql);

                for (Map.Entry<String, Integer> entry : achievements.entrySet()) {
                    String achievementId = entry.getKey();
                    int progress = entry.getValue();

                    // Check if achievement is completed
                    boolean completed = false;
                    if (achievementDefinitions.containsKey(achievementId)) {
                        int requiredAmount = (int) achievementDefinitions.get(achievementId).get("amount");
                        completed = progress >= requiredAmount;
                    }

                    // Check if reward is claimed
                    boolean rewardClaimed = claimed.getOrDefault(achievementId, false);

                    insertStatement.setString(1, uuid.toString());
                    insertStatement.setString(2, achievementId);
                    insertStatement.setInt(3, progress);
                    insertStatement.setBoolean(4, completed);
                    insertStatement.setBoolean(5, rewardClaimed);
                    insertStatement.addBatch();
                }

                insertStatement.executeBatch();
                insertStatement.close();

                // Debug log
                if (plugin.isDebugMode()) {
                    plugin.debug("Saved achievements for player " + uuid);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save achievements for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Save all player achievements to the database
     */
    public void saveAllAchievements() {
        for (UUID uuid : playerAchievements.keySet()) {
            savePlayerAchievements(uuid);
        }
    }

    /**
     * Get player achievements
     * @param player The player
     * @return The player achievements
     */
    public Map<String, Integer> getPlayerAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        return playerAchievements.getOrDefault(uuid, new HashMap<>());
    }

    /**
     * Get player claimed rewards
     * @param player The player
     * @return The player claimed rewards
     */
    public Map<String, Boolean> getPlayerClaimedRewards(Player player) {
        UUID uuid = player.getUniqueId();
        return claimedRewards.getOrDefault(uuid, new HashMap<>());
    }

    /**
     * Update achievement progress for a player
     * @param player The player
     * @param achievementId The achievement ID
     * @param progress The progress to add
     */
    public void updateAchievementProgress(Player player, String achievementId, int progress) {
        if (!enabled || !achievementDefinitions.containsKey(achievementId)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Integer> achievements = playerAchievements.getOrDefault(uuid, new HashMap<>());
        int currentProgress = achievements.getOrDefault(achievementId, 0);
        int newProgress = currentProgress + progress;
        achievements.put(achievementId, newProgress);
        playerAchievements.put(uuid, achievements);

        // Check if achievement is completed
        int requiredAmount = (int) achievementDefinitions.get(achievementId).get("amount");
        if (currentProgress < requiredAmount && newProgress >= requiredAmount) {
            // Achievement newly completed
            String achievementName = (String) achievementDefinitions.get(achievementId).get("name");

            // Notify player
            if (notifyInChat) {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.achievements.unlocked", "%achievement%", achievementName));
            }

            // Play sound
            if (notifyWithSound) {
                player.playSound(player.getLocation(), notificationSound, soundVolume, soundPitch);
            }

            // Show title
            if (notifyWithTitle) {
                String title = titleText.replace("%achievement%", achievementName);
                String subtitle = subtitleText.replace("%achievement%", achievementName);

                player.sendTitle(
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', title),
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', subtitle),
                        fadeIn,
                        stay,
                        fadeOut
                );
            }

            // Send to Discord webhook
            if (discordEnabled && !discordWebhookUrl.isEmpty()) {
                String message = discordMessage
                        .replace("%player%", player.getName())
                        .replace("%achievement%", achievementName);

                // Send asynchronously
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        sendDiscordWebhook(discordWebhookUrl, message);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
                    }
                });
            }

            // Check for SKILL_MASTER achievement
            if (achievementId.equals("skill_master")) {
                // This is a special achievement that requires all skills to be maxed out
                checkSkillMasterAchievement(player);
            }
        }

        // Notify achievement update
        notifyAchievementUpdate(uuid, achievementId, newProgress);
    }

    /**
     * Update achievement progress for a block mine
     * @param player The player
     * @param blockType The block type
     * @param amount The amount mined
     */
    public void updateBlockMineAchievements(Player player, String blockType, int amount) {
        if (!enabled) {
            return;
        }

        // Update specific block achievements
        List<String> blockAchievements = achievementsByBlock.getOrDefault(blockType, new ArrayList<>());
        for (String achievementId : blockAchievements) {
            updateAchievementProgress(player, achievementId, amount);
        }

        // Update total blocks mined achievements
        List<String> totalBlocksAchievements = achievementsByType.getOrDefault("TOTAL_BLOCKS", new ArrayList<>());
        for (String achievementId : totalBlocksAchievements) {
            updateAchievementProgress(player, achievementId, amount);
        }
    }

    /**
     * Update level-based achievements
     * @param player The player
     * @param level The new level
     */
    public void updateLevelAchievements(Player player, int level) {
        if (!enabled) {
            return;
        }

        List<String> levelAchievements = achievementsByType.getOrDefault("LEVEL", new ArrayList<>());
        for (String achievementId : levelAchievements) {
            Map<String, Object> achievement = achievementDefinitions.get(achievementId);
            int requiredLevel = (int) achievement.get("amount");

            if (level >= requiredLevel) {
                // Set progress to 1 (completed) if the player has reached the required level
                UUID uuid = player.getUniqueId();
                Map<String, Integer> achievements = playerAchievements.getOrDefault(uuid, new HashMap<>());
                int currentProgress = achievements.getOrDefault(achievementId, 0);

                if (currentProgress == 0) {
                    updateAchievementProgress(player, achievementId, 1);
                }
            }
        }
    }

    /**
     * Check if a player has completed the skill master achievement
     * @param player The player
     */
    private void checkSkillMasterAchievement(Player player) {
        if (!enabled || !plugin.getSkillManager().isEnabled()) {
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        int maxLevel = plugin.getSkillManager().getMaxSkillLevel();

        // Check if all skills are maxed out
        if (playerData.getEfficiencyLevel() >= maxLevel &&
                playerData.getLuckLevel() >= maxLevel &&
                playerData.getEnergyLevel() >= maxLevel) {

            // Find the skill master achievement
            List<String> skillMasterAchievements = achievementsByType.getOrDefault("SKILL_MASTER", new ArrayList<>());
            for (String achievementId : skillMasterAchievements) {
                updateAchievementProgress(player, achievementId, 1);
            }
        }
    }

    /**
     * Check if a player has completed an achievement
     * @param player The player
     * @param achievementId The achievement ID
     * @return True if completed, false otherwise
     */
    public boolean hasCompletedAchievement(Player player, String achievementId) {
        if (!enabled || !achievementDefinitions.containsKey(achievementId)) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Integer> achievements = playerAchievements.getOrDefault(uuid, new HashMap<>());
        int currentProgress = achievements.getOrDefault(achievementId, 0);
        int requiredAmount = (int) achievementDefinitions.get(achievementId).get("amount");

        return currentProgress >= requiredAmount;
    }

    /**
     * Get the progress for an achievement
     * @param player The player
     * @param achievementId The achievement ID
     * @return The progress
     */
    public int getAchievementProgress(Player player, String achievementId) {
        if (!enabled || !achievementDefinitions.containsKey(achievementId)) {
            return 0;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Integer> achievements = playerAchievements.getOrDefault(uuid, new HashMap<>());
        return achievements.getOrDefault(achievementId, 0);
    }

    /**
     * Check if a player has claimed rewards for an achievement
     * @param player The player
     * @param achievementId The achievement ID
     * @return True if claimed, false otherwise
     */
    public boolean hasClaimedRewards(Player player, String achievementId) {
        if (!enabled || !achievementDefinitions.containsKey(achievementId)) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Boolean> claimed = claimedRewards.getOrDefault(uuid, new HashMap<>());
        return claimed.getOrDefault(achievementId, false);
    }

    /**
     * Claim rewards for an achievement
     * @param player The player
     * @param achievementId The achievement ID
     * @return True if rewards were claimed, false otherwise
     */
    public boolean claimAchievementRewards(Player player, String achievementId) {
        if (!enabled || !achievementDefinitions.containsKey(achievementId)) {
            return false;
        }

        // Check if achievement is completed
        if (!hasCompletedAchievement(player, achievementId)) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Boolean> claimed = claimedRewards.getOrDefault(uuid, new HashMap<>());

        // Check if rewards are already claimed
        if (claimed.getOrDefault(achievementId, false)) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.achievements.already-claimed"));
            return false;
        }

        // Get rewards
        Map<String, Object> achievement = achievementDefinitions.get(achievementId);
        Map<String, Object> rewards = (Map<String, Object>) achievement.get("rewards");

        if (rewards != null) {
            // Give money reward
            if (economyEnabled && rewards.containsKey("money")) {
                double money = (double) rewards.get("money");
                if (money > 0) {
                    String command = giveCommand
                            .replace("%player%", player.getName())
                            .replace("%amount%", String.valueOf(money));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }

            // Give item rewards
            if (rewards.containsKey("items")) {
                for (String itemString : (java.util.List<String>) rewards.get("items")) {
                    String[] parts = itemString.split(":");
                    if (parts.length >= 2) {
                        try {
                            Material material = Material.valueOf(parts[0]);
                            int amount = Integer.parseInt(parts[1]);
                            player.getInventory().addItem(new ItemStack(material, amount));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid item in achievement rewards: " + itemString);
                        }
                    }
                }
            }

            // Execute commands
            if (rewards.containsKey("commands")) {
                for (String commandString : (java.util.List<String>) rewards.get("commands")) {
                    String command = commandString.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }

        // Mark rewards as claimed
        claimed.put(achievementId, true);
        claimedRewards.put(uuid, claimed);

        // Send message
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.achievements.claimed"));

        return true;
    }

    /**
     * Get the custom icon for an achievement
     * @param achievementId The achievement ID
     * @return The custom icon material, or null if not found
     */
    public Material getCustomIcon(String achievementId) {
        return customIcons.getOrDefault(achievementId, null);
    }

    /**
     * Get the number of completed achievements for a player
     * @param player The player
     * @return The number of completed achievements
     */
    public int getCompletedAchievementsCount(Player player) {
        if (!enabled) {
            return 0;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Integer> achievements = playerAchievements.getOrDefault(uuid, new HashMap<>());
        int count = 0;

        for (Map.Entry<String, Integer> entry : achievements.entrySet()) {
            String achievementId = entry.getKey();
            int progress = entry.getValue();

            if (achievementDefinitions.containsKey(achievementId)) {
                int requiredAmount = (int) achievementDefinitions.get(achievementId).get("amount");
                if (progress >= requiredAmount) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Get the total number of achievements
     * @return The total number of achievements
     */
    public int getTotalAchievementsCount() {
        return achievementDefinitions.size();
    }

    /**
     * Notify achievement update
     * @param uuid The player UUID
     * @param achievementId The achievement ID
     * @param progress The new progress
     */
    private void notifyAchievementUpdate(UUID uuid, String achievementId, int progress) {
        // This method can be used to notify other systems about achievement updates
        // For example, sending messages to a Discord webhook
    }

    /**
     * Send a message to a Discord webhook
     * @param webhookUrl The webhook URL
     * @param message The message to send
     */
    private void sendDiscordWebhook(String webhookUrl, String message) {
        // Implementation for sending Discord webhook
        // This would typically use HttpURLConnection or a library like OkHttp
    }
}