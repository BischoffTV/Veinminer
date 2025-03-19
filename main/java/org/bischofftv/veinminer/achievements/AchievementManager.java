package org.bischofftv.veinminer.achievements;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bischofftv.veinminer.database.PlayerData;
import org.bukkit.ChatColor;

public class AchievementManager {

    private final Veinminer plugin;
    private final Map<String, Achievement> achievements;
    private final Map<UUID, Map<String, PlayerAchievement>> playerAchievements;
    private boolean enabled;
    private boolean economyEnabled;
    private String economyCommand;
    private boolean debug;

    public AchievementManager(Veinminer plugin) {
        this.plugin = plugin;
        this.achievements = new HashMap<>();
        this.playerAchievements = new ConcurrentHashMap<>();
        loadSettings();
    }

    public void loadSettings() {
        this.enabled = plugin.getConfig().getBoolean("achievement-system.enabled", true);
        this.economyEnabled = plugin.getConfig().getBoolean("economy.enabled", true);
        this.economyCommand = plugin.getConfig().getString("economy.give-command", "eco give %player% %amount%");
        this.debug = plugin.getConfig().getBoolean("settings.debug", false);

        // Speichere die vorhandenen Achievement-IDs, um später zu prüfen, ob neue hinzugefügt wurden
        Set<String> existingAchievementIds = new HashSet<>(achievements.keySet());

        // Leere die vorhandenen Achievements, aber behalte die Spielerdaten
        achievements.clear();

        // Lade Achievements aus der Konfiguration
        ConfigurationSection achievementsSection = plugin.getConfig().getConfigurationSection("achievements");
        if (achievementsSection != null) {
            for (String id : achievementsSection.getKeys(false)) {
                ConfigurationSection section = achievementsSection.getConfigurationSection(id);
                if (section != null) {
                    String name = section.getString("name", id);
                    String description = section.getString("description", "");
                    String type = section.getString("type", "BLOCK_MINE");
                    String block = section.getString("block", "");
                    int amount = section.getInt("amount", 1);

                    // Lade Belohnungen
                    ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
                    int money = 0;
                    List<String> items = new ArrayList<>();

                    if (rewardsSection != null) {
                        money = rewardsSection.getInt("money", 0);
                        items = rewardsSection.getStringList("items");
                    }

                    Achievement achievement = new Achievement(id, name, description, type, block, amount, money, items);
                    achievements.put(id, achievement);

                    if (debug) {
                        plugin.debug("[Debug] Loaded achievement: " + id + " - " + name +
                                " (Type: " + type + ", Block: " + block + ", Amount: " + amount + ")");
                    }

                    // Entferne die ID aus der Liste der vorhandenen Achievements
                    existingAchievementIds.remove(id);
                }
            }
        }

        // Überprüfe, ob neue Achievements hinzugefügt wurden
        if (!existingAchievementIds.isEmpty()) {
            plugin.getLogger().info("Es wurden " + existingAchievementIds.size() + " Achievements aus der Konfiguration entfernt.");
        }

        // Überprüfe, ob neue Achievements hinzugefügt wurden
        Set<String> newAchievementIds = new HashSet<>(achievements.keySet());
        newAchievementIds.removeAll(existingAchievementIds);

        if (!newAchievementIds.isEmpty()) {
            plugin.getLogger().info("Es wurden " + newAchievementIds.size() + " neue Achievements hinzugefügt.");

            // Initialisiere die neuen Achievements für alle Spieler
            initializeNewAchievements(newAchievementIds);

            // Force a database sync to ensure changes are propagated
            plugin.forceSyncDataNow();

            // Reload achievements for all online players to include the new ones
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                // Clear existing achievement data for this player to force a reload
                playerAchievements.remove(uuid);
                // Load achievements again
                loadPlayerAchievements(player);

                if (debug) {
                    plugin.debug("[Debug] Reloaded achievements for " + player.getName() + " after adding new achievements");
                }
            }
        }

