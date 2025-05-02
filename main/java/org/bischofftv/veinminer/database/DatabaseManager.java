package org.bischofftv.veinminer.database;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

public class DatabaseManager {

    private final Veinminer plugin;
    private HikariDataSource dataSource;
    private boolean useMySQL;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String tablePrefix;
    private boolean fallbackMode = false;
    private Connection fallbackConnection = null;
    private long lastSyncTime = 0;
    private boolean reduceLogging;

    public DatabaseManager(Veinminer plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the database manager
     */
    public void initialize() {
        // Load database settings from config
        useMySQL = plugin.getConfig().getBoolean("database.use-mysql", false);
        host = plugin.getConfig().getString("database.host", "localhost");
        port = plugin.getConfig().getInt("database.port", 3306);
        database = plugin.getConfig().getString("database.name", "veinminer");
        username = plugin.getConfig().getString("database.username", "root");
        password = plugin.getConfig().getString("database.password", "");
        tablePrefix = plugin.getConfig().getString("database.table-prefix", "vm_");
        reduceLogging = plugin.getConfig().getBoolean("database.reduce-logging", true);

        // Log database settings (without password)
        plugin.getLogger().info("Database settings: useMySQL=" + useMySQL + ", host=" + host + ", port=" + port + ", database=" + database + ", username=" + username + ", tablePrefix=" + tablePrefix);

        if (useMySQL) {
            // Try to connect to MySQL
            try {
                setupHikariCP();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
                plugin.getLogger().warning("Falling back to SQLite...");
                useMySQL = false;
                fallbackMode = true;
                setupSQLite();
            }
        } else {
            // Use SQLite
            setupSQLite();
        }
    }

    /**
     * Set up HikariCP connection pool for MySQL
     */
    private void setupHikariCP() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true");
        config.setUsername(username);
        config.setPassword(password);
        
        // Optimierte Pool-Einstellungen für mehrere Server
        config.setMaximumPoolSize(20); // Erhöht für mehrere Server
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000); // 5 Minuten
        config.setConnectionTimeout(10000); // 10 Sekunden
        config.setMaxLifetime(1800000); // 30 Minuten
        
        // Optimierte Einstellungen für hohe Last
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        // Verbesserte Fehlerbehandlung
        config.setLeakDetectionThreshold(30000); // 30 Sekunden
        config.setConnectionTestQuery("SELECT 1");
        
