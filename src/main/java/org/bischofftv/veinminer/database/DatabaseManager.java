package org.bischofftv.veinminer.database;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final Veinminer plugin;
    private Connection connection;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String tablePrefix;
    private final boolean useMySQL;
    private final boolean debug;

    public DatabaseManager(Veinminer plugin) {
        this.plugin = plugin;
        this.useMySQL = plugin.getConfig().getBoolean("database.use-mysql", true);
        this.host = plugin.getConfig().getString("database.host", "localhost");
        this.port = plugin.getConfig().getInt("database.port", 3306);
        this.database = plugin.getConfig().getString("database.database", "veinminer");
        this.username = plugin.getConfig().getString("database.username", "root");
        this.password = plugin.getConfig().getString("database.password", "password");
        this.tablePrefix = plugin.getConfig().getString("database.table-prefix", "vm_");
        this.debug = plugin.getConfig().getBoolean("settings.debug", false);
    }

    public void initialize() {
        if (!useMySQL) {
            plugin.getLogger().info("MySQL is disabled in config. Using file-based storage.");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connect();
            createTables();
            plugin.getLogger().info("Successfully connected to MySQL database!");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("MySQL JDBC driver not found! Please make sure you have the correct driver.");
            e.printStackTrace();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        connection = DriverManager.getConnection(url, username, password);
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create players table
            String playersTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "level INT NOT NULL DEFAULT 1, " +
                    "xp INT NOT NULL DEFAULT 0, " +
                    "blocks_mined INT NOT NULL DEFAULT 0, " +
                    "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")";
            statement.execute(playersTable);

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
            statement.execute(settingsTable);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public CompletableFuture<PlayerData> loadPlayerData(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (!useMySQL) {
                return new PlayerData(player.getUniqueId(), player.getName(), 1, 0, 0);
            }

            UUID uuid = player.getUniqueId();
            String name = player.getName();

            try {
                connect(); // Ensure connection is active

                // Use INSERT ... ON DUPLICATE KEY UPDATE to handle both new and existing players
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO " + tablePrefix + "players (uuid, name, level, xp, blocks_mined) " +
                                "VALUES (?, ?, 1, 0, 0) " +
                                "ON DUPLICATE KEY UPDATE name = ?, last_seen = CURRENT_TIMESTAMP")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setString(3, name);
                    ps.executeUpdate();

                    if (debug) {
                        plugin.getLogger().info("[Debug] Player data inserted/updated for " + name);
                    }
                }

                // Load player data
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM " + tablePrefix + "players WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        // Create settings if they don't exist
                        try (PreparedStatement settingsPs = connection.prepareStatement(
                                "INSERT IGNORE INTO " + tablePrefix + "settings (uuid, enabled, pickaxe_enabled, axe_enabled, shovel_enabled, hoe_enabled) " +
                                        "VALUES (?, TRUE, TRUE, TRUE, TRUE, TRUE)")) {
                            settingsPs.setString(1, uuid.toString());
                            settingsPs.executeUpdate();
                        }

                        PlayerData playerData = new PlayerData(
                                uuid,
                                name,
                                rs.getInt("level"),
                                rs.getInt("xp"),
                                rs.getInt("blocks_mined")
                        );

                        if (debug) {
                            plugin.getLogger().info("[Debug] Loaded player data for " + name +
                                    ": Level=" + playerData.getLevel() +
                                    ", XP=" + playerData.getXp() +
                                    ", Blocks Mined=" + playerData.getBlocksMined());
                        }

                        return playerData;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading player data for " + name + ": " + e.getMessage());
                e.printStackTrace();
            }

            // Return default data if something went wrong
            return new PlayerData(uuid, name, 1, 0, 0);
        });
    }

    public CompletableFuture<PlayerSettings> loadPlayerSettings(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!useMySQL) {
                return new PlayerSettings(uuid, true, true, true, true, true);
            }

            try {
                connect(); // Ensure connection is active

                try (PreparedStatement ps = connection.prepareStatement(
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
                            plugin.getLogger().info("[Debug] Loaded settings for " + uuid +
                                    ": Enabled=" + settings.isEnabled());
                        }

                        return settings;
                    } else {
                        // Create default settings if they don't exist
                        try (PreparedStatement insertPs = connection.prepareStatement(
                                "INSERT INTO " + tablePrefix + "settings (uuid, enabled, pickaxe_enabled, axe_enabled, shovel_enabled, hoe_enabled) " +
                                        "VALUES (?, TRUE, TRUE, TRUE, TRUE, TRUE)")) {
                            insertPs.setString(1, uuid.toString());
                            insertPs.executeUpdate();

                            if (debug) {
                                plugin.getLogger().info("[Debug] Created default settings for " + uuid);
                            }
                        }

                        return new PlayerSettings(uuid, true, true, true, true, true);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading player settings for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
                return new PlayerSettings(uuid, true, true, true, true, true);
            }
        });
    }

    public void savePlayerData(PlayerData playerData) {
        if (!useMySQL) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                connect(); // Ensure connection is active

                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE " + tablePrefix + "players SET level = ?, xp = ?, blocks_mined = ?, last_seen = CURRENT_TIMESTAMP WHERE uuid = ?")) {
                    ps.setInt(1, playerData.getLevel());
                    ps.setInt(2, playerData.getXp());
                    ps.setInt(3, playerData.getBlocksMined());
                    ps.setString(4, playerData.getUuid().toString());
                    int updated = ps.executeUpdate();

                    if (debug && updated > 0) {
                        plugin.getLogger().info("[Debug] Saved player data for " + playerData.getName() +
                                ": Level=" + playerData.getLevel() +
                                ", XP=" + playerData.getXp() +
                                ", Blocks Mined=" + playerData.getBlocksMined());
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error saving player data for " + playerData.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void savePlayerSettings(PlayerSettings settings) {
        if (!useMySQL) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                connect(); // Ensure connection is active

                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE " + tablePrefix + "settings SET enabled = ?, pickaxe_enabled = ?, axe_enabled = ?, shovel_enabled = ?, hoe_enabled = ? WHERE uuid = ?")) {
                    ps.setBoolean(1, settings.isEnabled());
                    ps.setBoolean(2, settings.isPickaxeEnabled());
                    ps.setBoolean(3, settings.isAxeEnabled());
                    ps.setBoolean(4, settings.isShovelEnabled());
                    ps.setBoolean(5, settings.isHoeEnabled());
                    ps.setString(6, settings.getUuid().toString());
                    int updated = ps.executeUpdate();

                    if (debug && updated > 0) {
                        plugin.getLogger().info("[Debug] Saved settings for " + settings.getUuid() +
                                ": Enabled=" + settings.isEnabled());
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error saving player settings for " + settings.getUuid() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static class PlayerData {
        private final UUID uuid;
        private final String name;
        private int level;
        private int xp;
        private int blocksMined;

        public PlayerData(UUID uuid, String name, int level, int xp, int blocksMined) {
            this.uuid = uuid;
            this.name = name;
            this.level = level;
            this.xp = xp;
            this.blocksMined = blocksMined;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getXp() {
            return xp;
        }

        public void setXp(int xp) {
            this.xp = xp;
        }

        public int getBlocksMined() {
            return blocksMined;
        }

        public void setBlocksMined(int blocksMined) {
            this.blocksMined = blocksMined;
        }

        public void addBlocksMined(int amount) {
            this.blocksMined += amount;
        }
    }

    public static class PlayerSettings {
        private final UUID uuid;
        private boolean enabled;
        private boolean pickaxeEnabled;
        private boolean axeEnabled;
        private boolean shovelEnabled;
        private boolean hoeEnabled;

        public PlayerSettings(UUID uuid, boolean enabled, boolean pickaxeEnabled, boolean axeEnabled, boolean shovelEnabled, boolean hoeEnabled) {
            this.uuid = uuid;
            this.enabled = enabled;
            this.pickaxeEnabled = pickaxeEnabled;
            this.axeEnabled = axeEnabled;
            this.shovelEnabled = shovelEnabled;
            this.hoeEnabled = hoeEnabled;
        }

        public UUID getUuid() {
            return uuid;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isPickaxeEnabled() {
            return pickaxeEnabled;
        }

        public void setPickaxeEnabled(boolean pickaxeEnabled) {
            this.pickaxeEnabled = pickaxeEnabled;
        }

        public boolean isAxeEnabled() {
            return axeEnabled;
        }

        public void setAxeEnabled(boolean axeEnabled) {
            this.axeEnabled = axeEnabled;
        }

        public boolean isShovelEnabled() {
            return shovelEnabled;
        }

        public void setShovelEnabled(boolean shovelEnabled) {
            this.shovelEnabled = shovelEnabled;
        }

        public boolean isHoeEnabled() {
            return hoeEnabled;
        }

        public void setHoeEnabled(boolean hoeEnabled) {
            this.hoeEnabled = hoeEnabled;
        }

        public boolean isToolEnabled(String toolType) {
            switch (toolType.toLowerCase()) {
                case "pickaxe":
                    return pickaxeEnabled;
                case "axe":
                    return axeEnabled;
                case "shovel":
                    return shovelEnabled;
                case "hoe":
                    return hoeEnabled;
                default:
                    return true;
            }
        }

        public void setToolEnabled(String toolType, boolean enabled) {
            switch (toolType.toLowerCase()) {
                case "pickaxe":
                    pickaxeEnabled = enabled;
                    break;
                case "axe":
                    axeEnabled = enabled;
                    break;
                case "shovel":
                    shovelEnabled = enabled;
                    break;
                case "hoe":
                    hoeEnabled = enabled;
                    break;
            }
        }
    }
}