        if (debug) {
            plugin.debug("[Debug] Loaded " + achievements.size() + " achievements");
        }
    }

    private void initializeNewAchievements(Set<String> newAchievementIds) {
        if (newAchievementIds.isEmpty()) {
            return;
        }

        // Initialisiere die neuen Achievements für alle Spieler in der Datenbank
        if (!plugin.getDatabaseManager().isFallbackMode()) {
            Connection conn = null;
            try {
                conn = plugin.getDatabaseManager().getConnection();
                if (conn == null) {
                    plugin.getLogger().warning("Konnte keine Verbindung zur Datenbank herstellen. Neue Achievements werden nicht initialisiert.");
                    return;
                }

                // Hole alle Spieler-UUIDs aus der Datenbank
                List<UUID> playerUUIDs = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT uuid FROM " + plugin.getDatabaseManager().getTablePrefix() + "players")) {
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            playerUUIDs.add(uuid);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Ungültige UUID in der Datenbank: " + rs.getString("uuid"));
                        }
                    }
                }

                if (playerUUIDs.isEmpty()) {
                    plugin.getLogger().info("Keine Spieler in der Datenbank gefunden. Neue Achievements werden nicht initialisiert.");
                    return;
                }

                // Initialisiere die neuen Achievements für jeden Spieler
                int initializedCount = 0;
                for (UUID uuid : playerUUIDs) {
                    for (String achievementId : newAchievementIds) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT IGNORE INTO " + plugin.getDatabaseManager().getTablePrefix() +
                                        "achievements (uuid, achievement_id, progress, claimed) VALUES (?, ?, 0, false)")) {
                            ps.setString(1, uuid.toString());
                            ps.setString(2, achievementId);
                            ps.executeUpdate();
                            initializedCount++;
                        } catch (SQLException e) {
                            plugin.getLogger().warning("Fehler beim Initialisieren des Achievements " + achievementId + " für " + uuid + ": " + e.getMessage());
                        }
                    }
                }

                plugin.getLogger().info("Initialisierte " + initializedCount + " neue Achievement-Einträge für " + playerUUIDs.size() + " Spieler.");
            } catch (SQLException e) {
                plugin.getLogger().severe("Fehler beim Initialisieren neuer Achievements: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        // Ignore
                    }
                }
            }
        }

        // Also initialize the new achievements for all online players in the cache
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Map<String, PlayerAchievement> playerAchievementMap = playerAchievements.get(uuid);

            if (playerAchievementMap != null) {
                boolean achievementsAdded = false;
                for (String achievementId : newAchievementIds) {
                    if (!playerAchievementMap.containsKey(achievementId)) {
                        PlayerAchievement playerAchievement = new PlayerAchievement(achievementId, 0, false);
                        playerAchievementMap.put(achievementId, playerAchievement);
                        achievementsAdded = true;

                        if (debug) {
                            plugin.debug("[Debug] Initialized new achievement " + achievementId + " for online player " + player.getName());
                        }

                        // Also save to database immediately
                        saveAchievement(player.getUniqueId(), achievementId, 0, false);
                    }
                }

                if (achievementsAdded) {
                    // Notify the player about new achievements
                    player.sendMessage(ChatColor.GREEN + "New achievements have been added! Check the achievements menu.");
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEconomyEnabled() {
        return this.economyEnabled;
    }

    public Collection<Achievement> getAchievements() {
        return achievements.values();
    }

    public Achievement getAchievement(String id) {
        return achievements.get(id);
    }

    public void loadPlayerAchievements(Player player) {
        if (!enabled) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Clear existing data for this player
        playerAchievements.remove(uuid);

        if (debug) {
            plugin.debug("[Debug] Loading achievements for " + player.getName());
        }

        // Add a method to force immediate, synchronous loading for critical scenarios
        if (Bukkit.isPrimaryThread()) {
            loadPlayerAchievementsSynchronously(player);
            return;
        }

        // Load from database asynchronously
        CompletableFuture.runAsync(() -> {
            Connection conn = null;
            try {
                Map<String, PlayerAchievement> playerAchievementMap = new HashMap<>();

                // Create table if it doesn't exist
                createAchievementsTable();

                // Load player achievements directly with retry logic
                boolean loaded = false;
                int attempts = 0;

                while (!loaded && attempts < 3) {
                    attempts++;

                    try {
                        conn = plugin.getDatabaseManager().getConnection();
                        if (conn == null) {
                            plugin.getLogger().warning("Could not load achievements for " + player.getName() + ": Database connection is null (attempt " + attempts + ")");
                            Thread.sleep(500); // Wait before retry
                            continue;
                        }

                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "achievements WHERE uuid = ?")) {

                            ps.setString(1, uuid.toString());
                            ResultSet rs = ps.executeQuery();

                            int achievementsLoaded = 0;
                            while (rs.next()) {
                                String achievementId = rs.getString("achievement_id");
                                int progress = rs.getInt("progress");
                                boolean claimed = rs.getBoolean("claimed");

                                PlayerAchievement playerAchievement = new PlayerAchievement(achievementId, progress, claimed);
                                playerAchievementMap.put(achievementId, playerAchievement);
                                achievementsLoaded++;

                                if (debug) {
                                    plugin.debug("[Debug] Loaded achievement for " + player.getName() + ": " +
                                            achievementId + ", Progress: " + progress + ", Claimed: " + claimed);
                                }
                            }

                            if (debug) {
                                plugin.debug("[Debug] Loaded " + achievementsLoaded + " achievements for " + player.getName());
                            }

                            // Mark as successfully loaded
                            loaded = true;
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Error loading achievements for " + player.getName() + " (attempt " + attempts + "): " + e.getMessage());
                        Thread.sleep(1000); // Wait longer before next retry
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        plugin.getLogger().warning("Achievement loading thread interrupted for " + player.getName());
                    } finally {
                        if (conn != null) {
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                // Ignore
                            }
                            conn = null;
                        }
                    }
                }

                // Initialize achievements that don't exist in the database
                int newAchievements = 0;
                for (String achievementId : achievements.keySet()) {
                    if (!playerAchievementMap.containsKey(achievementId)) {
                        PlayerAchievement playerAchievement = new PlayerAchievement(achievementId, 0, false);
                        playerAchievementMap.put(achievementId, playerAchievement);
                        newAchievements++;

                        // Insert into database
                        try {
                            conn = plugin.getDatabaseManager().getConnection();
                            if (conn != null) {
                                try (PreparedStatement ps = conn.prepareStatement(
                                        "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() +
                                                "achievements (uuid, achievement_id, progress, claimed) VALUES (?, ?, 0, false)")) {

                                    ps.setString(1, uuid.toString());
                                    ps.setString(2, achievementId);
                                    ps.executeUpdate();
                                }
                            }
                        } catch (SQLException e) {
                            plugin.getLogger().warning("Error initializing achievement " + achievementId + " for " + player.getName() + ": " + e.getMessage());
                        } finally {
                            if (conn != null) {
                                try {
                                    conn.close();
                                } catch (SQLException e) {
                                    // Ignore
                                }
                                conn = null;
                            }
                        }
                    }
                }

                if (debug && newAchievements > 0) {
                    plugin.debug("[Debug] Initialized " + newAchievements + " new achievements for " + player.getName());
                }

                // Store in cache
                playerAchievements.put(uuid, playerAchievementMap);

                // Check level achievements immediately after loading
                checkLevelAchievements(player);

                if (debug) {
                    plugin.debug("[Debug] Achievement loading completed for " + player.getName());
                    plugin.debug("[Debug] Total achievements in cache: " + playerAchievementMap.size());
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Error loading achievements for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();

                // Create default achievement data if loading failed
                Map<String, PlayerAchievement> tempAchievements = new HashMap<>();
                for (String achievementId : achievements.keySet()) {
                    tempAchievements.put(achievementId, new PlayerAchievement(achievementId, 0, false));
                }

                // Store in cache
                playerAchievements.put(uuid, tempAchievements);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        // Ignore
                    }
                }
            }
        });
    }

    private void loadPlayerAchievementsSynchronously(Player player) {
        UUID uuid = player.getUniqueId();

        Connection conn = null;
        try {
            Map<String, PlayerAchievement> playerAchievementMap = new HashMap<>();

            // Create table if it doesn't exist
            createAchievementsTable();

            // Load player achievements
            try {
                conn = plugin.getDatabaseManager().getConnection();
                if (conn == null) {
                    plugin.getLogger().warning("Could not load achievements synchronously for " + player.getName() + ": Database connection is null");

                    // Create default achievement data
                    for (String achievementId : achievements.keySet()) {
                        playerAchievementMap.put(achievementId, new PlayerAchievement(achievementId, 0, false));
                    }
                    playerAchievements.put(uuid, playerAchievementMap);
                    return;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "achievements WHERE uuid = ?")) {

                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();

                    int achievementsLoaded = 0;
                    while (rs.next()) {
                        String achievementId = rs.getString("achievement_id");
                        int progress = rs.getInt("progress");
                        boolean claimed = rs.getBoolean("claimed");

                        PlayerAchievement playerAchievement = new PlayerAchievement(achievementId, progress, claimed);
                        playerAchievementMap.put(achievementId, playerAchievement);
                        achievementsLoaded++;
                    }

                    if (debug) {
                        plugin.debug("[Debug] Synchronously loaded " + achievementsLoaded + " achievements for " + player.getName());
                    }
                }
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        // Ignore
                    }
                    conn = null;
                }
            }

            // Initialize achievements that don't exist in the database
            for (String achievementId : achievements.keySet()) {
                if (!playerAchievementMap.containsKey(achievementId)) {
                    playerAchievementMap.put(achievementId, new PlayerAchievement(achievementId, 0, false));
                }
            }

            // Store in cache
            playerAchievements.put(uuid, playerAchievementMap);

            // Don't check level achievements here to avoid recursion
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading achievements synchronously for " + player.getName() + ": " + e.getMessage());

            // Create default achievement data if loading failed
            Map<String, PlayerAchievement> defaultAchievements = new HashMap<>();
            for (String achievementId : achievements.keySet()) {
                defaultAchievements.put(achievementId, new PlayerAchievement(achievementId, 0, false));
            }

            // Store in cache
            playerAchievements.put(uuid, defaultAchievements);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }

    private void createAchievementsTable() throws SQLException {
        Connection conn = null;
        try {
            conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + plugin.getDatabaseManager().getTablePrefix() + "achievements (" +
                            "uuid VARCHAR(36) NOT NULL, " +
                            "achievement_id VARCHAR(64) NOT NULL, " +
                            "progress INT NOT NULL DEFAULT 0, " +
                            "claimed BOOLEAN NOT NULL DEFAULT false, " +
                            "PRIMARY KEY (uuid, achievement_id), " +
                            "FOREIGN KEY (uuid) REFERENCES " + plugin.getDatabaseManager().getTablePrefix() +
                            "players(uuid) ON DELETE CASCADE)")) {

                ps.executeUpdate();
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }

    public void savePlayerAchievements(Player player) {
        if (!enabled) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, PlayerAchievement> playerAchievementMap = playerAchievements.get(uuid);

        if (playerAchievementMap == null) {
            return;
        }

        if (debug) {
            plugin.debug("[Debug] Saving achievements for " + player.getName());
        }

        // Speichere direkt in der Datenbank, um sicherzustellen, dass die Änderungen sofort übernommen werden
        Connection conn = null;
        try {
            conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("Could not save achievements for " + player.getName() + ": Database connection is null");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() +
                            "achievements (uuid, achievement_id, progress, claimed) VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE progress = ?, claimed = ?")) {

                int updatedCount = 0;
                for (Map.Entry<String, PlayerAchievement> entry : playerAchievementMap.entrySet()) {
                    PlayerAchievement playerAchievement = entry.getValue();

                    ps.setString(1, uuid.toString());
                    ps.setString(2, playerAchievement.getAchievementId());
                    ps.setInt(3, playerAchievement.getProgress());
                    ps.setBoolean(4, playerAchievement.isClaimed());
                    ps.setInt(5, playerAchievement.getProgress());
                    ps.setBoolean(6, playerAchievement.isClaimed());
                    ps.addBatch();

                    if (debug) {
                        plugin.debug("[Debug] Batched achievement for " + player.getName() + ": " +
                                playerAchievement.getAchievementId() + ", Progress: " +
                                playerAchievement.getProgress() + ", Claimed: " +
                                playerAchievement.isClaimed());
                    }
                    updatedCount++;
                }

                int[] results = ps.executeBatch();

                if (debug) {
                    plugin.debug("[Debug] Saved " + updatedCount + " achievements for " + player.getName());
                }

                // Trigger synchronization to other servers
                plugin.forceSyncDataNow();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving achievements for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }

    public void saveAllAchievements() {
        if (!enabled) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerAchievements(player);
        }
    }

    public Map<String, PlayerAchievement> getPlayerAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        return playerAchievements.getOrDefault(uuid, new HashMap<>());
    }

    public PlayerAchievement getPlayerAchievement(Player player, String achievementId) {
        UUID uuid = player.getUniqueId();
        Map<String, PlayerAchievement> playerAchievementMap = playerAchievements.get(uuid);

        if (playerAchievementMap == null) {
            return null;
        }

        return playerAchievementMap.get(achievementId);
    }

    // Updated method to properly track block mining achievements
    public void updateBlockMineProgress(Player player, Material blockType, int amount) {
        if (!enabled) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, PlayerAchievement> playerAchievementMap = playerAchievements.get(uuid);

        if (playerAchievementMap == null) {
            if (debug) {
                plugin.debug("[Debug] No achievement data found for " + player.getName() +
                        " when updating block progress for " + blockType);
            }
            return;
        }

        String blockName = blockType.name();

        if (debug) {
            plugin.debug("[Debug] Checking block mining achievements for " + player.getName() +
                    " - Block: " + blockName + ", Amount: " + amount);
        }

        for (Achievement achievement : achievements.values()) {
            if (achievement.getType().equalsIgnoreCase("BLOCK_MINE")) {
                String[] blocks = achievement.getBlock().split(",");
                boolean matchesBlock = false;

                for (String block : blocks) {
                    String trimmedBlock = block.trim();
                    if (trimmedBlock.equalsIgnoreCase(blockName)) {
                        matchesBlock = true;
                        if (debug) {
                            plugin.debug("[Debug] Block " + blockName + " matches achievement " +
                                    achievement.getId() + " (" + trimmedBlock + ")");
                        }
                        break;
                    }
                }

                if (matchesBlock) {
                    PlayerAchievement playerAchievement = playerAchievementMap.get(achievement.getId());

                    if (playerAchievement != null && !playerAchievement.isClaimed()) {
                        int oldProgress = playerAchievement.getProgress();
                        int newProgress = Math.min(oldProgress + amount, achievement.getAmount());
                        playerAchievement.setProgress(newProgress);

                        if (debug) {
                            plugin.debug("[Debug] Updated progress for " + player.getName() +
                                    " on achievement " + achievement.getId() +
                                    ": " + oldProgress + " -> " + newProgress);
                        }

                        // Check if progress has been made
                        if (newProgress > oldProgress) {
                            // Check if achievement is completed
                            if (newProgress == achievement.getAmount() && oldProgress < achievement.getAmount()) {
                                // Achievement completed
                                Map<String, String> placeholders = new HashMap<>();
                                placeholders.put("name", achievement.getName());
                                player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.progress.completed", placeholders));

                                // Notify other servers about this significant progress
                                plugin.getDatabaseManager().notifyAchievementUpdate(uuid, achievement.getId());
                            } else if (newProgress % (achievement.getAmount() / 4) == 0 ||
                                    (newProgress == achievement.getAmount() / 2)) {
                                // Progress update (at 25%, 50%, 75%)
                                Map<String, String> placeholders = new HashMap<>();
                                placeholders.put("name", achievement.getName());
                                placeholders.put("progress", String.valueOf(newProgress));
                                placeholders.put("total", String.valueOf(achievement.getAmount()));
                                player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.progress.updated", placeholders));

                                // For significant milestones, also notify other servers
                                plugin.getDatabaseManager().notifyAchievementUpdate(uuid, achievement.getId());
                            }
                        }
                    }
                }
            }
        }
    }

    public void updateTotalBlocksProgress(Player player, int amount) {
        if (!enabled) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, PlayerAchievement> playerAchievementMap = playerAchievements.get(uuid);

        if (playerAchievementMap == null) {
            if (debug) {
                plugin.debug("[Debug] No achievement data found for " + player.getName() +
                        " when updating total blocks progress");
            }
            return;
        }

        if (debug) {
            plugin.debug("[Debug] Updating total blocks progress for " + player.getName() +
                    ": +" + amount + " blocks");
        }

        for (Achievement achievement : achievements.values()) {
            if (achievement.getType().equalsIgnoreCase("TOTAL_BLOCKS")) {
                PlayerAchievement playerAchievement = playerAchievementMap.get(achievement.getId());

                if (playerAchievement != null && !playerAchievement.isClaimed()) {
                    int oldProgress = playerAchievement.getProgress();
                    int newProgress = Math.min(oldProgress + amount, achievement.getAmount());
                    playerAchievement.setProgress(newProgress);

                    if (debug) {
                        plugin.debug("[Debug] Updated total blocks progress for " + player.getName() +
                                " on achievement " + achievement.getId() +
                                ": " + oldProgress + " -> " + newProgress);
                    }

                    // Check if progress has been made
                    if (newProgress > oldProgress) {
                        // Check if achievement is completed
                        if (newProgress == achievement.getAmount() && oldProgress < achievement.getAmount()) {
                            // Achievement completed
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("name", achievement.getName());
                            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.progress.completed", placeholders));
                        } else if (newProgress % (achievement.getAmount() / 4) == 0 ||
                                (newProgress == achievement.getAmount() / 2)) {
                            // Progress update (at 25%, 50%, 75%)
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("name", achievement.getName());
                            placeholders.put("progress", String.valueOf(newProgress));
                            placeholders.put("total", String.valueOf(achievement.getAmount()));
                            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.progress.updated", placeholders));
                        }
                    }
                }
            }
        }
    }

    // Check level achievements based on player's current level in the database
    private void checkLevelAchievements(Player player) {
        if (!enabled || !plugin.getLevelManager().isEnabled()) {
            return;
        }

        // Get player's current level from the level manager
        UUID uuid = player.getUniqueId();
        int currentLevel = 1; // Default level

        try {
            // Get player data from level manager
            if (plugin.getLevelManager().getPlayerData(uuid) != null) {
                currentLevel = plugin.getLevelManager().getPlayerData(uuid).getLevel();

                if (debug) {
                    plugin.debug("[Debug] Checking level achievements for " + player.getName() +
                            " - Current Level: " + currentLevel);
                }

                // Update level achievements with the current level
                updateLevelProgress(player, currentLevel);
            } else {
                if (debug) {
                    plugin.debug("[Debug] No level data found for " + player.getName() +
                            " when checking level achievements");
                }

                // Try to load level data directly from database as a fallback
                try {
                    org.bischofftv.veinminer.database.PlayerData playerData = plugin.getDatabaseManager().loadPlayerData(player).join();
                    if (playerData != null) {
                        currentLevel = playerData.getLevel();
                        if (debug) {
                            plugin.debug("[Debug] Loaded level data directly from database: " + currentLevel);
                        }
                        updateLevelProgress(player, currentLevel);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load level data from database: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking level achievements for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Updated method to use the player's level from the database
    public void updateLevelProgress(Player player, int level) {
        if (!enabled) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, PlayerAchievement> playerAchievementMap = playerAchievements.get(uuid);

        if (playerAchievementMap == null) {
            if (debug) {
                plugin.debug("[Debug] No achievement data found for " + player.getName() +
                        " when updating level progress");
            }
            return;
        }

        if (debug) {
            plugin.debug("[Debug] Updating level achievements for " + player.getName() +
                    " - Current Level: " + level);
        }

        for (Achievement achievement : achievements.values()) {
            if (achievement.getType().equalsIgnoreCase("LEVEL")) {
                PlayerAchievement playerAchievement = playerAchievementMap.get(achievement.getId());

                if (playerAchievement != null && !playerAchievement.isClaimed()) {
                    int targetLevel = achievement.getAmount();

                    if (debug) {
                        plugin.debug("[Debug] Checking level achievement " + achievement.getId() +
                                " - Target Level: " + targetLevel +
                                ", Current Level: " + level +
                                ", Current Progress: " + playerAchievement.getProgress());
                    }

                    // Update progress to match current level if it's higher
                    if (level > playerAchievement.getProgress()) {
                        playerAchievement.setProgress(level);

                        if (debug) {
                            plugin.debug("[Debug] Updated level progress for " + player.getName() +
                                    " on achievement " + achievement.getId() +
                                    ": " + playerAchievement.getProgress() + "/" + targetLevel);
                        }
                    }

                    // Check if achievement is completed
                    if (level >= targetLevel && playerAchievement.getProgress() < targetLevel) {
                        playerAchievement.setProgress(targetLevel);

                        if (debug) {
                            plugin.debug("[Debug] Level achievement completed for " + player.getName() +
                                    ": " + achievement.getId());
                        }

                        // Achievement completed
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("name", achievement.getName());
                        player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.progress.completed", placeholders));
                    }
                }
            }
        }
    }

    // Update the claimAchievement method to provide better debugging
    public boolean claimAchievement(Player player, String achievementId) {
        if (!enabled) {
            if (debug) {
                plugin.debug("[Debug] Achievement system is disabled");
            }
            return false;
        }

        UUID uuid = player.getUniqueId();
        Map<String, PlayerAchievement> playerAchievementMap = playerAchievements.get(uuid);

        if (playerAchievementMap == null) {
            if (debug) {
                plugin.debug("[Debug] No achievement data found for player " + player.getName());
            }
            return false;
        }

        PlayerAchievement playerAchievement = playerAchievementMap.get(achievementId);
        Achievement achievement = achievements.get(achievementId);

        if (playerAchievement == null) {
            if (debug) {
                plugin.debug("[Debug] Player achievement data not found for: " + achievementId);
            }
            return false;
        }

        if (achievement == null) {
            if (debug) {
                plugin.debug("[Debug] Achievement definition not found for: " + achievementId);
            }
            return false;
        }

        if (debug) {
            plugin.debug("[Debug] Attempting to claim achievement: " + achievement.getName() +
                    " (" + achievementId + ") for " + player.getName());
            plugin.debug("[Debug] Progress: " + playerAchievement.getProgress() + "/" +
                    achievement.getAmount() + ", Claimed: " + playerAchievement.isClaimed());
        }

        // Check if already claimed
        if (playerAchievement.isClaimed()) {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.claim.already-claimed"));
            if (debug) {
                plugin.debug("[Debug] Achievement already claimed");
            }
            return false;
        }

        // Check if completed
        if (playerAchievement.getProgress() < achievement.getAmount()) {
            if (debug) {
                plugin.debug("[Debug] Achievement not completed yet: " +
                        playerAchievement.getProgress() + "/" + achievement.getAmount());
            }
            return false;
        }

        // Mark as claimed
        playerAchievement.setClaimed(true);

        // Give rewards
        boolean success = giveRewards(player, achievement);

        // After achievement is successfully claimed and before the message is shown:
        if (success) {
            // Notify other servers about this achievement update
            plugin.getDatabaseManager().notifyAchievementUpdate(uuid, achievementId);

            // Send success message
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", achievement.getName());
            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.claim.success", placeholders));

            // Build rewards message
            StringBuilder rewardsMessage = new StringBuilder();

            if (achievement.getMoney() > 0 && economyEnabled) {
                Map<String, String> moneyPlaceholders = new HashMap<>();
                moneyPlaceholders.put("amount", String.valueOf(achievement.getMoney()));
                rewardsMessage.append(plugin.getMessageManager().getMessage("messages.achievements.claim.money", moneyPlaceholders));
            }

            if (!achievement.getItems().isEmpty()) {
                if (rewardsMessage.length() > 0) {
                    rewardsMessage.append(", ");
                }

                Map<String, String> itemsPlaceholders = new HashMap<>();
                itemsPlaceholders.put("items", formatItemsList(achievement.getItems()));
                rewardsMessage.append(plugin.getMessageManager().getMessage("messages.achievements.claim.items", itemsPlaceholders));
            }

            if (rewardsMessage.length() > 0) {
                Map<String, String> rewardsPlaceholders = new HashMap<>();
                rewardsPlaceholders.put("rewards", rewardsMessage.toString());
                player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.claim.rewards", rewardsPlaceholders));
            }

            // WICHTIG: Sofort in der Datenbank speichern
            try {
                // Direkt in der Datenbank speichern, nicht asynchron
                Connection conn = plugin.getDatabaseManager().getConnection();
                if (conn != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE " + plugin.getDatabaseManager().getTablePrefix() +
                                    "achievements SET progress = ?, claimed = ? WHERE uuid = ? AND achievement_id = ?")) {

                        ps.setInt(1, playerAchievement.getProgress());
                        ps.setBoolean(2, playerAchievement.isClaimed());
                        ps.setString(3, uuid.toString());
                        ps.setString(4, achievementId);
                        ps.executeUpdate();

                        if (debug) {
                            plugin.debug("[Debug] Directly saved achievement claim to database: " +
                                    achievementId + " for player " + player.getName());
                        }
                    }
                    conn.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error directly saving achievement claim: " + e.getMessage());
            }

            // Dann auch asynchron speichern für alle anderen Achievements
            savePlayerAchievements(player);

            // Force synchronization to other servers
            plugin.forceSyncDataNow();

            // Warte kurz und synchronisiere erneut, um sicherzustellen, dass die Änderungen übernommen wurden
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.forceSyncDataNow();

                // Überprüfe, ob die Änderungen korrekt gespeichert wurden
                if (debug) {
                    plugin.debug("[Debug] Verifying achievement claim was saved correctly for " + player.getName());
                    PlayerAchievement verifyAchievement = playerAchievementMap.get(achievementId);
                    if (verifyAchievement != null) {
                        plugin.debug("[Debug] Current state: Progress=" + verifyAchievement.getProgress() +
                                ", Claimed=" + verifyAchievement.isClaimed());
                    }
                }
            }, 20L); // 1 Sekunde später

            if (debug) {
                plugin.debug("[Debug] Achievement successfully claimed");
            }

            return true;
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.claim.failed"));
            if (debug) {
                plugin.debug("[Debug] Failed to give rewards");
            }
            return false;
        }
    }

    private boolean giveRewards(Player player, Achievement achievement) {
        try {
            // Give money if enabled
            if (achievement.getMoney() > 0 && economyEnabled) {
                String command = economyCommand
                        .replace("%player%", player.getName())
                        .replace("%amount%", String.valueOf(achievement.getMoney()));

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

                if (debug) {
                    plugin.debug("[Debug] Executed economy command: " + command);
                }
            }

            // Give items
            for (String itemString : achievement.getItems()) {
                String[] parts = itemString.split(":");
                if (parts.length >= 2) {
                    try {
                        Material material = Material.valueOf(parts[0]);
                        int amount = Integer.parseInt(parts[1]);

                        ItemStack item = new ItemStack(material, amount);
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);

                        // Drop items that didn't fit in inventory
                        for (ItemStack leftoverItem : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
                        }

                        if (debug) {
                            plugin.debug("[Debug] Gave item to " + player.getName() + ": " +
                                    material.name() + " x" + amount);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid item in achievement rewards: " + itemString);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error giving rewards to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String formatItemsList(List<String> items) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < items.size(); i++) {
            String itemString = items.get(i);
            String[] parts = itemString.split(":");

            if (parts.length >= 2) {
                try {
                    Material material = Material.valueOf(parts[0]);
                    int amount = Integer.parseInt(parts[1]);

                    sb.append(amount).append("x ").append(formatMaterialName(material.name()));

                    if (i < items.size() - 1) {
                        sb.append(", ");
                    }
                } catch (IllegalArgumentException e) {
                    // Skip invalid items
                }
            }
        }

        return sb.toString();
    }

    private String formatMaterialName(String name) {
        String[] words = name.toLowerCase().split("_");
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

    /**
     * Saves an achievement directly to the database
     */
    private void saveAchievement(UUID uuid, String achievementId, int progress, boolean claimed) {
        if (plugin.getDatabaseManager().isFallbackMode()) {
            // Save to local cache in fallback mode
            Map<String, Object> achievements = plugin.getDatabaseManager().getLocalAchievementCache().computeIfAbsent(uuid, k -> new HashMap<>());
            Map<String, Object> achievementData = new HashMap<>();
            achievementData.put("progress", progress);
            achievementData.put("claimed", claimed);
            achievements.put(achievementId, achievementData);
            return;
        }

        Connection conn = null;
        try {
            conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + "achievements (uuid, achievement_id, progress, claimed) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE progress = ?, claimed = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, achievementId);
                ps.setInt(3, progress);
                ps.setBoolean(4, claimed);
                ps.setInt(5, progress);
                ps.setBoolean(6, claimed);
                ps.executeUpdate();

                if (debug) {
                    plugin.debug("[Debug] Saved achievement for " + uuid +
                            ": " + achievementId +
                            ", Progress: " + progress +
                            ", Claimed: " + claimed);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving achievement for " + uuid + ": " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Clears the cached achievements for a player to force a reload
     */
    public void clearPlayerAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        playerAchievements.remove(uuid);

        if (debug) {
            plugin.debug("[Debug] Cleared achievement cache for " + player.getName());
        }
    }

    public static class Achievement {
        private final String id;
        private final String name;
        private final String description;
        private final String type;
        private final String block;
        private final int amount;
        private final int money;
        private final List<String> items;

        public Achievement(String id, String name, String description, String type, String block, int amount, int money, List<String> items) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.block = block;
            this.amount = amount;
            this.money = money;
            this.items = items;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getBlock() {
            return block;
        }

        public int getAmount() {
            return amount;
        }

        public int getMoney() {
            return money;
        }

        public List<String> getItems() {
            return items;
        }
    }

    public static class PlayerAchievement {
        private final String achievementId;
        private int progress;
        private boolean claimed;

        public PlayerAchievement(String achievementId, int progress, boolean claimed) {
            this.achievementId = achievementId;
            this.progress = progress;
            this.claimed = claimed;
        }

        public String getAchievementId() {
            return achievementId;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public boolean isClaimed() {
            return claimed;
        }

        public void setClaimed(boolean claimed) {
            this.claimed = claimed;
        }
    }
}