package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Veinminer plugin;
    private final Map<UUID, PlayerData> playerDataMap;

    public PlayerDataManager(Veinminer plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
    }

    /**
     * Load player data from the database
     * @param player The player
     */
    public void loadPlayerData(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Check if player data is already loaded
        if (playerDataMap.containsKey(playerUUID)) {
            return;
        }

        // Create new player data
        PlayerData playerData = new PlayerData(playerUUID);

        // Try to load from database
        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            if (connection != null) {
                String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "player_data WHERE uuid = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, playerUUID.toString());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    playerData.setVeinMinerEnabled(resultSet.getBoolean("enabled"));
                    playerData.setLevel(resultSet.getInt("level"));
                    playerData.setExperience(resultSet.getInt("experience"));
                    playerData.setBlocksMined(resultSet.getInt("blocks_mined"));

                    // Load tool settings
                    playerData.setToolEnabled("pickaxe", resultSet.getBoolean("pickaxe_enabled"));
                    playerData.setToolEnabled("axe", resultSet.getBoolean("axe_enabled"));
                    playerData.setToolEnabled("shovel", resultSet.getBoolean("shovel_enabled"));
                    playerData.setToolEnabled("hoe", resultSet.getBoolean("hoe_enabled"));

                    // Load skill levels
                    playerData.setEfficiencyLevel(resultSet.getInt("efficiency_level"));
                    playerData.setLuckLevel(resultSet.getInt("luck_level"));
                    playerData.setEnergyLevel(resultSet.getInt("energy_level"));

                    plugin.getLogger().info("Loaded player data for " + player.getName() +
                            ": Level=" + playerData.getLevel() +
                            ", XP=" + playerData.getExperience() +
                            ", Blocks Mined=" + playerData.getBlocksMined());
                } else {
                    // Player not found in database, create a new entry
                    sql = "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() +
                            "player_data (uuid, player_name, enabled, level, experience, blocks_mined, " +
                            "pickaxe_enabled, axe_enabled, shovel_enabled, hoe_enabled, " +
                            "efficiency_level, luck_level, energy_level) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    PreparedStatement insertStatement = connection.prepareStatement(sql);
                    insertStatement.setString(1, playerUUID.toString());
                    insertStatement.setString(2, player.getName());
                    insertStatement.setBoolean(3, playerData.isVeinMinerEnabled());
                    insertStatement.setInt(4, playerData.getLevel());
                    insertStatement.setInt(5, playerData.getExperience());
                    insertStatement.setInt(6, playerData.getBlocksMined());
                    insertStatement.setBoolean(7, playerData.isToolEnabled("pickaxe"));
                    insertStatement.setBoolean(8, playerData.isToolEnabled("axe"));
                    insertStatement.setBoolean(9, playerData.isToolEnabled("shovel"));
                    insertStatement.setBoolean(10, playerData.isToolEnabled("hoe"));
                    insertStatement.setInt(11, playerData.getEfficiencyLevel());
                    insertStatement.setInt(12, playerData.getLuckLevel());
                    insertStatement.setInt(13, playerData.getEnergyLevel());

                    insertStatement.executeUpdate();
                    insertStatement.close();

                    plugin.getLogger().info("Created new player data for " + player.getName());
                }

                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load player data for " + player.getName() + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        // Store player data
        playerDataMap.put(playerUUID, playerData);
    }

    /**
     * Save player data to the database
     * @param player The player
     */
    public void savePlayerData(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Check if player data is loaded
        if (!playerDataMap.containsKey(playerUUID)) {
            return;
        }

        PlayerData playerData = playerDataMap.get(playerUUID);

        // Try to save to database
        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            if (connection != null) {
                String sql = "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + "player_data " +
                        "(uuid, player_name, enabled, level, experience, blocks_mined, " +
                        "pickaxe_enabled, axe_enabled, shovel_enabled, hoe_enabled, " +
                        "efficiency_level, luck_level, energy_level) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "player_name = ?, enabled = ?, level = ?, experience = ?, blocks_mined = ?, " +
                        "pickaxe_enabled = ?, axe_enabled = ?, shovel_enabled = ?, hoe_enabled = ?, " +
                        "efficiency_level = ?, luck_level = ?, energy_level = ?";

                PreparedStatement statement = connection.prepareStatement(sql);

                // Insert values
                statement.setString(1, playerUUID.toString());
                statement.setString(2, player.getName());
                statement.setBoolean(3, playerData.isVeinMinerEnabled());
                statement.setInt(4, playerData.getLevel());
                statement.setInt(5, playerData.getExperience());
                statement.setInt(6, playerData.getBlocksMined());
                statement.setBoolean(7, playerData.isToolEnabled("pickaxe"));
                statement.setBoolean(8, playerData.isToolEnabled("axe"));
                statement.setBoolean(9, playerData.isToolEnabled("shovel"));
                statement.setBoolean(10, playerData.isToolEnabled("hoe"));
                statement.setInt(11, playerData.getEfficiencyLevel());
                statement.setInt(12, playerData.getLuckLevel());
                statement.setInt(13, playerData.getEnergyLevel());

                // Update values
                statement.setString(14, player.getName());
                statement.setBoolean(15, playerData.isVeinMinerEnabled());
                statement.setInt(16, playerData.getLevel());
                statement.setInt(17, playerData.getExperience());
                statement.setInt(18, playerData.getBlocksMined());
                statement.setBoolean(19, playerData.isToolEnabled("pickaxe"));
                statement.setBoolean(20, playerData.isToolEnabled("axe"));
                statement.setBoolean(21, playerData.isToolEnabled("shovel"));
                statement.setBoolean(22, playerData.isToolEnabled("hoe"));
                statement.setInt(23, playerData.getEfficiencyLevel());
                statement.setInt(24, playerData.getLuckLevel());
                statement.setInt(25, playerData.getEnergyLevel());

                statement.executeUpdate();
                statement.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save player data for " + player.getName() + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Unload player data
     * @param player The player
     */
    public void unloadPlayerData(Player player) {
        playerDataMap.remove(player.getUniqueId());
    }

    /**
     * Save all player data
     */
    public void saveAllData() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            savePlayerData(player);
        }
    }

    /**
     * Get player data
     * @param playerUUID The player UUID
     * @return The player data
     */
    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataMap.get(playerUUID);
    }

    /**
     * Check if vein miner is enabled for a player
     * @param player The player
     * @return True if vein miner is enabled, false otherwise
     */
    public boolean isVeinMinerEnabled(Player player) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        return playerData != null && playerData.isVeinMinerEnabled();
    }

    /**
     * Set vein miner enabled for a player
     * @param player The player
     * @param enabled Whether vein miner is enabled
     */
    public void setVeinMinerEnabled(Player player, boolean enabled) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        if (playerData != null) {
            playerData.setVeinMinerEnabled(enabled);
        }
    }

    /**
     * Check if a tool is enabled for a player
     * @param player The player
     * @param toolType The tool type
     * @return True if the tool is enabled, false otherwise
     */
    public boolean isToolEnabled(Player player, String toolType) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        return playerData != null && playerData.isToolEnabled(toolType);
    }

    /**
     * Set a tool enabled for a player
     * @param player The player
     * @param toolType The tool type
     * @param enabled Whether the tool is enabled
     */
    public void setToolEnabled(Player player, String toolType, boolean enabled) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        if (playerData != null) {
            playerData.setToolEnabled(toolType, enabled);
        }
    }
}