        // Server-spezifische Einstellungen
        String serverId = "server-" + System.currentTimeMillis();
        config.addDataSourceProperty("serverName", serverId);
        config.addDataSourceProperty("applicationName", "VeinMiner-" + serverId);
        
        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("HikariCP connection pool initialized successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HikariCP: " + e.getMessage(), e);
        }
    }

    /**
     * Set up SQLite connection
     */
    private void setupSQLite() {
        try {
            // Ensure the data folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdir();
            }

            // Connect to SQLite database
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/veinminer.db";
            fallbackConnection = DriverManager.getConnection(url);

            plugin.getLogger().info("Connected to SQLite database.");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to connect to SQLite database: " + e.getMessage());
            fallbackMode = true;
        }
    }

    /**
     * Create database tables
     */
    public void createTables() {
        Connection connection = null;
        Statement statement = null;

        try {
            connection = getConnection();
            if (connection == null) {
                plugin.getLogger().severe("Failed to create tables: No database connection");
                return;
            }

            // Create player_data table
            String playerDataTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "veinminer_enabled BOOLEAN DEFAULT 0, " +
                    "level INT DEFAULT 1, " +
                    "experience INT DEFAULT 0, " +
                    "blocks_mined BIGINT DEFAULT 0, " +
                    "skill_points INT DEFAULT 0, " +
                    "efficiency_level INT DEFAULT 0, " +
                    "luck_level INT DEFAULT 0, " +
                    "energy_level INT DEFAULT 0, " +
                    "pickaxe_enabled BOOLEAN DEFAULT 0, " +
                    "axe_enabled BOOLEAN DEFAULT 0, " +
                    "shovel_enabled BOOLEAN DEFAULT 0, " +
                    "hoe_enabled BOOLEAN DEFAULT 0, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            // Create achievements table
            String achievementsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "achievements (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "achievement_id VARCHAR(64) NOT NULL, " +
                    "progress INT DEFAULT 0, " +
                    "completed BOOLEAN DEFAULT 0, " +
                    "reward_claimed BOOLEAN DEFAULT 0, " +
                    "UNIQUE (uuid, achievement_id)" +
                    ")";

            // Create sync_status table for multiserver setups
            String syncStatusTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "sync_status (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "server_id VARCHAR(36) NOT NULL, " +
                    "last_sync TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE (server_id)" +
                    ")";

            // Execute create table statements
            statement = connection.createStatement();

            // SQLite doesn't support AUTO_INCREMENT or TIMESTAMP
            if (isFallbackMode()) {
                // Modify SQL for SQLite compatibility
                playerDataTable = playerDataTable.replace("TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "TEXT DEFAULT CURRENT_TIMESTAMP");
                achievementsTable = achievementsTable.replace("INT AUTO_INCREMENT PRIMARY KEY", "INTEGER PRIMARY KEY AUTOINCREMENT")
                        .replace("BOOLEAN DEFAULT 0", "INTEGER DEFAULT 0");
                syncStatusTable = syncStatusTable.replace("INT AUTO_INCREMENT PRIMARY KEY", "INTEGER PRIMARY KEY AUTOINCREMENT")
                        .replace("TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "TEXT DEFAULT CURRENT_TIMESTAMP");
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Using SQLite-compatible table structure");
                }
            }

            try {
                statement.executeUpdate(playerDataTable);
                plugin.getLogger().info("Player data table created/verified successfully");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to create player data table: " + e.getMessage());
                if (plugin.isDebugMode()) {
                    e.printStackTrace();
                }
            }

            try {
                statement.executeUpdate(achievementsTable);
                plugin.getLogger().info("Achievements table created/verified successfully");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to create achievements table: " + e.getMessage());
                if (plugin.isDebugMode()) {
                    e.printStackTrace();
                }
            }

            try {
                statement.executeUpdate(syncStatusTable);
                plugin.getLogger().info("Sync status table created/verified successfully");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to create sync status table: " + e.getMessage());
                if (plugin.isDebugMode()) {
                    e.printStackTrace();
                }
            }

            plugin.getLogger().info("Database tables created/verified.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
        } finally {
            // Close resources
            closeResourcesInternal(null, statement, connection);
        }
    }

    /**
     * Update database schema if needed
     */
    public void updateDatabaseSchema() {
        Connection connection = null;

        try {
            connection = getConnection();
            if (connection == null) {
                plugin.getLogger().severe("Failed to update database schema: No database connection");
                return;
            }

            // Check if columns exist and add them if they don't
            checkAndAddColumn(connection, tablePrefix + "player_data", "skill_points", "INT DEFAULT 0");
            checkAndAddColumn(connection, tablePrefix + "player_data", "efficiency_level", "INT DEFAULT 0");
            checkAndAddColumn(connection, tablePrefix + "player_data", "luck_level", "INT DEFAULT 0");
            checkAndAddColumn(connection, tablePrefix + "player_data", "energy_level", "INT DEFAULT 0");
            checkAndAddColumn(connection, tablePrefix + "player_data", "last_updated", isFallbackMode() ? "TEXT DEFAULT CURRENT_TIMESTAMP" : "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            plugin.getLogger().info("Database schema updated if needed.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update database schema: " + e.getMessage());
        } finally {
            // Close resources
            closeResourcesInternal(null, null, connection);
        }
    }

    /**
     * Check if a column exists in a table and add it if it doesn't
     * @param connection The database connection
     * @param table The table name
     * @param column The column name
     * @param definition The column definition
     * @throws SQLException If an SQL error occurs
     */
    private void checkAndAddColumn(Connection connection, String table, String column, String definition) throws SQLException {
        // Check if column exists
        boolean columnExists = false;
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        Statement alterStatement = null;

        try {
            if (isFallbackMode()) {
                // SQLite
                resultSet = connection.getMetaData().getColumns(null, null, table, column);
            } else {
                // MySQL
                String sql = "SHOW COLUMNS FROM " + table + " LIKE ?";
                statement = connection.prepareStatement(sql);
                statement.setString(1, column);
                resultSet = statement.executeQuery();
            }

            columnExists = resultSet.next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check if column exists: " + e.getMessage());
        } finally {
            closeResourcesInternal(resultSet, statement, null);
        }

        // Add column if it doesn't exist
        if (!columnExists) {
            try {
                String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
                alterStatement = connection.createStatement();
                alterStatement.executeUpdate(sql);
                plugin.getLogger().info("Added column " + column + " to table " + table);
            } finally {
                if (alterStatement != null) {
                    alterStatement.close();
                }
            }
        }
    }

    /**
     * Get a database connection
     * @return A database connection, or null if an error occurs
     */
    public Connection getConnection() {
        if (isFallbackMode()) {
            return fallbackConnection;
        }

        try {
            // Setze einen kürzeren Timeout für das Abrufen einer Verbindung
            Connection connection = dataSource.getConnection();

            // Setze einen Timeout für Datenbankoperationen
            connection.setNetworkTimeout(java.util.concurrent.Executors.newSingleThreadExecutor(),
                    5000); // 5 Sekunden Timeout

            return connection;
        } catch (SQLException e) {
            if (!reduceLogging) {
                plugin.getLogger().severe("Failed to get database connection: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Check if the database connection is valid without opening a new connection
     * @return True if the connection is valid, false otherwise
     */
    public boolean isConnectionValid() {
        if (isFallbackMode()) {
            try {
                return fallbackConnection != null && !fallbackConnection.isClosed() && fallbackConnection.isValid(2);
            } catch (SQLException e) {
                if (!reduceLogging) {
                    plugin.getLogger().warning("Failed to check fallback connection validity: " + e.getMessage());
                }
                return false;
            }
        } else {
            try {
                // Prüfe, ob der DataSource verfügbar ist
                if (dataSource == null || dataSource.isClosed()) {
                    return false;
                }

                // Teste eine Verbindung mit kurzem Timeout
                Connection testConnection = null;
                try {
                    testConnection = dataSource.getConnection();
                    boolean isValid = testConnection != null && testConnection.isValid(2);
                    return isValid;
                } finally {
                    if (testConnection != null) {
                        try {
                            testConnection.close();
                        } catch (SQLException e) {
                            if (!reduceLogging) {
                                plugin.getLogger().warning("Failed to close test connection: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                if (!reduceLogging) {
                    plugin.getLogger().warning("Failed to check connection validity: " + e.getMessage());
                }
                return false;
            }
        }
    }

    /**
     * Check if the database connection is valid
     * @return True if the connection is valid, false otherwise
     */
    public boolean checkConnection() {
        // Zuerst prüfen, ob wir den Status ohne neue Verbindung prüfen können
        if (isConnectionValid()) {
            return true;
        }

        Connection connection = null;
        try {
            connection = getConnection();
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            if (!reduceLogging) {
                plugin.getLogger().severe("Database connection check failed: " + e.getMessage());
            }
            return false;
        } finally {
            // Wichtig: Verbindung immer schließen
            if (connection != null && !isFallbackMode()) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    if (!reduceLogging) {
                        plugin.getLogger().warning("Failed to close connection in checkConnection: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Close the database connection
     */
    public void close() {
        if (isFallbackMode()) {
            try {
                if (fallbackConnection != null && !fallbackConnection.isClosed()) {
                    fallbackConnection.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to close SQLite connection: " + e.getMessage());
            }
        } else {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        }
    }

    /**
     * Check if the database manager is in fallback mode (SQLite)
     * @return True if in fallback mode, false otherwise
     */
    public boolean isFallbackMode() {
        return fallbackMode || !useMySQL;
    }

    /**
     * Get the table prefix
     * @return The table prefix
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /**
     * Synchronize data with other servers
     * This is called periodically to check for updates from other servers
     */
    public void synchronizeData() {
        // Only synchronize if using MySQL and not in fallback mode
        if (isFallbackMode()) {
            return;
        }

        // Don't synchronize too frequently
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSyncTime < TimeUnit.SECONDS.toMillis(10)) {
            return;
        }

        lastSyncTime = currentTime;

        // Update our server's sync status
        updateSyncStatus();

        // No need to do anything else - each server will load the latest data when needed
    }

    /**
     * Force synchronization now
     * This is called when a player uses the /vmsync command
     */
    public void forceSyncNow() {
        // Only synchronize if using MySQL and not in fallback mode
        if (isFallbackMode()) {
            return;
        }

        // Update our server's sync status
        updateSyncStatus();

        // No need to do anything else - each server will load the latest data when needed
    }

    /**
     * Update this server's sync status
     */
    private void updateSyncStatus() {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = getConnection();
            if (connection == null) {
                return;
            }

            // Get server ID
            String serverId = plugin.getBStatsServerUUID();

            // Update or insert sync status
            String sql = "INSERT INTO " + tablePrefix + "sync_status (server_id, last_sync) VALUES (?, NOW()) " +
                    "ON DUPLICATE KEY UPDATE last_sync = NOW()";

            // SQLite doesn't support ON DUPLICATE KEY UPDATE
            if (isFallbackMode()) {
                sql = "INSERT OR REPLACE INTO " + tablePrefix + "sync_status (server_id, last_sync) VALUES (?, datetime('now'))";
            }

            statement = connection.prepareStatement(sql);
            statement.setString(1, serverId);
            statement.executeUpdate();
        } catch (SQLException e) {
            if (!reduceLogging) {
                plugin.getLogger().warning("Failed to update sync status: " + e.getMessage());
            }
        } finally {
            closeResourcesInternal(null, statement, connection);
        }
    }

    /**
     * Debug method to dump player data from the database
     * @param uuid The player UUID
     * @return A string with the player data
     */
    public String dumpPlayerData(String uuid) {
        StringBuilder result = new StringBuilder();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            if (connection == null) {
                return "Failed to get database connection";
            }

            String sql = "SELECT * FROM " + tablePrefix + "player_data WHERE uuid = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, uuid);

            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                result.append("Player data for ").append(resultSet.getString("player_name")).append(":\n");
                result.append("- UUID: ").append(resultSet.getString("uuid")).append("\n");
                result.append("- VeinMiner Enabled: ").append(resultSet.getBoolean("veinminer_enabled")).append("\n");
                result.append("- Level: ").append(resultSet.getInt("level")).append("\n");
                result.append("- Experience: ").append(resultSet.getInt("experience")).append("\n");
                result.append("- Blocks Mined: ").append(resultSet.getLong("blocks_mined")).append("\n");
                result.append("- Skill Points: ").append(resultSet.getInt("skill_points")).append("\n");
                result.append("- Efficiency Level: ").append(resultSet.getInt("efficiency_level")).append("\n");
                result.append("- Luck Level: ").append(resultSet.getInt("luck_level")).append("\n");
                result.append("- Energy Level: ").append(resultSet.getInt("energy_level")).append("\n");
                result.append("- Pickaxe Enabled: ").append(resultSet.getBoolean("pickaxe_enabled")).append("\n");
                result.append("- Axe Enabled: ").append(resultSet.getBoolean("axe_enabled")).append("\n");
                result.append("- Shovel Enabled: ").append(resultSet.getBoolean("shovel_enabled")).append("\n");
                result.append("- Hoe Enabled: ").append(resultSet.getBoolean("hoe_enabled")).append("\n");
                result.append("- Last Updated: ").append(resultSet.getString("last_updated")).append("\n");
            } else {
                result.append("No player data found for UUID: ").append(uuid);
            }
            return result.toString();
        } catch (SQLException e) {
            result.append("Error retrieving player data: ").append(e.getMessage());
            return result.toString();
        } finally {
            closeResourcesInternal(resultSet, statement, connection);
        }
    }

    /**
     * Helper method to close database resources (public version for use by other classes)
     * @param resultSet The result set to close
     * @param statement The statement to close
     * @param connection The connection to close
     */
    public void closeResources(ResultSet resultSet, Statement statement, Connection connection) {
        closeResourcesInternal(resultSet, statement, connection);
    }

    /**
     * Helper method to close database resources (private version)
     * @param resultSet The result set to close
     * @param statement The statement to close
     * @param connection The connection to close
     */
    private void closeResourcesInternal(ResultSet resultSet, Statement statement, Connection connection) {
        // Schließe in umgekehrter Reihenfolge der Erstellung
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to close ResultSet: " + e.getMessage());
            }
        }

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to close Statement: " + e.getMessage());
            }
        }

        if (connection != null && !isFallbackMode()) {
            try {
                if (!connection.isClosed()) {
                    if (!connection.getAutoCommit()) {
                        try {
                            connection.rollback();
                        } catch (SQLException e) {
                            plugin.getLogger().warning("Failed to rollback transaction: " + e.getMessage());
                        }
                    }
                    connection.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to close Connection: " + e.getMessage());
            }
        }
    }

    /**
     * Execute a database operation within a transaction
     * @param operation The database operation to execute
     * @param <T> The return type of the operation
     * @return The result of the operation
     * @throws SQLException If the operation fails
     */
    public <T> T executeInTransaction(DatabaseOperation<T> operation) throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            if (connection == null) {
                throw new SQLException("Failed to get database connection");
            }

            // Start transaction
            connection.setAutoCommit(false);
            
            try {
                // Execute operation
                T result = operation.execute(connection);
                
                // Commit transaction
                connection.commit();
                return result;
            } catch (SQLException e) {
                // Rollback on error
                connection.rollback();
                throw e;
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    plugin.getLogger().warning("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Functional interface for database operations
     * @param <T> The return type of the operation
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(Connection connection) throws SQLException;
    }

    /**
     * Save player data to the database
     * @param uuid The player UUID
     * @param playerData The player data to save
     */
    public void savePlayerData(UUID uuid, PlayerData playerData) {
        if (playerData == null) {
            plugin.getLogger().warning("Attempted to save null player data for UUID: " + uuid);
            return;
        }
        
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            if (connection == null) {
                plugin.getLogger().warning("Failed to save player data: No database connection");
                return;
            }
            
            // Check if player exists
            String checkSql = "SELECT uuid FROM " + tablePrefix + "player_data WHERE uuid = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkSql);
            checkStatement.setString(1, uuid.toString());
            ResultSet resultSet = checkStatement.executeQuery();
            
            String sql;
            if (resultSet.next()) {
                // Update existing player
                sql = "UPDATE " + tablePrefix + "player_data SET " +
                        "player_name = ?, " +
                        "veinminer_enabled = ?, " +
                        "level = ?, " +
                        "experience = ?, " +
                        "blocks_mined = ?, " +
                        "skill_points = ?, " +
                        "efficiency_level = ?, " +
                        "luck_level = ?, " +
                        "energy_level = ?, " +
                        "pickaxe_enabled = ?, " +
                        "axe_enabled = ?, " +
                        "shovel_enabled = ?, " +
                        "hoe_enabled = ?, " +
                        "last_updated = " + (isFallbackMode() ? "datetime('now')" : "NOW()") + " " +
                        "WHERE uuid = ?";
            } else {
                // Insert new player
                sql = "INSERT INTO " + tablePrefix + "player_data " +
                        "(uuid, player_name, veinminer_enabled, level, experience, blocks_mined, " +
                        "skill_points, efficiency_level, luck_level, energy_level, " +
                        "pickaxe_enabled, axe_enabled, shovel_enabled, hoe_enabled) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            }
            
            statement = connection.prepareStatement(sql);
            
            if (resultSet.next()) {
                // Update existing player
                statement.setString(1, playerData.getPlayerName());
                statement.setBoolean(2, playerData.isVeinMinerEnabled());
                statement.setInt(3, playerData.getLevel());
                statement.setInt(4, playerData.getExperience());
                statement.setLong(5, playerData.getBlocksMined());
                statement.setInt(6, playerData.getSkillPoints());
                statement.setInt(7, playerData.getEfficiencyLevel());
                statement.setInt(8, playerData.getLuckLevel());
                statement.setInt(9, playerData.getEnergyLevel());
                statement.setBoolean(10, playerData.isToolEnabled("PICKAXE"));
                statement.setBoolean(11, playerData.isToolEnabled("AXE"));
                statement.setBoolean(12, playerData.isToolEnabled("SHOVEL"));
                statement.setBoolean(13, playerData.isToolEnabled("HOE"));
                statement.setString(14, uuid.toString());
            } else {
                // Insert new player
                statement.setString(1, uuid.toString());
                statement.setString(2, playerData.getPlayerName());
                statement.setBoolean(3, playerData.isVeinMinerEnabled());
                statement.setInt(4, playerData.getLevel());
                statement.setInt(5, playerData.getExperience());
                statement.setLong(6, playerData.getBlocksMined());
                statement.setInt(7, playerData.getSkillPoints());
                statement.setInt(8, playerData.getEfficiencyLevel());
                statement.setInt(9, playerData.getLuckLevel());
                statement.setInt(10, playerData.getEnergyLevel());
                statement.setBoolean(11, playerData.isToolEnabled("PICKAXE"));
                statement.setBoolean(12, playerData.isToolEnabled("AXE"));
                statement.setBoolean(13, playerData.isToolEnabled("SHOVEL"));
                statement.setBoolean(14, playerData.isToolEnabled("HOE"));
            }
            
            statement.executeUpdate();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Saved player data for " + playerData.getPlayerName());
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
        } finally {
            closeResourcesInternal(null, statement, connection);
        }
    }
    
    /**
     * Load player data from the database
     * @param uuid The player UUID
     * @return The player data, or null if not found
     */
    public PlayerData loadPlayerData(UUID uuid) {
        if (uuid == null) {
            plugin.getLogger().warning("Attempted to load player data for null UUID");
            return null;
        }
        
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            if (connection == null) {
                plugin.getLogger().warning("Failed to load player data: No database connection");
                return null;
            }
            
            String sql = "SELECT * FROM " + tablePrefix + "player_data WHERE uuid = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, uuid.toString());
            
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                PlayerData playerData = new PlayerData(uuid, resultSet.getString("player_name"));
                playerData.setVeinMinerEnabled(resultSet.getBoolean("veinminer_enabled"));
                playerData.setLevel(resultSet.getInt("level"));
                playerData.setExperience(resultSet.getInt("experience"));
                playerData.setBlocksMined(resultSet.getLong("blocks_mined"));
                playerData.setSkillPoints(resultSet.getInt("skill_points"));
                playerData.setEfficiencyLevel(resultSet.getInt("efficiency_level"));
                playerData.setLuckLevel(resultSet.getInt("luck_level"));
                playerData.setEnergyLevel(resultSet.getInt("energy_level"));
                playerData.setToolEnabled("PICKAXE", resultSet.getBoolean("pickaxe_enabled"));
                playerData.setToolEnabled("AXE", resultSet.getBoolean("axe_enabled"));
                playerData.setToolEnabled("SHOVEL", resultSet.getBoolean("shovel_enabled"));
                playerData.setToolEnabled("HOE", resultSet.getBoolean("hoe_enabled"));
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Loaded player data for " + playerData.getPlayerName());
                }
                
                return playerData;
            } else {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("No player data found for UUID: " + uuid);
                }
                return null;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
            return null;
        } finally {
            closeResourcesInternal(resultSet, statement, connection);
        }
    }
}