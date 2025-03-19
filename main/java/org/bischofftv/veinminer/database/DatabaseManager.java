package org.bischofftv.veinminer.database;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.achievements.AchievementManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

// Füge die Importe für die neuen Klassen hinzu
import org.bischofftv.veinminer.database.PlayerData;
import org.bischofftv.veinminer.database.PlayerSettings;

public class DatabaseManager {

    private final Veinminer plugin;
    private HikariDataSource dataSource;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String tablePrefix;
    private final boolean debug;
    private boolean fallbackMode = false;
    private final Map<UUID, PlayerData> localPlayerDataCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Object>> localAchievementCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSettings> localSettingsCache = new ConcurrentHashMap<>();
    private final ReentrantLock connectionLock = new ReentrantLock();
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY = 5000; // 5 seconds
    private boolean useMySQL;
    private boolean schemaUpdated = false;

    public DatabaseManager(Veinminer plugin) {
        this.plugin = plugin;
        this.useMySQL = plugin.getConfig().getBoolean("database.use-mysql", true);
        this.host = plugin.getConfig().getString("database.host", "localhost");
        this.port = plugin.getConfig().getInt("database.port", 3306);
        this.database = plugin.getConfig().getString("database.database",
                plugin.getConfig().getString("database.name", "veinminer"));
        this.username = plugin.getConfig().getString("database.username", "root");
        this.password = plugin.getConfig().getString("database.password", "password");
        this.tablePrefix = plugin.getConfig().getString("database.table-prefix", "vm_");
        this.debug = plugin.getConfig().getBoolean("settings.debug", false);

        // Zeige die verwendeten Datenbankeinstellungen an
        plugin.getLogger().info("Database settings:");
        plugin.getLogger().info("- MySQL enabled: " + useMySQL);
        plugin.getLogger().info("- Host: " + host);
        plugin.getLogger().info("- Port: " + port);
        plugin.getLogger().info("- Database: " + database);
        plugin.getLogger().info("- Username: " + username);
        plugin.getLogger().info("- Table prefix: " + tablePrefix);

        // Create data directory for local storage
        createDataDirectory();
    }

