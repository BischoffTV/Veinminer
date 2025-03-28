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

public class VeinMinerPlaceholders extends PlaceholderExpansion {

    private final Veinminer plugin;

    // Cache for top players to avoid frequent database queries
    private Map<String, List<TopPlayerData>> topPlayersCache = new HashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds

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
        plugin.getLogger().info("PlaceholderAPI request: " + identifier + " for player: " + (player != null ? player.getName() : "null"));

        // Check if cache needs to be refreshed
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_DURATION) {
            refreshCache();
        }

        // Player-specific placeholders
        if (player != null) {
            // Player level
            if (identifier.equals("level")) {
                if (!plugin.getLevelManager().isEnabled()) {
                    return "Disabled";
                }
                try {
                    return String.valueOf(plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getLevel());
                } catch (Exception e) {
                    plugin.getLogger().warning("Error getting level for " + player.getName() + ": " + e.getMessage());
                    return "Error";
                }
            }

            // Player experience
            if (identifier.equals("experience")) {
                if (!plugin.getLevelManager().isEnabled()) {
                    return "Disabled";
                }
                try {
                    return String.valueOf(plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getExperience());
                } catch (Exception e) {
                    plugin.getLogger().warning("Error getting experience for " + player.getName() + ": " + e.getMessage());
                    return "Error";
                }
            }

            // Player blocks mined
            if (identifier.equals("blocks_mined")) {
                if (!plugin.getLevelManager().isEnabled()) {
                    return "Disabled";
                }
                try {
                    return String.valueOf(plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getBlocksMined());
                } catch (Exception e) {
                    plugin.getLogger().warning("Error getting blocks mined for " + player.getName() + ": " + e.getMessage());
                    return "Error";
                }
            }

            // Player achievements completed
            if (identifier.equals("achievements_completed")) {
                if (!plugin.getAchievementManager().isEnabled()) {
                    return "Disabled";
                }

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
                    return String.valueOf(completed);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error getting achievements for " + player.getName() + ": " + e.getMessage());
                    return "Error";
                }
            }
        }

        // Top players by level
        if (identifier.startsWith("top_level_")) {
            try {
                int position = Integer.parseInt(identifier.substring("top_level_".length()));
                if (position < 1 || position > 10) {
                    return "Invalid position";
                }

                List<TopPlayerData> topLevelPlayers = topPlayersCache.getOrDefault("level", new ArrayList<>());
                if (position <= topLevelPlayers.size()) {
                    TopPlayerData playerData = topLevelPlayers.get(position - 1);
                    return playerData.getPlayerName() + ": " + playerData.getValue();
                } else {
                    return "N/A";
                }
            } catch (NumberFormatException e) {
                return "Invalid format";
            }
        }

        // Top players by achievements
        if (identifier.startsWith("top_achievements_")) {
            try {
                int position = Integer.parseInt(identifier.substring("top_achievements_".length()));
                if (position < 1 || position > 10) {
                    return "Invalid position";
                }

                List<TopPlayerData> topAchievementPlayers = topPlayersCache.getOrDefault("achievements", new ArrayList<>());
                if (position <= topAchievementPlayers.size()) {
                    TopPlayerData playerData = topAchievementPlayers.get(position - 1);
                    return playerData.getPlayerName() + ": " + playerData.getValue();
                } else {
                    return "N/A";
                }
            } catch (NumberFormatException e) {
                return "Invalid format";
            }
        }

        // Top players by blocks mined
        if (identifier.startsWith("top_blocks_")) {
            try {
                int position = Integer.parseInt(identifier.substring("top_blocks_".length()));
                if (position < 1 || position > 10) {
                    return "Invalid position";
                }

                List<TopPlayerData> topBlocksPlayers = topPlayersCache.getOrDefault("blocks", new ArrayList<>());
                if (position <= topBlocksPlayers.size()) {
                    TopPlayerData playerData = topBlocksPlayers.get(position - 1);
                    return playerData.getPlayerName() + ": " + playerData.getValue();
                } else {
                    return "N/A";
                }
            } catch (NumberFormatException e) {
                return "Invalid format";
            }
        }

        return null; // Placeholder not found
    }

    /**
     * Refresh the cache of top players
     */
    public void refreshCache() {
        plugin.getLogger().info("Refreshing PlaceholderAPI cache...");

        if (plugin.getDatabaseManager() == null || !plugin.getDatabaseManager().checkConnection()) {
            plugin.getLogger().warning("Cannot refresh cache: Database connection is not available");
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

        plugin.getLogger().info("PlaceholderAPI cache refreshed successfully");
    }

    /**
     * Get top players by level
     * @param limit The maximum number of players to return
     * @return A list of top players by level
     */
    private List<TopPlayerData> getTopPlayersByLevel(int limit) {
        List<TopPlayerData> topPlayers = new ArrayList<>();

        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            if (connection == null) {
                plugin.getLogger().warning("Failed to get database connection for top players by level");
                return topPlayers;
            }

            String sql = "SELECT player_name, level FROM " + plugin.getDatabaseManager().getTablePrefix() +
                    "player_data ORDER BY level DESC, experience DESC LIMIT ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, limit);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                int level = resultSet.getInt("level");

                topPlayers.add(new TopPlayerData(playerName, level));
            }

            resultSet.close();
            statement.close();
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
            Connection connection = plugin.getDatabaseManager().getConnection();
            if (connection == null) {
                plugin.getLogger().warning("Failed to get database connection for top players by achievements");
                return topPlayers;
            }

            String sql = "SELECT a.uuid, p.player_name, COUNT(*) as completed_count " +
                    "FROM " + plugin.getDatabaseManager().getTablePrefix() + "achievements a " +
                    "JOIN " + plugin.getDatabaseManager().getTablePrefix() + "player_data p ON a.uuid = p.uuid " +
                    "WHERE a.completed = 1 " +
                    "GROUP BY a.uuid, p.player_name " +
                    "ORDER BY completed_count DESC LIMIT ?";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, limit);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                int completedCount = resultSet.getInt("completed_count");

                topPlayers.add(new TopPlayerData(playerName, completedCount));
            }

            resultSet.close();
            statement.close();
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
            Connection connection = plugin.getDatabaseManager().getConnection();
            if (connection == null) {
                plugin.getLogger().warning("Failed to get database connection for top players by blocks mined");
                return topPlayers;
            }

            String sql = "SELECT player_name, blocks_mined FROM " + plugin.getDatabaseManager().getTablePrefix() +
                    "player_data ORDER BY blocks_mined DESC LIMIT ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, limit);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                int blocksMined = resultSet.getInt("blocks_mined");

                topPlayers.add(new TopPlayerData(playerName, blocksMined));
            }

            resultSet.close();
            statement.close();
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