package org.bischofftv.veinminer.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

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

public class VeinMinerPlaceholders extends PlaceholderExpansion {

    private final Veinminer plugin;

    // Cache for top players to avoid frequent database queries
    private Map<String, List<TopPlayerData>> topPlayersCache = new HashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds

    // Cache for placeholder results to avoid excessive logging and database queries
    private Map<String, String> placeholderResultCache = new ConcurrentHashMap<>();
    private long lastPlaceholderCacheUpdate = 0;
    private static final long PLACEHOLDER_CACHE_DURATION = 30 * 1000; // 30 seconds

    public VeinMinerPlaceholders(Veinminer plugin) {
        this.plugin = plugin;
        // Initialize cache on startup
        refreshCache();
    }

    @Override
    public String getIdentifier() {
        return "veinminer";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is important so the expansion doesn't unregister on reload
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        // Check if we have this placeholder in cache
        String cacheKey = (player != null ? player.getUniqueId().toString() : "null") + ":" + identifier;

        // Return from cache if available and not expired
        if (placeholderResultCache.containsKey(cacheKey) &&
                System.currentTimeMillis() - lastPlaceholderCacheUpdate < PLACEHOLDER_CACHE_DURATION) {
            return placeholderResultCache.get(cacheKey);
        }

        // Only log once per cache refresh to avoid spam
        if (System.currentTimeMillis() - lastPlaceholderCacheUpdate > PLACEHOLDER_CACHE_DURATION) {
            plugin.debug("PlaceholderAPI request: " + identifier + " for player: " + (player != null ? player.getName() : "null"));
        }

        // Check if cache needs to be refreshed
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_DURATION) {
            refreshCache();
        }

        String result = null;

        // Player-specific placeholders
        if (player != null) {
            // Player level
            if (identifier.equals("level")) {
                if (!plugin.getLevelManager().isEnabled()) {
                    result = "Disabled";
                } else {
                    try {
                        result = String.valueOf(plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getLevel());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error getting level for " + player.getName() + ": " + e.getMessage());
                        result = "Error";
                    }
                }
            }

            // Player experience
            else if (identifier.equals("experience")) {
                if (!plugin.getLevelManager().isEnabled()) {
                    result = "Disabled";
                } else {
                    try {
                        result = String.valueOf(plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getExperience());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error getting experience for " + player.getName() + ": " + e.getMessage());
                        result = "Error";
                    }
                }
            }

            // Player blocks mined
            else if (identifier.equals("blocks_mined")) {
                if (!plugin.getLevelManager().isEnabled()) {
                    result = "Disabled";
                } else {
                    try {
                        result = String.valueOf(plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getBlocksMined());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error getting blocks mined for " + player.getName() + ": " + e.getMessage());
                        result = "Error";
                    }
                }
            }

            // Player achievements completed
            else if (identifier.equals("achievements_completed")) {
                if (!plugin.getAchievementManager().isEnabled()) {
                    result = "Disabled";
                } else {
                    try {
                        int completed = 0;
                        Player onlinePlayer = player.getPlayer();
                        if (onlinePlayer != null) {
                            Map<String, Integer> achievements = plugin.getAchievementManager().getPlayerAchievements(onlinePlayer);
                            Map<String, Map<String, Object>> definitions = plugin.getAchievementManager().getAchievementDefinitions();

                            for (Map.Entry<String, Integer> entry : achievements.entrySet()) {
                                String achievementId = entry.getKey();
                                int progress = entry.getValue();

                                if (definitions.containsKey(achievementId)) {
                                    int requiredAmount = (int) definitions.get(achievementId).get("amount");
                                    if (progress >= requiredAmount) {
                                        completed++;
                                    }
                                }
                            }
                        }
                        result = String.valueOf(completed);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error getting achievements for " + player.getName() + ": " + e.getMessage());
                        result = "Error";
                    }
                }
            }
        }

        // Top players by level
        if (identifier.startsWith("top_level_")) {
            try {
                int position = Integer.parseInt(identifier.substring("top_level_".length()));
                if (position < 1 || position > 10) {
                    result = "Invalid position";
                } else {
                    List<TopPlayerData> topLevelPlayers = topPlayersCache.getOrDefault("level", new ArrayList<>());
                    if (position <= topLevelPlayers.size()) {
                        TopPlayerData playerData = topLevelPlayers.get(position - 1);
                        result = playerData.getPlayerName() + ": " + playerData.getValue();
                    } else {
                        result = "N/A";
                    }
                }
            } catch (NumberFormatException e) {
                result = "Invalid format";
            }
        }

        // Top players by achievements
        else if (identifier.startsWith("top_achievements_")) {
            try {
                int position = Integer.parseInt(identifier.substring("top_achievements_".length()));
                if (position < 1 || position > 10) {
                    result = "Invalid position";
                } else {
                    List<TopPlayerData> topAchievementPlayers = topPlayersCache.getOrDefault("achievements", new ArrayList<>());
                    if (position <= topAchievementPlayers.size()) {
                        TopPlayerData playerData = topAchievementPlayers.get(position - 1);
                        result = playerData.getPlayerName() + ": " + playerData.getValue();
                    } else {
                        result = "N/A";
                    }
                }
            } catch (NumberFormatException e) {
                result = "Invalid format";
            }
        }

