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
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        // Check if player data is already loaded
        if (playerDataMap.containsKey(uuid)) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] Player data for " + playerName + " is already loaded in memory.");
            }
            PlayerData existingData = playerDataMap.get(uuid);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] Memory data: Level=" + existingData.getLevel() +
                        ", XP=" + existingData.getExperience() +
                        ", Blocks=" + existingData.getBlocksMined());
            }
            return;
        }

        // Create new player data
        PlayerData playerData = new PlayerData(uuid, playerName);
        // Standardmäßig ist VeinMiner deaktiviert für neue Spieler
        playerData.setVeinMinerEnabled(false);
        // Standardmäßig sind alle Tools deaktiviert
        playerData.setToolEnabled("pickaxe", false);
        playerData.setToolEnabled("axe", false);
        playerData.setToolEnabled("shovel", false);
        playerData.setToolEnabled("hoe", false);

        // Try to load from database
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnectionValid()) {
            Connection connection = null;
            PreparedStatement statement = null;
            ResultSet resultSet = null;

            try {
                // Setze ein Timeout für das Abrufen der Verbindung
                long startTime = System.currentTimeMillis();
                connection = plugin.getDatabaseManager().getConnection();
                long endTime = System.currentTimeMillis();

                if (endTime - startTime > 1000) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("[DEBUG] Slow connection acquisition for " + playerName + ": " + (endTime - startTime) + "ms");
                    }
                }

                if (connection == null) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("[DEBUG] Failed to get database connection when loading player data for " + playerName);
                    }
                    playerDataMap.put(uuid, playerData);
                    return;
                }

                String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "player_data WHERE uuid = ?";
                statement = connection.prepareStatement(sql);
                statement.setString(1, uuid.toString());

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] Executing SQL query to load player data for " + playerName);
                }
                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    playerData.setVeinMinerEnabled(resultSet.getBoolean("veinminer_enabled"));
                    playerData.setLevel(resultSet.getInt("level"));
                    playerData.setExperience(resultSet.getInt("experience"));
                    playerData.setBlocksMined(resultSet.getLong("blocks_mined"));
                    playerData.setSkillPoints(resultSet.getInt("skill_points"));
                    playerData.setEfficiencyLevel(resultSet.getInt("efficiency_level"));
                    playerData.setLuckLevel(resultSet.getInt("luck_level"));
                    playerData.setEnergyLevel(resultSet.getInt("energy_level"));

                    // Load tool settings
                    playerData.setToolEnabled("pickaxe", resultSet.getBoolean("pickaxe_enabled"));
                    playerData.setToolEnabled("axe", resultSet.getBoolean("axe_enabled"));
                    playerData.setToolEnabled("shovel", resultSet.getBoolean("shovel_enabled"));
                    playerData.setToolEnabled("hoe", resultSet.getBoolean("hoe_enabled"));

                    // Add debug logging
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[DEBUG] Loaded player data from database for " + playerName +
                                ": Level=" + playerData.getLevel() +
                                ", XP=" + playerData.getExperience() +
                                ", Blocks=" + playerData.getBlocksMined());
                    }
                } else {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[DEBUG] No existing data found in database for " + playerName + ", creating new entry");
                    }

                    // Insert new player data
                    PreparedStatement insertStatement = null;
                    try {
                        sql = "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() +
                                "player_data (uuid, player_name, veinminer_enabled, level, experience, blocks_mined, " +
                                "skill_points, efficiency_level, luck_level, energy_level, " +
                                "pickaxe_enabled, axe_enabled, shovel_enabled, hoe_enabled) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                        insertStatement = connection.prepareStatement(sql);
                        insertStatement.setString(1, uuid.toString());
                        insertStatement.setString(2, playerName);
                        insertStatement.setBoolean(3, playerData.isVeinMinerEnabled());
                        insertStatement.setInt(4, playerData.getLevel());
                        insertStatement.setInt(5, playerData.getExperience());
                        insertStatement.setLong(6, playerData.getBlocksMined());
                        insertStatement.setInt(7, playerData.getSkillPoints());
                        insertStatement.setInt(8, playerData.getEfficiencyLevel());
                        insertStatement.setInt(9, playerData.getLuckLevel());
                        insertStatement.setInt(10, playerData.getEnergyLevel());
                        insertStatement.setBoolean(11, playerData.isToolEnabled("pickaxe"));
                        insertStatement.setBoolean(12, playerData.isToolEnabled("axe"));
                        insertStatement.setBoolean(13, playerData.isToolEnabled("shovel"));
                        insertStatement.setBoolean(14, playerData.isToolEnabled("hoe"));

                        insertStatement.executeUpdate();
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[DEBUG] Created new player data in database for " + playerName);
                        }
                    } finally {
                        if (insertStatement != null) {
                            try {
                                insertStatement.close();
                            } catch (SQLException e) {
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().warning("Failed to close insert statement: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Failed to load player data for " + playerName + ": " + e.getMessage());
                }
                e.printStackTrace();
            } finally {
                // Verwende die closeResources-Methode des DatabaseManagers
                plugin.getDatabaseManager().closeResources(resultSet, statement, connection);
            }
        } else {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[DEBUG] Database connection not available when loading player data for " + playerName);
            }
        }

        // Add to map
        playerDataMap.put(uuid, playerData);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Added player data to memory map for " + playerName);
        }
    }

    /**
     * Save player data to the database
     * @param uuid The player UUID
     */
    public void savePlayerData(UUID uuid) {
        PlayerData playerData = playerDataMap.get(uuid);
        if (playerData == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[DEBUG] Cannot save player data: No data found in memory for UUID " + uuid);
            }
            return;
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Saving player data for " + playerData.getPlayerName() +
                    ": Level=" + playerData.getLevel() +
                    ", XP=" + playerData.getExperience() +
                    ", Blocks=" + playerData.getBlocksMined());
        }

        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnectionValid()) {
            Connection connection = null;
            PreparedStatement statement = null;

            try {
                // Setze ein Timeout für das Abrufen der Verbindung
                long startTime = System.currentTimeMillis();
                connection = plugin.getDatabaseManager().getConnection();
                long endTime = System.currentTimeMillis();

                if (endTime - startTime > 1000) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("[DEBUG] Slow connection acquisition for " + playerData.getPlayerName() + ": " + (endTime - startTime) + "ms");
                    }
                }

                if (connection == null) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("[DEBUG] Failed to get database connection when saving player data for " + playerData.getPlayerName());
                    }
                    return;
                }

                String sql = "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + "player_data SET " +
                        "player_name = ?, veinminer_enabled = ?, level = ?, experience = ?, blocks_mined = ?, " +
                        "skill_points = ?, efficiency_level = ?, luck_level = ?, energy_level = ?, " +
                        "pickaxe_enabled = ?, axe_enabled = ?, shovel_enabled = ?, hoe_enabled = ? " +
                        "WHERE uuid = ?";

                statement = connection.prepareStatement(sql);
                statement.setString(1, playerData.getPlayerName());
                statement.setBoolean(2, playerData.isVeinMinerEnabled());
                statement.setInt(3, playerData.getLevel());
                statement.setInt(4, playerData.getExperience());
                statement.setLong(5, playerData.getBlocksMined());
                statement.setInt(6, playerData.getSkillPoints());
                statement.setInt(7, playerData.getEfficiencyLevel());
                statement.setInt(8, playerData.getLuckLevel());
                statement.setInt(9, playerData.getEnergyLevel());
                statement.setBoolean(10, playerData.isToolEnabled("pickaxe"));
                statement.setBoolean(11, playerData.isToolEnabled("axe"));
                statement.setBoolean(12, playerData.isToolEnabled("shovel"));
                statement.setBoolean(13, playerData.isToolEnabled("hoe"));
                statement.setString(14, uuid.toString());

                int rowsUpdated = statement.executeUpdate();
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] Saved player data to database for " + playerData.getPlayerName() +
                            ", rows updated: " + rowsUpdated);
                }
            } catch (SQLException e) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Failed to save player data for " + playerData.getPlayerName() + ": " + e.getMessage());
                }
                e.printStackTrace();
            } finally {
                // Verwende die closeResources-Methode des DatabaseManagers
                plugin.getDatabaseManager().closeResources(null, statement, connection);
            }
        } else {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[DEBUG] Database connection not available when saving player data for " + playerData.getPlayerName());
            }
        }
    }

    /**
     * Save all player data to the database
     */
    public void saveAllData() {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Saving all player data, count: " + playerDataMap.size());
        }
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }
    }

    /**
     * Remove player data from memory
     * @param uuid The player UUID
     */
    public void removePlayerData(UUID uuid) {
        PlayerData playerData = playerDataMap.get(uuid);
        if (playerData != null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] Removing player data from memory for " + playerData.getPlayerName());
            }
        }
        playerDataMap.remove(uuid);
    }

    /**
     * Get player data
     * @param uuid The player UUID
     * @return The player data, or null if not found
     */
    public PlayerData getPlayerData(UUID uuid) {
        PlayerData playerData = playerDataMap.get(uuid);
        if (playerData == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[DEBUG] Attempted to get player data for UUID " + uuid + " but none was found in memory");
            }
        }
        return playerData;
    }

    /**
     * Check if VeinMiner is enabled for a player
     * @param player The player
     * @return True if enabled, false otherwise
     */
    public boolean isVeinMinerEnabled(Player player) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        return playerData != null && playerData.isVeinMinerEnabled();
    }

    /**
     * Set VeinMiner enabled/disabled for a player
     * @param player The player
     * @param enabled Whether VeinMiner should be enabled
     */
    public void setVeinMinerEnabled(Player player, boolean enabled) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        if (playerData != null) {
            playerData.setVeinMinerEnabled(enabled);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] Set VeinMiner enabled=" + enabled + " for " + player.getName());
            }
        }
    }

    /**
     * Check if a tool is enabled for a player
     * @param player The player
     * @param toolType The tool type
     * @return True if enabled, false otherwise
     */
    public boolean isToolEnabled(Player player, String toolType) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        return playerData != null && playerData.isToolEnabled(toolType);
    }

    /**
     * Set a tool enabled/disabled for a player
     * @param player The player
     * @param toolType The tool type
     * @param enabled Whether the tool should be enabled
     */
    public void setToolEnabled(Player player, String toolType, boolean enabled) {
        PlayerData playerData = getPlayerData(player.getUniqueId());
        if (playerData != null) {
            playerData.setToolEnabled(toolType, enabled);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] Set tool " + toolType + " enabled=" + enabled + " for " + player.getName());
            }
        }
    }

    /**
     * Force reload player data from database
     * @param player The player
     */
    public void forceReloadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Force reloading player data for " + playerName);
        }

        // Remove existing data
        playerDataMap.remove(uuid);

        // Load fresh data
        loadPlayerData(player);

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Player data force reloaded for " + playerName);
        }
    }

    /**
     * Speichert die Einstellungen eines Spielers sofort in die Datenbank
     * @param player Der Spieler
     */
    public void savePlayerSettings(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = playerDataMap.get(uuid);
        if (playerData == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[DEBUG] Cannot save player settings: No data found in memory for " + player.getName());
            }
            return;
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Saving settings for " + playerData.getPlayerName() +
                    ": VeinMiner=" + playerData.isVeinMinerEnabled() +
                    ", Pickaxe=" + playerData.isToolEnabled("pickaxe") +
                    ", Axe=" + playerData.isToolEnabled("axe") +
                    ", Shovel=" + playerData.isToolEnabled("shovel") +
                    ", Hoe=" + playerData.isToolEnabled("hoe"));
        }

        savePlayerData(uuid);
    }

    /**
     * Test the database connection and log the result
     */
    public void testDatabaseConnection() {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Testing database connection...");
        }

        if (plugin.getDatabaseManager() == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[DEBUG] Database manager is null!");
            }
            return;
        }

        boolean isValid = plugin.getDatabaseManager().isConnectionValid();
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Database connection is valid: " + isValid);
        }

        if (isValid) {
            Connection connection = null;
            try {
                long startTime = System.currentTimeMillis();
                connection = plugin.getDatabaseManager().getConnection();
                long endTime = System.currentTimeMillis();

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] Connection acquisition time: " + (endTime - startTime) + "ms");
                }

                if (connection != null) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[DEBUG] Connection obtained successfully.");
                        plugin.getLogger().info("[DEBUG] Auto-commit: " + connection.getAutoCommit());
                        plugin.getLogger().info("[DEBUG] Transaction isolation: " + connection.getTransactionIsolation());
                    }
                } else {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("[DEBUG] Failed to get connection!");
                    }
                }
            } catch (Exception e) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().severe("[DEBUG] Error testing connection: " + e.getMessage());
                }
                e.printStackTrace();
            } finally {
                if (connection != null && !plugin.getDatabaseManager().isFallbackMode()) {
                    try {
                        connection.close();
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[DEBUG] Connection closed successfully.");
                        }
                    } catch (SQLException e) {
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().warning("[DEBUG] Failed to close connection: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}