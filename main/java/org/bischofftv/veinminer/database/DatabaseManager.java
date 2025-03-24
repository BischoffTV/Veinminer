package org.bischofftv.veinminer.database;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class DatabaseManager {

    private final Veinminer plugin;
    private Connection connection;
    private boolean fallbackMode;
    private String host, database, username, password;
    private int port;
    private String tablePrefix;
    private boolean useMySQL;

    public DatabaseManager(Veinminer plugin) {
        this.plugin = plugin;
        this.fallbackMode = false;
    }

    /**
     * Initialize the database connection
     */
    public void initialize() {
        // Load database settings from config
        useMySQL = plugin.getConfig().getBoolean("database.use-mysql", false);
        host = plugin.getConfig().getString("database.host", "localhost");
        port = plugin.getConfig().getInt("database.port", 3306);

        // Get database name - check both possible config paths
        if (plugin.getConfig().isString("database.name")) {
            database = plugin.getConfig().getString("database.name", "veinminer");
        } else {
            database = "veinminer"; // Default value
            plugin.getLogger().warning("Database name not found in config. Using default: veinminer");
        }

        username = plugin.getConfig().getString("database.username", "root");
        password = plugin.getConfig().getString("database.password", "");
        tablePrefix = plugin.getConfig().getString("database.table-prefix", "vm_");

        plugin.getLogger().info("Database settings: useMySQL=" + useMySQL + ", host=" + host + ", port=" + port +
                ", database=" + database + ", username=" + username + ", tablePrefix=" + tablePrefix);

        if (useMySQL) {
            // Try to connect to MySQL
            try {
                // Try to create the database if it doesn't exist
                tryCreateDatabase();

                // Use the newer driver class name
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true";
                plugin.getLogger().info("Attempting to connect to MySQL database: " + url);
                connection = DriverManager.getConnection(url, username, password);
                plugin.getLogger().info("Connected to MySQL database.");
                fallbackMode = false;
                return; // Successfully connected to MySQL, so return early
            } catch (ClassNotFoundException e) {
                // Try the older driver class name
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
                    plugin.getLogger().info("Attempting to connect to MySQL database with legacy driver: " + url);
                    connection = DriverManager.getConnection(url, username, password);
                    plugin.getLogger().info("Connected to MySQL database with legacy driver.");
                    fallbackMode = false;
                    return; // Successfully connected to MySQL, so return early
                } catch (ClassNotFoundException | SQLException ex) {
                    plugin.getLogger().severe("Failed to connect to MySQL database with legacy driver: " + ex.getMessage());
                    plugin.getLogger().severe("Falling back to SQLite database.");
                    fallbackMode = true;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
                plugin.getLogger().severe("Falling back to SQLite database.");
                fallbackMode = true;
            }
        } else {
            fallbackMode = true;
        }

        if (fallbackMode) {
            // Use SQLite as fallback
            try {
                Class.forName("org.sqlite.JDBC");
                String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/veinminer.db";
                connection = DriverManager.getConnection(url);
                plugin.getLogger().info("Connected to SQLite database.");
            } catch (ClassNotFoundException | SQLException e) {
                plugin.getLogger().severe("Failed to connect to SQLite database: " + e.getMessage());
                plugin.getLogger().severe("Plugin will continue without database support.");
                connection = null;
            }
        }
    }

    /**
     * Try to create the database if it doesn't exist
     */
    private void tryCreateDatabase() {
        String url = "jdbc:mysql://" + host + ":" + port + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true";
        plugin.getLogger().info("Trying to connect to MySQL server to check/create database: " + url);

        try (Connection rootConnection = DriverManager.getConnection(url, username, password);
             Statement stmt = rootConnection.createStatement()) {

            plugin.getLogger().info("Connected to MySQL server. Checking if database exists...");

            // Check if database exists
            ResultSet rs = stmt.executeQuery("SHOW DATABASES LIKE '" + database + "'");
            if (!rs.next()) {
                // Database doesn't exist, create it
                plugin.getLogger().info("Database '" + database + "' does not exist. Creating...");
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                plugin.getLogger().info("Database '" + database + "' created successfully!");
            } else {
                plugin.getLogger().info("Database '" + database + "' already exists.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database: " + e.getMessage());
            plugin.getLogger().severe("Please make sure your MySQL user has CREATE DATABASE privileges or create the database manually.");
        }
    }

    // Update the createTables method to add the claimed column
    public void createTables() {
        if (connection == null) {
            return;
        }

        try {
            Statement statement = connection.createStatement();

            // Player data table
            String playerDataTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "enabled BOOLEAN DEFAULT TRUE, " +
                    "level INT DEFAULT 1, " +
                    "experience INT DEFAULT 0, " +
                    "blocks_mined INT DEFAULT 0, " +
                    "pickaxe_enabled BOOLEAN DEFAULT TRUE, " +
                    "axe_enabled BOOLEAN DEFAULT TRUE, " +
                    "shovel_enabled BOOLEAN DEFAULT TRUE, " +
                    "hoe_enabled BOOLEAN DEFAULT TRUE, " +
                    "efficiency_level INT DEFAULT 0, " +
                    "luck_level INT DEFAULT 0, " +
                    "energy_level INT DEFAULT 0" +
                    ")";
            statement.executeUpdate(playerDataTable);

            // Achievements table
            String achievementsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "achievements (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "achievement_id VARCHAR(32) NOT NULL, " +
                    "progress INT DEFAULT 0, " +
                    "completed BOOLEAN DEFAULT FALSE, " +
                    "claimed BOOLEAN DEFAULT FALSE, " +
                    "PRIMARY KEY (uuid, achievement_id)" +
                    ")";
            statement.executeUpdate(achievementsTable);

            // Sync table for multi-server setups
            String syncTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "sync (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "timestamp BIGINT NOT NULL, " +
                    "server_id VARCHAR(36) NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "data_type VARCHAR(32) NOT NULL, " +
                    "data_key VARCHAR(64) NOT NULL, " +
                    "data_value TEXT" +
                    ")";
            statement.executeUpdate(syncTable);

            statement.close();
            plugin.getLogger().info("Database tables created successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    // Update the updateDatabaseSchema method to add the claimed column if it doesn't exist
    public void updateDatabaseSchema() {
        if (connection == null) {
            return;
        }

        try {
            // Check if skills columns exist, if not add them
            if (!fallbackMode) {
                // For MySQL
                Statement statement = connection.createStatement();

                // Check if efficiency_level column exists
                try {
                    statement.executeUpdate("ALTER TABLE " + tablePrefix + "player_data ADD COLUMN efficiency_level INT DEFAULT 0");
                    plugin.getLogger().info("Added efficiency_level column to player_data table.");
                } catch (SQLException e) {
                    // Column already exists
                }

                // Check if luck_level column exists
                try {
                    statement.executeUpdate("ALTER TABLE " + tablePrefix + "player_data ADD COLUMN luck_level INT DEFAULT 0");
                    plugin.getLogger().info("Added luck_level column to player_data table.");
                } catch (SQLException e) {
                    // Column already exists
                }

                // Check if energy_level column exists
                try {
                    statement.executeUpdate("ALTER TABLE " + tablePrefix + "player_data ADD COLUMN energy_level INT DEFAULT 0");
                    plugin.getLogger().info("Added energy_level column to player_data table.");
                } catch (SQLException e) {
                    // Column already exists
                }

                // Check if claimed column exists in achievements table
                try {
                    statement.executeUpdate("ALTER TABLE " + tablePrefix + "achievements ADD COLUMN claimed BOOLEAN DEFAULT FALSE");
                    plugin.getLogger().info("Added claimed column to achievements table.");
                } catch (SQLException e) {
                    // Column already exists
                }

                statement.close();
            } else {
                // For SQLite
                // SQLite doesn't support ALTER TABLE ADD COLUMN IF NOT EXISTS
                // So we need to check if the column exists first

                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tablePrefix + "player_data)");

                boolean hasEfficiencyLevel = false;
                boolean hasLuckLevel = false;
                boolean hasEnergyLevel = false;

                while (resultSet.next()) {
                    String columnName = resultSet.getString("name");
                    if (columnName.equals("efficiency_level")) {
                        hasEfficiencyLevel = true;
                    } else if (columnName.equals("luck_level")) {
                        hasLuckLevel = true;
                    } else if (columnName.equals("energy_level")) {
                        hasEnergyLevel = true;
                    }
                }

                resultSet.close();

                // Add missing columns
                if (!hasEfficiencyLevel) {
                    statement.executeUpdate("ALTER TABLE " + tablePrefix + "player_data ADD COLUMN efficiency_level INT DEFAULT 0");
                    plugin.getLogger().info("Added efficiency_level column to player_data table.");
                }

                if (!hasLuckLevel) {
                    statement.executeUpdate("ALTER TABLE " + tablePrefix + "player_data ADD COLUMN luck_level INT DEFAULT 0");
                    plugin.getLogger().info("Added luck_level column to player_data table.");
                }

                if (!hasEnergyLevel) {
                    statement.executeUpdate("ALTER TABLE " + tablePrefix + "player_data ADD COLUMN energy_level INT DEFAULT 0");
                    plugin.getLogger().info("Added energy_level column to player_data table.");
                }

                // Check if claimed column exists in achievements table
                ResultSet achievementsResultSet = statement.executeQuery("PRAGMA table_info(" + tablePrefix + "achievements)");

                boolean hasClaimedColumn = false;

                while (achievementsResultSet.next()) {
                    String columnName = achievementsResultSet.getString("name");
                    if (columnName.equals("claimed")) {
                        hasClaimedColumn = true;
                        break;
                    }
                }

                achievementsResultSet.close();

                if (!hasClaimedColumn) {
                    statement.executeUpdate("ALTER TABLE " + tablePrefix + "achievements ADD COLUMN claimed BOOLEAN DEFAULT FALSE");
                    plugin.getLogger().info("Added claimed column to achievements table.");
                }

                statement.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update database schema: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    // Add method to update achievement schema for new achievements
    public void updateAchievementSchema() {
        if (connection == null) {
            return;
        }

        // This method ensures all defined achievements have entries in the database
        try {
            // Get all achievement IDs from the plugin's achievement definitions
            Set<String> definedAchievements = plugin.getAchievementManager().getAchievementDefinitions().keySet();

            plugin.getLogger().info("Updating achievement schema with " + definedAchievements.size() + " defined achievements");
            for (String achievementId : definedAchievements) {
                plugin.getLogger().info("Found achievement in config: " + achievementId);
            }

            // For online players, ensure they have entries for all defined achievements
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID playerUUID = player.getUniqueId();

                // Get the player's existing achievements from database directly
                Map<String, Integer> playerAchievements = new HashMap<>();

                String sql = "SELECT achievement_id FROM " + tablePrefix + "achievements WHERE uuid = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, playerUUID.toString());
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String achievementId = resultSet.getString("achievement_id");
                    playerAchievements.put(achievementId, 0); // We only need the keys here
                }

                resultSet.close();
                statement.close();

                plugin.getLogger().info("Player " + player.getName() + " has " + playerAchievements.size() + " achievements in database");

                // For each defined achievement, ensure the player has an entry
                for (String achievementId : definedAchievements) {
                    if (!playerAchievements.containsKey(achievementId)) {
                        // Add a default entry for this achievement
                        String insertSql;

                        if (!fallbackMode) {
                            // MySQL syntax
                            insertSql = "INSERT IGNORE INTO " + tablePrefix + "achievements " +
                                    "(uuid, achievement_id, progress, completed, claimed) " +
                                    "VALUES (?, ?, 0, 0, 0)";
                        } else {
                            // SQLite syntax
                            insertSql = "INSERT OR IGNORE INTO " + tablePrefix + "achievements " +
                                    "(uuid, achievement_id, progress, completed, claimed) " +
                                    "VALUES (?, ?, 0, 0, 0)";
                        }

                        PreparedStatement insertStatement = connection.prepareStatement(insertSql);
                        insertStatement.setString(1, playerUUID.toString());
                        insertStatement.setString(2, achievementId);
                        insertStatement.executeUpdate();
                        insertStatement.close();

                        plugin.getLogger().info("Added new achievement " + achievementId + " for player " + player.getName());
                    }
                }
            }

            plugin.getLogger().info("Achievement schema updated for online players");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update achievement schema: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get the database connection
     * @return The database connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Close the database connection
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
            }
        }
    }

    /**
     * Check if the database is in fallback mode
     * @return True if in fallback mode, false otherwise
     */
    public boolean isFallbackMode() {
        return fallbackMode;
    }

    /**
     * Get the table prefix
     * @return The table prefix
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /**
     * Synchronize data across servers
     */
    public void synchronizeData() {
        if (connection == null || fallbackMode) {
            return;
        }

        try {
            // Check for updates from other servers
            String sql = "SELECT * FROM " + tablePrefix + "sync WHERE server_id != ? AND timestamp > ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, plugin.getBStatsServerUUID());
            statement.setLong(2, System.currentTimeMillis() - 60000); // Last minute

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String playerUUID = resultSet.getString("player_uuid");
                String dataType = resultSet.getString("data_type");
                String dataKey = resultSet.getString("data_key");
                String dataValue = resultSet.getString("data_value");

                // Process the update
                processUpdate(playerUUID, dataType, dataKey, dataValue);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to synchronize data: " + e.getMessage());
        }
    }

    /**
     * Process an update from another server
     * @param playerUUID The player UUID
     * @param dataType The data type
     * @param dataKey The data key
     * @param dataValue The data value
     */
    private void processUpdate(String playerUUID, String dataType, String dataKey, String dataValue) {
        // Implementation depends on the data type
        // For example, if dataType is "achievement", update the achievement progress
    }

    /**
     * Force synchronization of data
     */
    public void forceSyncNow() {
        synchronizeData();
    }

    /**
     * Notify other servers about an achievement update
     * @param player The player
     * @param achievementId The achievement ID
     * @param progress The progress
     */
    public void notifyAchievementUpdate(Player player, String achievementId, int progress) {
        if (connection == null || fallbackMode) {
            return;
        }

        try {
            String sql = "INSERT INTO " + tablePrefix + "sync (timestamp, server_id, player_uuid, data_type, data_key, data_value) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, plugin.getBStatsServerUUID());
            statement.setString(3, player.getUniqueId().toString());
            statement.setString(4, "achievement");
            statement.setString(5, achievementId);
            statement.setString(6, String.valueOf(progress));

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to notify achievement update: " + e.getMessage());
        }
    }

    /**
     * Load player achievements from the database
     * @param player The player
     * @return A map of achievement IDs to progress
     */
    public Map<String, Integer> loadPlayerAchievements(Player player) {
        Map<String, Integer> achievements = new HashMap<>();

        if (connection == null) {
            return achievements;
        }

        try {
            if (!checkConnection()) {
                plugin.getLogger().warning("Cannot load achievements: Database connection check failed");
                return achievements;
            }

            String sql = "SELECT * FROM " + tablePrefix + "achievements WHERE uuid = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, player.getUniqueId().toString());

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String achievementId = resultSet.getString("achievement_id");
                int progress = resultSet.getInt("progress");
                achievements.put(achievementId, progress);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load achievements for " + player.getName() + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        return achievements;
    }

    /**
     * Check database connection and reconnect if needed
     * @return true if connection is valid, false otherwise
     */
    public boolean checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().warning("Database connection lost. Attempting to reconnect...");
                initialize();
                return connection != null && !connection.isClosed();
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check database connection: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Get the local achievement cache
     * @return The local achievement cache
     */
    public Map<UUID, Map<String, Integer>> getLocalAchievementCache() {
        Map<UUID, Map<String, Integer>> cache = new HashMap<>();

        if (connection == null) {
            return cache;
        }

        try {
            String sql = "SELECT * FROM " + tablePrefix + "achievements";
            PreparedStatement statement = connection.prepareStatement(sql);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                UUID playerUUID = UUID.fromString(resultSet.getString("uuid"));
                String achievementId = resultSet.getString("achievement_id");
                int progress = resultSet.getInt("progress");

                if (!cache.containsKey(playerUUID)) {
                    cache.put(playerUUID, new HashMap<>());
                }

                cache.get(playerUUID).put(achievementId, progress);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load achievement cache: " + e.getMessage());
        }

        return cache;
    }
}