        // Top players by blocks mined
        else if (identifier.startsWith("top_blocks_")) {
            try {
                int position = Integer.parseInt(identifier.substring("top_blocks_".length()));
                if (position < 1 || position > 10) {
                    result = "Invalid position";
                } else {
                    List<TopPlayerData> topBlocksPlayers = topPlayersCache.getOrDefault("blocks", new ArrayList<>());
                    if (position <= topBlocksPlayers.size()) {
                        TopPlayerData playerData = topBlocksPlayers.get(position - 1);
                        result = playerData.getPlayerName() + ": " + playerData.getValue();
                    } else {
                        result = "N/A";
                    }
                }
            } catch (NumberFormatException e) {
                result = "Invalid format";
            }
        }

        // Cache the result
        if (result != null) {
            placeholderResultCache.put(cacheKey, result);
        }

        return result;
    }

    /**
     * Refresh the cache of top players
     */
    public void refreshCache() {
        plugin.debug("Refreshing PlaceholderAPI cache...");

        if (plugin.getDatabaseManager() == null || !plugin.getDatabaseManager().checkConnection()) {
            plugin.debug("Cannot refresh cache: Database connection is not available");
            return;
        }

        // Clear existing cache
        topPlayersCache.clear();

        // Get top players by level
        topPlayersCache.put("level", getTopPlayersByLevel(10));

        // Get top players by achievements
        topPlayersCache.put("achievements", getTopPlayersByAchievements(10));

        // Get top players by blocks mined
        topPlayersCache.put("blocks", getTopPlayersByBlocksMined(10));

        // Update cache timestamp
        lastCacheUpdate = System.currentTimeMillis();

        // Also update placeholder cache timestamp
        lastPlaceholderCacheUpdate = System.currentTimeMillis();
        // Clear placeholder cache
        placeholderResultCache.clear();

        plugin.debug("PlaceholderAPI cache refreshed successfully");
    }

    /**
     * Get top players by level
     * @param limit The maximum number of players to return
     * @return A list of top players by level
     */
    private List<TopPlayerData> getTopPlayersByLevel(int limit) {
        List<TopPlayerData> topPlayers = new ArrayList<>();
        
        try {
            topPlayers = plugin.getDatabaseManager().executeInTransaction(connection -> {
                List<TopPlayerData> result = new ArrayList<>();
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                
                try {
                    String sql = "SELECT player_name, level FROM " + plugin.getDatabaseManager().getTablePrefix() +
                            "player_data ORDER BY level DESC, experience DESC LIMIT ?";
                    statement = connection.prepareStatement(sql);
                    statement.setInt(1, limit);

                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        String playerName = resultSet.getString("player_name");
                        int level = resultSet.getInt("level");
                        result.add(new TopPlayerData(playerName, level));
                    }
                } finally {
                    if (resultSet != null) resultSet.close();
                    if (statement != null) statement.close();
                }
                
                return result;
            });
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get top players by level: " + e.getMessage());
        }

        return topPlayers;
    }

    /**
     * Get top players by achievements completed
     * @param limit The maximum number of players to return
     * @return A list of top players by achievements completed
     */
    private List<TopPlayerData> getTopPlayersByAchievements(int limit) {
        List<TopPlayerData> topPlayers = new ArrayList<>();
        
        try {
            topPlayers = plugin.getDatabaseManager().executeInTransaction(connection -> {
                List<TopPlayerData> result = new ArrayList<>();
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                
                try {
                    String sql = "SELECT a.uuid, p.player_name, COUNT(*) as completed_count " +
                            "FROM " + plugin.getDatabaseManager().getTablePrefix() + "achievements a " +
                            "JOIN " + plugin.getDatabaseManager().getTablePrefix() + "player_data p ON a.uuid = p.uuid " +
                            "WHERE a.completed = 1 " +
                            "GROUP BY a.uuid, p.player_name " +
                            "ORDER BY completed_count DESC LIMIT ?";

                    statement = connection.prepareStatement(sql);
                    statement.setInt(1, limit);

                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        String playerName = resultSet.getString("player_name");
                        int completedCount = resultSet.getInt("completed_count");
                        result.add(new TopPlayerData(playerName, completedCount));
                    }
                } finally {
                    if (resultSet != null) resultSet.close();
                    if (statement != null) statement.close();
                }
                
                return result;
            });
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get top players by achievements: " + e.getMessage());
        }

        return topPlayers;
    }

    /**
     * Get top players by blocks mined
     * @param limit The maximum number of players to return
     * @return A list of top players by blocks mined
     */
    private List<TopPlayerData> getTopPlayersByBlocksMined(int limit) {
        List<TopPlayerData> topPlayers = new ArrayList<>();
        
        try {
            topPlayers = plugin.getDatabaseManager().executeInTransaction(connection -> {
                List<TopPlayerData> result = new ArrayList<>();
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                
                try {
                    String sql = "SELECT player_name, blocks_mined FROM " + plugin.getDatabaseManager().getTablePrefix() +
                            "player_data ORDER BY blocks_mined DESC LIMIT ?";
                    statement = connection.prepareStatement(sql);
                    statement.setInt(1, limit);

                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        String playerName = resultSet.getString("player_name");
                        int blocksMined = resultSet.getInt("blocks_mined");
                        result.add(new TopPlayerData(playerName, blocksMined));
                    }
                } finally {
                    if (resultSet != null) resultSet.close();
                    if (statement != null) statement.close();
                }
                
                return result;
            });
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get top players by blocks mined: " + e.getMessage());
        }

        return topPlayers;
    }

    /**
     * Force refresh the cache
     */
    public void forceRefreshCache() {
        refreshCache();
    }

    /**
     * Class to store top player data
     */
    private static class TopPlayerData {
        private final String playerName;
        private final int value;

        public TopPlayerData(String playerName, int value) {
            this.playerName = playerName;
            this.value = value;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getValue() {
            return value;
        }
    }
}