    private void createDataDirectory() {
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public void initialize() {
        if (!useMySQL) {
            plugin.getLogger().info("MySQL is disabled in config. Using file-based storage.");
            fallbackMode = true;
            loadLocalData();
            return;
        }

        try {
            setupConnectionPool();

            // Don't create tables here, let the main class call createTables explicitly

            plugin.getLogger().info("Successfully connected to MySQL database!");

            // Start periodic synchronization task with a reduced frequency (10 seconds instead of 5)
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                    this::synchronizeData, 200L, 200L); // Run every 10 seconds (200 ticks)

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            enableFallbackMode("Database initialization failed: " + e.getMessage());
            if (debug) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
            } else {
                plugin.getLogger().severe("Error occurred: " + e.getMessage());
            }
        }
    }

    // Method to update database schema if needed
    public void updateDatabaseSchema() {
        if (fallbackMode || schemaUpdated) {
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) return;

            // Check if we need to update the sync_status table
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM " + tablePrefix + "sync_status")) {

                Set<String> existingColumns = new HashSet<>();
                while (rs.next()) {
                    existingColumns.add(rs.getString("Field").toLowerCase());
                }

                // Add missing columns if needed
                List<String> alterStatements = new ArrayList<>();

                if (!existingColumns.contains("sync_type")) {
                    alterStatements.add("ADD COLUMN sync_type VARCHAR(32) DEFAULT 'REGULAR'");
                }

                if (!existingColumns.contains("sync_timestamp")) {
                    alterStatements.add("ADD COLUMN sync_timestamp BIGINT DEFAULT 0");
                }

                if (!existingColumns.contains("player_uuid")) {
                    alterStatements.add("ADD COLUMN player_uuid VARCHAR(36) DEFAULT NULL");
                }

                if (!existingColumns.contains("achievement_id")) {
                    alterStatements.add("ADD COLUMN achievement_id VARCHAR(64) DEFAULT NULL");
                }

                // Execute ALTER TABLE statements if needed
                if (!alterStatements.isEmpty()) {
                    StringBuilder alterSql = new StringBuilder("ALTER TABLE " + tablePrefix + "sync_status ");
                    for (int i = 0; i < alterStatements.size(); i++) {
                        if (i > 0) {
                            alterSql.append(", ");
                        }
                        alterSql.append(alterStatements.get(i));
                    }

                    try (Statement alterStmt = conn.createStatement()) {
                        alterStmt.execute(alterSql.toString());
                        plugin.getLogger().info("Updated sync_status table schema with new columns");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Error checking sync_status table schema: " + e.getMessage());
            }

            // Mark schema as updated
            schemaUpdated = true;

        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating database schema: " + e.getMessage());
        } finally {
            safelyCloseConnection(conn);
        }
    }

    private void setupConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true" +
                "&useUnicode=true&characterEncoding=utf8");
        config.setUsername(username);
        config.setPassword(password);

        // Connection pool settings
        config.setMaximumPoolSize(5); // Reduced from 10 to 5
        config.setMinimumIdle(1);     // Reduced from 2 to 1
        config.setIdleTimeout(60000); // 1 minute
        config.setMaxLifetime(300000); // 5 minutes
        config.setConnectionTimeout(30000); // 30 seconds

        // Enable leak detection to identify connection leaks
        config.setLeakDetectionThreshold(60000); // 60 seconds

        // Add health check properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        // BungeeCord specific settings
        config.addDataSourceProperty("socketTimeout", "30000");
        config.addDataSourceProperty("connectTimeout", "10000");

        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to setup connection pool: " + e.getMessage());
            throw e;
        }
    }

    public Connection getConnection() throws SQLException {
        if (fallbackMode) {
            return null;
        }

        try {
            if (connectionLock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    if (dataSource == null || dataSource.isClosed()) {
                        reconnectAttempts++;
                        if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
                            enableFallbackMode("Max reconnection attempts reached");
                            return null;
                        }
                        plugin.getLogger().warning("Attempting to reconnect to database (Attempt " +
                                reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")");
                        setupConnectionPool();
                        Thread.sleep(RECONNECT_DELAY);
                    }

                    Connection conn = dataSource.getConnection();
                    if (conn.isValid(5)) { // Test connection with 5 second timeout
                        reconnectAttempts = 0; // Reset counter on successful connection
                        return conn;
                    } else {
                        safelyCloseConnection(conn);
                        throw new SQLException("Invalid connection obtained from pool");
                    }
                } finally {
                    connectionLock.unlock();
                }
            } else {
                throw new SQLException("Timeout waiting for database lock");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Database connection error: " + e.getMessage());
            if (!fallbackMode) {
                enableFallbackMode("Connection error: " + e.getMessage());
            }
            throw new SQLException("Failed to get database connection", e);
        }
    }

    private void safelyCloseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                if (debug) {
                    plugin.debug("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    public void synchronizeData() {
        if (fallbackMode) {
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) return;

            // Generate a unique server identifier
            String serverId = plugin.getServer().getName() + "-" + plugin.getServer().getPort();
            long currentTime = System.currentTimeMillis();

            // Update last sync timestamp with more detailed information
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + tablePrefix + "sync_status (server_id, last_sync, sync_type, sync_timestamp) " +
                            "VALUES (?, CURRENT_TIMESTAMP, 'REGULAR', ?) ON DUPLICATE KEY UPDATE last_sync = CURRENT_TIMESTAMP, sync_type = 'REGULAR', sync_timestamp = ?")) {
                ps.setString(1, serverId);
                ps.setLong(2, currentTime);
                ps.setLong(3, currentTime);
                ps.executeUpdate();
            }

            // Check for updates from other servers with a shorter time window (5 seconds instead of 10)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT server_id, sync_type, sync_timestamp FROM " + tablePrefix + "sync_status " +
                            "WHERE server_id != ? AND (sync_type = 'FORCED' OR sync_type = 'ACHIEVEMENT_UPDATE' " +
                            "OR last_sync > DATE_SUB(NOW(), INTERVAL 5 SECOND))")) {
                ps.setString(1, serverId);
                ResultSet rs = ps.executeQuery();

                boolean needsReload = false;
                while (rs.next()) {
                    String otherServerId = rs.getString("server_id");
                    String syncType = rs.getString("sync_type");

                    if (debug) {
                        plugin.debug("Detected update from server " + otherServerId + ", type: " + syncType);
                    }

                    // Always reload on forced or achievement updates
                    if ("FORCED".equals(syncType) || "ACHIEVEMENT_UPDATE".equals(syncType)) {
                        needsReload = true;
                        break;
                    } else {
                        needsReload = true;
                    }
                }

                if (needsReload) {
                    // Other servers have recent updates, reload data for all online players
                    if (debug) {
                        plugin.debug("Detected updates from other servers, reloading data...");
                    }
                    reloadAllData();
                }
            }
        } catch (SQLException e) {
            // Only log at warning level for non-critical errors
            if (debug) {
                plugin.debug("Sync error: " + e.getMessage());
            }
        } finally {
            // Always close the connection
            safelyCloseConnection(conn);
        }
    }

    public void notifyAchievementUpdate(UUID playerUuid, String achievementId) {
        if (fallbackMode) {
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) return;

            // Generate a unique server identifier
            String serverId = plugin.getServer().getName() + "-" + plugin.getServer().getPort();
            long currentTime = System.currentTimeMillis();

            // Check if the sync_status table has been updated with the new columns
            if (!schemaUpdated) {
                updateDatabaseSchema();
            }

            // Insert achievement-specific update notification
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + tablePrefix + "sync_status (server_id, last_sync, sync_type, sync_timestamp, player_uuid, achievement_id) " +
                            "VALUES (?, CURRENT_TIMESTAMP, 'ACHIEVEMENT_UPDATE', ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE last_sync = CURRENT_TIMESTAMP, sync_type = 'ACHIEVEMENT_UPDATE', " +
                            "sync_timestamp = ?, player_uuid = ?, achievement_id = ?")) {
                ps.setString(1, serverId);
                ps.setLong(2, currentTime);
                ps.setString(3, playerUuid.toString());
                ps.setString(4, achievementId);
                ps.setLong(5, currentTime);
                ps.setString(6, playerUuid.toString());
                ps.setString(7, achievementId);
                ps.executeUpdate();

                if (debug) {
                    plugin.debug("Notified other servers about achievement update: " + achievementId + " for player " + playerUuid);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error notifying achievement update: " + e.getMessage());
        } finally {
            safelyCloseConnection(conn);
        }
    }

    private void reloadAllData() {
        // Clear caches
        localPlayerDataCache.clear();
        localSettingsCache.clear();
        localAchievementCache.clear();

        // Reload all online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Force reload player data directly from database
            forceReloadPlayerData(player);

            // Explicitly reload achievements if enabled
            if (plugin.getAchievementManager().isEnabled()) {
                // Entferne zuerst alle vorhandenen Achievement-Daten für diesen Spieler
                UUID uuid = player.getUniqueId();
                Map<String, AchievementManager.PlayerAchievement> existingAchievements =
                        plugin.getAchievementManager().getPlayerAchievements(player);

                if (existingAchievements != null) {
                    existingAchievements.clear();
                }

                // Lade die Achievements neu
                plugin.getAchievementManager().loadPlayerAchievements(player);

                if (debug) {
                    plugin.debug("Explicitly reloaded achievements for " + player.getName());
                }
            }
        }

        if (debug) {
            plugin.debug("Data reload completed for all online players");
        }
    }

    private void forceReloadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        if (fallbackMode) {
            plugin.getLogger().warning("Cannot force reload player data in fallback mode");
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM " + tablePrefix + "players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    PlayerData playerData = new PlayerData(
                            uuid,
                            name,
                            rs.getInt("level"),
                            rs.getLong("xp"),
                            rs.getInt("blocks_mined")
                    );

                    // Update the cache
                    plugin.getLevelManager().cachePlayerData(uuid, playerData);

                    if (debug) {
                        plugin.debug("Force reloaded player data for " + name +
                                ": Level=" + playerData.getLevel() +
                                ", XP=" + playerData.getXp() +
                                ", Blocks Mined=" + playerData.getBlocksMined());
                    }
                }
            }

            // Also reload player settings
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM " + tablePrefix + "settings WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    PlayerSettings settings = new PlayerSettings(
                            uuid,
                            rs.getBoolean("enabled"),
                            rs.getBoolean("pickaxe_enabled"),
                            rs.getBoolean("axe_enabled"),
                            rs.getBoolean("shovel_enabled"),
                            rs.getBoolean("hoe_enabled")
                    );

                    // Update the cache
                    localSettingsCache.put(uuid, settings);

                    if (debug) {
                        plugin.debug("Force reloaded settings for " + name);
                    }
                }
            }
        } catch (SQLException e) {
            // Only log at warning level if debug is enabled
            if (debug) {
                plugin.getLogger().warning("Error force reloading player data for " + name + ": " + e.getMessage());
            }
        } finally {
            // Always close the connection
            safelyCloseConnection(conn);
        }
    }

    /**
     * Erzwingt eine sofortige Synchronisierung mit allen Servern
     * Diese Methode markiert alle Einträge als nicht verarbeitet, damit sie erneut verarbeitet werden
     */
    public void forceSyncNow() {
        if (isFallbackMode()) {
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                plugin.getLogger().warning("Could not force sync: Database connection is null");
                return;
            }

            // Generate a unique server identifier
            String serverId = plugin.getServer().getName() + "-" + plugin.getServer().getPort();
            long currentTime = System.currentTimeMillis();

            // Update sync status with FORCED type
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + tablePrefix + "sync_status (server_id, last_sync, sync_type, sync_timestamp) " +
                            "VALUES (?, CURRENT_TIMESTAMP, 'FORCED', ?) " +
                            "ON DUPLICATE KEY UPDATE last_sync = CURRENT_TIMESTAMP, sync_type = 'FORCED', sync_timestamp = ?")) {
                ps.setString(1, serverId);
                ps.setLong(2, currentTime);
                ps.setLong(3, currentTime);
                ps.executeUpdate();

                if (plugin.isDebugMode()) {
                    plugin.debug("[Debug] Forced sync notification sent to other servers");
                }
            }

            // Force immediate synchronization
            synchronizeData();

        } catch (SQLException e) {
            plugin.getLogger().warning("Error forcing sync: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
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

    /**
     * Gibt die Server-ID zurück, die für die Synchronisierung verwendet wird
     * @return Die Server-ID
     */
    private String getServerId() {
        // Verwende die Server-ID aus der Konfiguration oder generiere eine eindeutige ID
        String serverId = plugin.getConfig().getString("server-id");
        if (serverId == null || serverId.isEmpty()) {
            serverId = "server-" + Bukkit.getPort();
        }
        return serverId;
    }

    private void createSyncStatusTable() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS " + tablePrefix + "sync_status (" +
                            "server_id VARCHAR(64) PRIMARY KEY, " +
                            "last_sync TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "sync_type VARCHAR(32) DEFAULT 'REGULAR', " +
                            "sync_timestamp BIGINT DEFAULT 0, " +
                            "player_uuid VARCHAR(36) DEFAULT NULL, " +
                            "achievement_id VARCHAR(64) DEFAULT NULL" +
                            ")"
            );

            // Set schema as updated since we just created it
            schemaUpdated = true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating sync status table: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
        }
    }

    // Make this method public so it can be called directly from the main class
    public void createTables() {
        if (fallbackMode) {
            return;
        }

        try {
            // Try to create the database if it doesn't exist
            tryCreateDatabase();

            // Create tables
            Connection conn = getConnection();
            if (conn == null) {
                plugin.getLogger().severe("Could not create tables: Database connection is null");
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                // Create players table with BIGINT for xp
                String playersTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "players (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "name VARCHAR(16) NOT NULL, " +
                        "level INT NOT NULL DEFAULT 1, " +
                        "xp BIGINT NOT NULL DEFAULT 0, " +
                        "blocks_mined INT NOT NULL DEFAULT 0, " +
                        "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")";
                stmt.execute(playersTable);

                // Create settings table for player-specific settings
                String settingsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "settings (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "enabled BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "pickaxe_enabled BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "axe_enabled BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "shovel_enabled BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "hoe_enabled BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "FOREIGN KEY (uuid) REFERENCES " + tablePrefix + "players(uuid) ON DELETE CASCADE" +
                        ")";
                stmt.execute(settingsTable);

                // Create achievements table
                String achievementsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "achievements (" +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "achievement_id VARCHAR(64) NOT NULL, " +
                        "progress INT NOT NULL DEFAULT 0, " +
                        "claimed BOOLEAN NOT NULL DEFAULT false, " +
                        "PRIMARY KEY (uuid, achievement_id), " +
                        "FOREIGN KEY (uuid) REFERENCES " + tablePrefix + "players(uuid) ON DELETE CASCADE" +
                        ")";
                stmt.execute(achievementsTable);

                // Create sync status table
                createSyncStatusTable();

                plugin.getLogger().info("Database tables created successfully");
            }

            conn.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating tables: " + e.getMessage());
            if (debug) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
            } else {
                plugin.getLogger().severe("Error occurred: " + e.getMessage());
            }
        }
    }

    private void tryCreateDatabase() {
        String url = "jdbc:mysql://" + host + ":" + port + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true";
        plugin.getLogger().info("Trying to connect to MySQL server to check/create database: " + url);
        plugin.getLogger().info("Using username: " + username);

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
            if (debug) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
            } else {
                plugin.getLogger().severe("Error occurred: " + e.getMessage());
            }
        }
    }

    private void enableFallbackMode(String reason) {
        fallbackMode = true;
        plugin.getLogger().warning("Enabling fallback mode: " + reason);
        plugin.getLogger().warning("Player data will be stored locally until database connection is restored.");
        loadLocalData();
    }

    private void loadLocalData() {
        File dataDir = new File(plugin.getDataFolder(), "data");

        // Load player data files
        File playerDataDir = new File(dataDir, "players");
        if (playerDataDir.exists()) {
            for (File file : playerDataDir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".dat")) {
                    try {
                        String uuidStr = file.getName().replace(".dat", "");
                        UUID uuid = UUID.fromString(uuidStr);

                        String content = new String(Files.readAllBytes(file.toPath()));
                        String[] lines = content.split("\n");

                        if (lines.length >= 4) {
                            String name = lines[0];
                            int level = Integer.parseInt(lines[1]);
                            int xp = Integer.parseInt(lines[2]);
                            int blocksMined = Integer.parseInt(lines[3]);

                            PlayerData playerData = new PlayerData(uuid, name, level, xp, blocksMined);
                            localPlayerDataCache.put(uuid, playerData);

                            if (debug) {
                                plugin.debug("Loaded local player data for " + name);
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load local player data file: " + file.getName());
                        if (debug) {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            e.printStackTrace(pw);
                            plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
                        } else {
                            plugin.getLogger().severe("Error occurred: " + e.getMessage());
                        }
                    }
                }
            }
        } else {
            playerDataDir.mkdirs();
        }

        // Load settings files
        File settingsDir = new File(dataDir, "settings");
        if (settingsDir.exists()) {
            for (File file : settingsDir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".dat")) {
                    try {
                        String uuidStr = file.getName().replace(".dat", "");
                        UUID uuid = UUID.fromString(uuidStr);

                        String content = new String(Files.readAllBytes(file.toPath()));
                        String[] lines = content.split("\n");

                        if (lines.length >= 5) {
                            boolean enabled = Boolean.parseBoolean(lines[0]);
                            boolean pickaxeEnabled = Boolean.parseBoolean(lines[1]);
                            boolean axeEnabled = Boolean.parseBoolean(lines[2]);
                            boolean shovelEnabled = Boolean.parseBoolean(lines[3]);
                            boolean hoeEnabled = Boolean.parseBoolean(lines[4]);

                            PlayerSettings settings = new PlayerSettings(uuid, enabled, pickaxeEnabled, axeEnabled, shovelEnabled, hoeEnabled);
                            localSettingsCache.put(uuid, settings);

                            if (debug) {
                                plugin.debug("Loaded local settings for " + uuid);
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load local settings file: " + file.getName());
                        if (debug) {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            e.printStackTrace(pw);
                            plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
                        } else {
                            plugin.getLogger().severe("Error occurred: " + e.getMessage());
                        }
                    }
                }
            }
        } else {
            settingsDir.mkdirs();
        }

        // Load achievements files
        File achievementsDir = new File(dataDir, "achievements");
        if (achievementsDir.exists()) {
            for (File file : achievementsDir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".dat")) {
                    try {
                        String uuidStr = file.getName().replace(".dat", "");
                        UUID uuid = UUID.fromString(uuidStr);

                        Map<String, Object> achievements = new HashMap<>();
                        String content = new String(Files.readAllBytes(file.toPath()));
                        String[] lines = content.split("\n");

                        for (String line : lines) {
                            String[] parts = line.split(":");
                            if (parts.length >= 3) {
                                String achievementId = parts[0];
                                int progress = Integer.parseInt(parts[1]);
                                boolean claimed = Boolean.parseBoolean(parts[2]);

                                Map<String, Object> achievementData = new HashMap<>();
                                achievementData.put("progress", progress);
                                achievementData.put("claimed", claimed);

                                achievements.put(achievementId, achievementData);
                            }
                        }

                        localAchievementCache.put(uuid, achievements);

                        if (debug) {
                            plugin.debug("Loaded local achievements for " + uuid + ": " + achievements.size() + " entries");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load local achievements file: " + file.getName());
                        if (debug) {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            e.printStackTrace(pw);
                            plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
                        } else {
                            plugin.getLogger().severe("Error occurred: " + e.getMessage());
                        }
                    }
                }
            }
        } else {
            achievementsDir.mkdirs();
        }

        plugin.getLogger().info("Loaded local data: " + localPlayerDataCache.size() + " players, " +
                localSettingsCache.size() + " settings, " +
                localAchievementCache.size() + " achievement sets");
    }

    private void saveLocalData() {
        if (!fallbackMode) {
            return;
        }

        File dataDir = new File(plugin.getDataFolder(), "data");

        // Save player data
        File playerDataDir = new File(dataDir, "players");
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }

        for (Map.Entry<UUID, PlayerData> entry : localPlayerDataCache.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerData playerData = entry.getValue();

            File file = new File(playerDataDir, uuid.toString() + ".dat");
            try {
                StringBuilder content = new StringBuilder();
                content.append(playerData.getName()).append("\n");
                content.append(playerData.getLevel()).append("\n");
                content.append(playerData.getXp()).append("\n");
                content.append(playerData.getBlocksMined()).append("\n");

                Files.write(file.toPath(), content.toString().getBytes());

                if (debug) {
                    plugin.debug("Saved local player data for " + playerData.getName());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save local player data for " + uuid + ": " + e.getMessage());
                if (debug) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
                } else {
                    plugin.getLogger().severe("Error occurred: " + e.getMessage());
                }
            }
        }

        // Save settings
        File settingsDir = new File(dataDir, "settings");
        if (!settingsDir.exists()) {
            settingsDir.mkdirs();
        }

        for (Map.Entry<UUID, PlayerSettings> entry : localSettingsCache.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerSettings settings = entry.getValue();

            File file = new File(settingsDir, uuid.toString() + ".dat");
            try {
                StringBuilder content = new StringBuilder();
                content.append(settings.isEnabled()).append("\n");
                content.append(settings.isPickaxeEnabled()).append("\n");
                content.append(settings.isAxeEnabled()).append("\n");
                content.append(settings.isShovelEnabled()).append("\n");
                content.append(settings.isHoeEnabled()).append("\n");

                Files.write(file.toPath(), content.toString().getBytes());

                if (debug) {
                    plugin.debug("Saved local settings for " + uuid);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save local settings for " + uuid + ": " + e.getMessage());
                if (debug) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
                } else {
                    plugin.getLogger().severe("Error occurred: " + e.getMessage());
                }
            }
        }

        // Save achievements
        File achievementsDir = new File(dataDir, "achievements");
        if (!achievementsDir.exists()) {
            achievementsDir.mkdirs();
        }

        for (Map.Entry<UUID, Map<String, Object>> entry : localAchievementCache.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Object> achievements = entry.getValue();

            File file = new File(achievementsDir, uuid.toString() + ".dat");
            try {
                StringBuilder content = new StringBuilder();

                for (Map.Entry<String, Object> achievementEntry : achievements.entrySet()) {
                    String achievementId = achievementEntry.getKey();
                    Map<String, Object> achievementData = (Map<String, Object>) achievementEntry.getValue();

                    int progress = (int) achievementData.get("progress");
                    boolean claimed = (boolean) achievementData.get("claimed");

                    content.append(achievementId).append(":")
                            .append(progress).append(":")
                            .append(claimed).append("\n");
                }

                Files.write(file.toPath(), content.toString().getBytes());

                if (debug) {
                    plugin.debug("Saved local achievements for " + uuid + ": " + achievements.size() + " entries");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save local achievements for " + uuid + ": " + e.getMessage());
                if (debug) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
                } else {
                    plugin.getLogger().severe("Error occurred: " + e.getMessage());
                }
            }
        }
    }

    public void reloadConnection() {
        if (!useMySQL) {
            plugin.getLogger().info("MySQL ist in der Konfiguration deaktiviert. Verwende dateibasierte Speicherung.");
            if (!fallbackMode) {
                enableFallbackMode("MySQL ist deaktiviert");
            }
            return;
        }

        // Wenn wir im Fallback-Modus sind, versuchen wir, die Verbindung wiederherzustellen
        if (fallbackMode) {
            plugin.getLogger().info("Versuche, die MySQL-Verbindung wiederherzustellen...");

            try {
                setupConnectionPool();
                createTables();
                createSyncStatusTable();

                // Wenn wir hier ankommen, war die Verbindung erfolgreich
                fallbackMode = false;
                plugin.getLogger().info("MySQL-Verbindung erfolgreich wiederhergestellt!");

                // Synchronisiere lokale Daten mit der Datenbank
                syncLocalDataToDatabase();

                // Starte periodische Synchronisierungsaufgabe
                plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                        this::synchronizeData, 200L, 200L); // Run every 10 seconds (200 ticks)

            } catch (Exception e) {
                plugin.getLogger().severe("Fehler beim Wiederherstellen der MySQL-Verbindung: " + e.getMessage());
                if (debug) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    plugin.getLogger().severe("Vollständiger Stack-Trace:\n" + sw.toString());
                } else {
                    plugin.getLogger().severe("Fehler aufgetreten: " + e.getMessage());
                }

                // Bleibe im Fallback-Modus
                plugin.getLogger().warning("Bleibe im Fallback-Modus. Lokale Daten werden weiterhin verwendet.");
            }
        } else {
            // Wenn wir nicht im Fallback-Modus sind, laden wir die Verbindung neu
            plugin.getLogger().info("Lade MySQL-Verbindung neu...");

            try {
                // Schließe die bestehende Verbindung
                if (dataSource != null && !dataSource.isClosed()) {
                    dataSource.close();
                }

                // Stelle die Verbindung neu her
                setupConnectionPool();
                plugin.getLogger().info("MySQL-Verbindung erfolgreich neu geladen!");

            } catch (Exception e) {
                plugin.getLogger().severe("Fehler beim Neuladen der MySQL-Verbindung: " + e.getMessage());
                enableFallbackMode("Fehler beim Neuladen der Verbindung: " + e.getMessage());
                if (debug) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    plugin.getLogger().severe("Vollständiger Stack-Trace:\n" + sw.toString());
                } else {
                    plugin.getLogger().severe("Fehler aufgetreten: " + e.getMessage());
                }
            }
        }
    }

    public void syncLocalDataToDatabase() {
        if (fallbackMode) {
            plugin.getLogger().warning("Kann lokale Daten nicht mit der Datenbank synchronisieren, da das Plugin im Fallback-Modus ist.");
            return;
        }

        plugin.getLogger().info("Synchronisiere lokale Daten mit der Datenbank...");

        // Synchronisiere Spielerdaten
        int playerDataCount = 0;
        for (PlayerData playerData : localPlayerDataCache.values()) {
            Connection conn = null;
            try {
                conn = getConnection();
                if (conn == null) {
                    plugin.getLogger().severe("Konnte keine Verbindung zur Datenbank herstellen. Synchronisierung abgebrochen.");
                    return;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO " + tablePrefix + "players (uuid, name, level, xp, blocks_mined) " +
                                "VALUES (?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE name = ?, level = ?, xp = ?, blocks_mined = ?")) {
                    ps.setString(1, playerData.getUuid().toString());
                    ps.setString(2, playerData.getName());
                    ps.setInt(3, playerData.getLevel());
                    ps.setLong(4, playerData.getXp());
                    ps.setInt(5, playerData.getBlocksMined());
                    ps.setString(6, playerData.getName());
                    ps.setInt(7, playerData.getLevel());
                    ps.setLong(8, playerData.getXp());
                    ps.setInt(9, playerData.getBlocksMined());
                    ps.executeUpdate();

                    playerDataCount++;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Fehler beim Synchronisieren der Spielerdaten für " + playerData.getName() + ": " + e.getMessage());
            } finally {
                safelyCloseConnection(conn);
            }
        }

        // Synchronisiere Einstellungen
        int settingsCount = 0;
        for (PlayerSettings settings : localSettingsCache.values()) {
            Connection conn = null;
            try {
                conn = getConnection();
                if (conn == null) continue;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO " + tablePrefix + "settings (uuid, enabled, pickaxe_enabled, axe_enabled, shovel_enabled, hoe_enabled) " +
                                "VALUES (?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE enabled = ?, pickaxe_enabled = ?, axe_enabled = ?, shovel_enabled = ?, hoe_enabled = ?")) {
                    ps.setString(1, settings.getUuid().toString());
                    ps.setBoolean(2, settings.isEnabled());
                    ps.setBoolean(3, settings.isPickaxeEnabled());
                    ps.setBoolean(4, settings.isAxeEnabled());
                    ps.setBoolean(5, settings.isShovelEnabled());
                    ps.setBoolean(6, settings.isHoeEnabled());
                    ps.setBoolean(7, settings.isEnabled());
                    ps.setBoolean(8, settings.isPickaxeEnabled());
                    ps.setBoolean(9, settings.isAxeEnabled());
                    ps.setBoolean(10, settings.isShovelEnabled());
                    ps.setBoolean(11, settings.isHoeEnabled());
                    ps.executeUpdate();

                    settingsCount++;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Fehler beim Synchronisieren der Einstellungen für " + settings.getUuid() + ": " + e.getMessage());
            } finally {
                safelyCloseConnection(conn);
            }
        }

        // Synchronisiere Achievements
        int achievementCount = 0;
        for (Map.Entry<UUID, Map<String, Object>> entry : localAchievementCache.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Object> achievements = entry.getValue();

            for (Map.Entry<String, Object> achievementEntry : achievements.entrySet()) {
                String achievementId = achievementEntry.getKey();
                Map<String, Object> achievementData = (Map<String, Object>) achievementEntry.getValue();

                int progress = (int) achievementData.get("progress");
                boolean claimed = (boolean) achievementData.get("claimed");

                Connection conn = null;
                try {
                    conn = getConnection();
                    if (conn == null) continue;

                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO " + tablePrefix + "achievements (uuid, achievement_id, progress, claimed) " +
                                    "VALUES (?, ?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE progress = ?, claimed = ?")) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, achievementId);
                        ps.setInt(3, progress);
                        ps.setBoolean(4, claimed);
                        ps.setInt(5, progress);
                        ps.setBoolean(6, claimed);
                        ps.executeUpdate();

                        achievementCount++;
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Fehler beim Synchronisieren des Achievements " + achievementId + " für " + uuid + ": " + e.getMessage());
                } finally {
                    safelyCloseConnection(conn);
                }
            }
        }

        plugin.getLogger().info("Lokale Daten synchronisiert: " + playerDataCount + " Spielerdaten, " +
                settingsCount + " Einstellungen, " +
                achievementCount + " Achievements");

        // Leere die lokalen Caches, da die Daten jetzt in der Datenbank sind
        localPlayerDataCache.clear();
        localSettingsCache.clear();
        localAchievementCache.clear();
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public boolean isFallbackMode() {
        return fallbackMode;
    }

    public CompletableFuture<PlayerData> loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        // Check if we're in fallback mode
        if (fallbackMode) {
            // Return from local cache if available
            if (localPlayerDataCache.containsKey(uuid)) {
                PlayerData cachedData = localPlayerDataCache.get(uuid);
                // Update name if it changed
                if (!cachedData.getName().equals(name)) {
                    cachedData = new PlayerData(uuid, name, cachedData.getLevel(), cachedData.getXp(), cachedData.getBlocksMined());
                    localPlayerDataCache.put(uuid, cachedData);
                }
                return CompletableFuture.completedFuture(cachedData);
            }

            // Create new default data
            PlayerData newData = new PlayerData(uuid, name, 1, 0, 0);
            localPlayerDataCache.put(uuid, newData);
            return CompletableFuture.completedFuture(newData);
        }

        return CompletableFuture.supplyAsync(() -> {
            Connection conn = null;
            try {
                conn = getConnection();
                if (conn == null) {
                    // Connection failed, switch to fallback mode
                    enableFallbackMode("Connection is null during loadPlayerData");
                    return loadPlayerData(player).join();
                }

                // Use INSERT ... ON DUPLICATE KEY UPDATE to handle both new and existing players
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO " + tablePrefix + "players (uuid, name, level, xp, blocks_mined) " +
                                "VALUES (?, ?, 1, 0, 0) " +
                                "ON DUPLICATE KEY UPDATE name = ?, last_seen = CURRENT_TIMESTAMP")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setString(3, name);
                    ps.executeUpdate();

                    if (debug) {
                        plugin.debug("Player data inserted/updated for " + name);
                    }
                }

                // Load player data
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM " + tablePrefix + "players WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        // Create settings if they don't exist
                        try (PreparedStatement settingsPs = conn.prepareStatement(
                                "INSERT IGNORE INTO " + tablePrefix + "settings (uuid, enabled, pickaxe_enabled, axe_enabled, shovel_enabled, hoe_enabled) " +
                                        "VALUES (?, TRUE, TRUE, TRUE, TRUE, TRUE)")) {
                            settingsPs.setString(1, uuid.toString());
                            settingsPs.executeUpdate();
                        }

                        PlayerData playerData = new PlayerData(
                                uuid,
                                name,
                                rs.getInt("level"),
                                rs.getLong("xp"),
                                rs.getInt("blocks_mined")
                        );

                        if (debug) {
                            plugin.debug("Loaded player data for " + name +
                                    ": Level=" + playerData.getLevel() +
                                    ", XP=" + playerData.getXp() +
                                    ", Blocks Mined=" + playerData.getBlocksMined());
                        }

                        return playerData;
                    }
                }

                // If we get here, something went wrong
                PlayerData defaultData = new PlayerData(uuid, name, 1, 0, 0);
                if (fallbackMode) {
                    localPlayerDataCache.put(uuid, defaultData);
                }
                return defaultData;

            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading player data for " + name + ": " + e.getMessage());
                if (debug) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
                } else {
                    plugin.getLogger().severe("Error occurred: " + e.getMessage());
                }

                // Switch to fallback mode on error
                if (!fallbackMode) {
                    enableFallbackMode("SQL error during loadPlayerData: " + e.getMessage());
                    return loadPlayerData(player).join();
                }

                // Return default data if something went wrong
                PlayerData defaultData = new PlayerData(uuid, name, 1, 0, 0);
                if (fallbackMode) {
                    localPlayerDataCache.put(uuid, defaultData);
                }
                return defaultData;
            } finally {
                safelyCloseConnection(conn);
            }
        });
    }

    public CompletableFuture<PlayerSettings> loadPlayerSettings(UUID uuid) {
        // Check if we're in fallback mode
        if (fallbackMode) {
            // Return from local cache if available
            if (localSettingsCache.containsKey(uuid)) {
                return CompletableFuture.completedFuture(localSettingsCache.get(uuid));
            }

            // Create new default settings
            PlayerSettings defaultSettings = new PlayerSettings(uuid, true, true, true, true, true);
            localSettingsCache.put(uuid, defaultSettings);
            return CompletableFuture.completedFuture(defaultSettings);
        }

        return CompletableFuture.supplyAsync(() -> {
            Connection conn = null;
            try {
                conn = getConnection();
                if (conn == null) {
                    // Connection failed, switch to fallback mode
                    enableFallbackMode("Connection is null during loadPlayerSettings");
                    return loadPlayerSettings(uuid).join();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM " + tablePrefix + "settings WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        PlayerSettings settings = new PlayerSettings(
                                uuid,
                                rs.getBoolean("enabled"),
                                rs.getBoolean("pickaxe_enabled"),
                                rs.getBoolean("axe_enabled"),
                                rs.getBoolean("shovel_enabled"),
                                rs.getBoolean("hoe_enabled")
                        );

                        if (debug) {
                            plugin.debug("Loaded settings for " + uuid +
                                    ": Enabled=" + settings.isEnabled());
                        }

                        return settings;
                    } else {
                        // Create default settings if they don't exist
                        try (PreparedStatement insertPs = conn.prepareStatement(
                                "INSERT INTO " + tablePrefix + "settings (uuid, enabled, pickaxe_enabled, axe_enabled, shovel_enabled, hoe_enabled) " +
                                        "VALUES (?, TRUE, TRUE, TRUE, TRUE, TRUE)")) {
                            insertPs.setString(1, uuid.toString());
                            insertPs.executeUpdate();

                            if (debug) {
                                plugin.debug("Created default settings for " + uuid);
                            }
                        }

                        return new PlayerSettings(uuid, true, true, true, true, true);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading player settings for " + uuid + ": " + e.getMessage());
                if (debug) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
                } else {
                    plugin.getLogger().severe("Error occurred: " + e.getMessage());
                }

                // Switch to fallback mode on error
                if (!fallbackMode) {
                    enableFallbackMode("SQL error during loadPlayerSettings: " + e.getMessage());
                    return loadPlayerSettings(uuid).join();
                }

                // Return default settings if something went wrong
                PlayerSettings defaultSettings = new PlayerSettings(uuid, true, true, true, true, true);
                if (fallbackMode) {
                    localSettingsCache.put(uuid, defaultSettings);
                }
                return defaultSettings;
            } finally {
                safelyCloseConnection(conn);
            }
        });
    }

    public void savePlayerData(PlayerData playerData) {
        if (fallbackMode) {
            // Save to local cache in fallback mode
            localPlayerDataCache.put(playerData.getUuid(), playerData);
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                // Connection failed, switch to fallback mode
                enableFallbackMode("Connection is null during savePlayerData");
                savePlayerData(playerData);
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + tablePrefix + "players SET level = ?, xp = ?, blocks_mined = ?, last_seen = CURRENT_TIMESTAMP WHERE uuid = ?")) {
                ps.setInt(1, playerData.getLevel());
                ps.setLong(2, playerData.getXp());
                ps.setInt(3, playerData.getBlocksMined());
                ps.setString(4, playerData.getUuid().toString());
                int updated = ps.executeUpdate();

                if (debug && updated > 0) {
                    plugin.debug("Saved player data for " + playerData.getName() +
                            ": Level=" + playerData.getLevel() +
                            ", XP=" + playerData.getXp() +
                            ", Blocks Mined=" + playerData.getBlocksMined());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving player data for " + playerData.getName() + ": " + e.getMessage());
            if (debug) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
            } else {
                plugin.getLogger().severe("Error occurred: " + e.getMessage());
            }

            // Switch to fallback mode on error
            if (!fallbackMode) {
                enableFallbackMode("SQL error during savePlayerData: " + e.getMessage());
                savePlayerData(playerData);
            }
        } finally {
            safelyCloseConnection(conn);
        }
    }

    public void savePlayerSettings(PlayerSettings settings) {
        if (fallbackMode) {
            // Save to local cache in fallback mode
            localSettingsCache.put(settings.getUuid(), settings);
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                // Connection failed, switch to fallback mode
                enableFallbackMode("Connection is null during savePlayerSettings");
                savePlayerSettings(settings);
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + tablePrefix + "settings SET enabled = ?, pickaxe_enabled = ?, axe_enabled = ?, shovel_enabled = ?, hoe_enabled = ? WHERE uuid = ?")) {
                ps.setBoolean(1, settings.isEnabled());
                ps.setBoolean(2, settings.isPickaxeEnabled());
                ps.setBoolean(3, settings.isAxeEnabled());
                ps.setBoolean(4, settings.isShovelEnabled());
                ps.setBoolean(5, settings.isHoeEnabled());
                ps.setString(6, settings.getUuid().toString());
                int updated = ps.executeUpdate();

                if (debug && updated > 0) {
                    plugin.debug("Saved settings for " + settings.getUuid() +
                            ": Enabled=" + settings.isEnabled());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving player settings for " + settings.getUuid() + ": " + e.getMessage());
            if (debug) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
            } else {
                plugin.getLogger().severe("Error occurred: " + e.getMessage());
            }

            // Switch to fallback mode on error
            if (!fallbackMode) {
                enableFallbackMode("SQL error during savePlayerSettings: " + e.getMessage());
                savePlayerSettings(settings);
            }
        } finally {
            safelyCloseConnection(conn);
        }
    }

    public void saveAchievement(UUID uuid, String achievementId, int progress, boolean claimed) {
        if (fallbackMode) {
            // Save to local cache in fallback mode
            Map<String, Object> achievements = localAchievementCache.computeIfAbsent(uuid, k -> new HashMap<>());
            Map<String, Object> achievementData = new HashMap<>();
            achievementData.put("progress", progress);
            achievementData.put("claimed", claimed);
            achievements.put(achievementId, achievementData);
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                // Connection failed, switch to fallback mode
                enableFallbackMode("Connection is null during saveAchievement");
                saveAchievement(uuid, achievementId, progress, claimed);
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + tablePrefix + "achievements (uuid, achievement_id, progress, claimed) " +
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
                    plugin.debug("Saved achievement for " + uuid +
                            ": " + achievementId +
                            ", Progress: " + progress +
                            ", Claimed: " + claimed);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving achievement for " + uuid + ": " + e.getMessage());
            if (debug) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
            } else {
                plugin.getLogger().severe("Error occurred: " + e.getMessage());
            }

            // Switch to fallback mode on error
            if (!fallbackMode) {
                enableFallbackMode("SQL error during saveAchievement: " + e.getMessage());
                saveAchievement(uuid, achievementId, progress, claimed);
            }
        } finally {
            safelyCloseConnection(conn);
        }
    }

    // Add this method to close the database connection
    public void close() {
        // Save all data before closing
        if (fallbackMode) {
            saveLocalData();
        } else {
            // Save data for all online players
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (plugin.getLevelManager().getPlayerData(uuid) != null) {
                    savePlayerData(plugin.getLevelManager().getPlayerData(uuid));
                }

                if (plugin.getAchievementManager().isEnabled()) {
                    plugin.getAchievementManager().savePlayerAchievements(player);
                }
            }
        }

        // Close the connection pool if it exists
        if (!fallbackMode && dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (Exception e) {
                plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
                if (debug) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    plugin.getLogger().severe("Full stack trace:\n" + sw.toString());
                }
            }
        }
    }

    /**
     * Gets the local achievement cache (for use in fallback mode)
     */
    public Map<UUID, Map<String, Object>> getLocalAchievementCache() {
        return localAchievementCache;
    }
}