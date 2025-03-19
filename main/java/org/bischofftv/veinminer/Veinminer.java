package org.bischofftv.veinminer;

import org.bischofftv.veinminer.achievements.AchievementManager;
import org.bischofftv.veinminer.achievements.AchievementGUI;
import org.bischofftv.veinminer.commands.*;
import org.bischofftv.veinminer.config.ConfigManager;
import org.bischofftv.veinminer.database.DatabaseManager;
import org.bischofftv.veinminer.gui.MainGUI;
import org.bischofftv.veinminer.listeners.AchievementListener;
import org.bischofftv.veinminer.listeners.BlockBreakListener;
import org.bischofftv.veinminer.listeners.GUIListener;
import org.bischofftv.veinminer.listeners.PlayerListener;
import org.bischofftv.veinminer.logging.MiningLogger;
import org.bischofftv.veinminer.utils.LevelManager;
import org.bischofftv.veinminer.utils.MessageManager;
import org.bischofftv.veinminer.utils.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class Veinminer extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private MiningLogger miningLogger;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private LevelManager levelManager;
    private AchievementManager achievementManager;
    private AchievementGUI achievementGUI;
    private MainGUI mainGUI;
    private BukkitTask autoSaveTask;
    private boolean debugMode;

    // Füge diese Felder und Methoden zur Veinminer-Klasse hinzu
    private AdminCommand adminCommand;

    private FileConfiguration messagesConfig = null;
    private File messagesConfigFile = null;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        try {
            // Save default messages.yml if it doesn't exist
            saveDefaultMessagesConfig();
        } catch (Exception e) {
            getLogger().warning("Failed to load messages.yml: " + e.getMessage());
            getLogger().warning("Will use default messages instead");
        }

        // Initialize ConfigManager first
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Check if debug mode is enabled
        this.debugMode = getConfig().getBoolean("settings.debug", false);

        // Initialize managers
        this.messageManager = new MessageManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.levelManager = new LevelManager(this);
        this.achievementManager = new AchievementManager(this);
        this.achievementGUI = new AchievementGUI(this);
        this.mainGUI = new MainGUI(this);
        this.miningLogger = new MiningLogger(this);

        // Initialize database
        try {
            databaseManager.initialize();

            // Create database tables first
            databaseManager.createTables();

            // Trigger database schema update on startup if needed
            updateDatabaseSchema();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getLogger().warning("Plugin will continue in fallback mode");
        }

        // Register command executors - safely check if commands exist first
        safeRegisterCommand("veinminer", new CommandHandler(this));
        safeRegisterCommand("vmtoggle", new ToggleCommand(this));
        safeRegisterCommand("vmlevel", new LevelCommand(this));
        safeRegisterCommand("vmsetlevel", new LevelSetCommand(this));
        safeRegisterCommand("vmsync", new SyncCommand(this));
        safeRegisterCommand("veinminerreload", new ReloadCommand(this));
        safeRegisterCommand("veinminerabout", new AboutCommand(this));
        safeRegisterCommand("veinminerhelp", new HelpCommand(this));

        // Also register the tab completer for the main command
        if (getCommand("veinminer") != null) {
            getCommand("veinminer").setTabCompleter(new VeinMinerTabCompleter(this));
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new AchievementListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Füge diese Zeile zur onEnable-Methode hinzu, nach der Initialisierung der anderen Befehle
        adminCommand = new AdminCommand(this);

        // Load data for online players (in case of reload)
        getServer().getOnlinePlayers().forEach(player -> {
            playerDataManager.loadPlayerData(player);
            if (achievementManager.isEnabled()) {
                achievementManager.loadPlayerAchievements(player);
            }
        });

        // Start auto-save task if enabled
        startAutoSaveTask();

        getLogger().info("VeinMiner has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel auto-save task if running
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        // Save all player data
        playerDataManager.saveAllData();

        // Save all achievement data
        if (achievementManager != null) {
            achievementManager.saveAllAchievements();
        }

        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("VeinMiner has been disabled!");
    }

    // Add a method to manually trigger data synchronization
    public void syncDataNow() {
        if (databaseManager != null) {
            if (debugMode) {
                getLogger().info("[Debug] Manually triggering data synchronization...");
            }
            databaseManager.synchronizeData();

            // Füge eine verzögerte zweite Synchronisierung hinzu, um sicherzustellen, dass Änderungen übernommen wurden
            getServer().getScheduler().runTaskLater(this, () -> {
                if (debugMode) {
                    getLogger().info("[Debug] Running follow-up synchronization...");
                }
                databaseManager.synchronizeData();
            }, 10L); // 0.5 Sekunden später
        }
    }

    // Füge eine Methode hinzu, um eine erzwungene Synchronisierung durchzuführen
    public void forceSyncDataNow() {
        if (databaseManager != null) {
            if (debugMode) {
                getLogger().info("[Debug] Forcing data synchronization across all servers...");
            }
            databaseManager.forceSyncNow();
        }
    }

    // Modify the startAutoSaveTask method to include synchronization
    public void startAutoSaveTask() {
        // Cancel existing task if running
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        // Get auto-save interval from config (in minutes)
        int intervalMinutes = configManager.getAutoSaveInterval();

        // If interval is 0 or negative, auto-save is disabled
        if (intervalMinutes <= 0) {
            getLogger().info("Auto-save is disabled.");
            return;
        }

        // Convert minutes to ticks (1 minute = 60 seconds = 1200 ticks)
        long intervalTicks = intervalMinutes * 60L * 20L;

        // Schedule the auto-save task
        autoSaveTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (debugMode) {
                getLogger().info("[Debug] Running auto-save task...");
            }

            // Save player data
            playerDataManager.saveAllData();

            // Save achievement data
            if (achievementManager != null && achievementManager.isEnabled()) {
                achievementManager.saveAllAchievements();

                if (debugMode) {
                    getLogger().info("[Debug] Auto-saved all player achievements.");
                }
            }

            // Trigger data synchronization
            if (databaseManager != null) {
                databaseManager.synchronizeData();

                if (debugMode) {
                    getLogger().info("[Debug] Triggered data synchronization.");
                }
            }

            if (debugMode) {
                getLogger().info("[Debug] Auto-save completed.");
            }
        }, intervalTicks, intervalTicks);

        // Add an additional task for more frequent synchronization (every 30 seconds)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (databaseManager != null && !databaseManager.isFallbackMode()) {
                if (debugMode) {
                    getLogger().info("[Debug] Running frequent synchronization check...");
                }
                databaseManager.synchronizeData();
            }
        }, 600L, 600L); // 30 seconds = 600 ticks

        getLogger().info("Auto-save scheduled every " + intervalMinutes + " minutes with additional sync every 30 seconds.");
    }

    /**
     * Restarts the auto-save task (called after config reload)
     */
    public void restartAutoSaveTask() {
        startAutoSaveTask();
    }

    /**
     * Logs a debug message if debug mode is enabled
     * @param message The message to log
     */
    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[Debug] " + message);
        }
    }

// Füge diese Methode zur Veinminer-Klasse hinzu

    /**
     * Setzt den Debug-Modus
     * @param debugMode true, um den Debug-Modus zu aktivieren, false, um ihn zu deaktivieren
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        getLogger().info("Debug-Modus wurde " + (debugMode ? "aktiviert" : "deaktiviert") + ".");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public MiningLogger getMiningLogger() {
        return miningLogger;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public AchievementGUI getAchievementGUI() {
        return achievementGUI;
    }

    public MainGUI getMainGUI() {
        return mainGUI;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    // Füge diese Getter-Methode hinzu
    public AdminCommand getAdminCommand() {
        return adminCommand;
    }

    // Safely register a command, checking if it exists first
    private void safeRegisterCommand(String name, Object executor) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor((CommandExecutor) executor);

            if (executor instanceof TabCompleter) {
                getCommand(name).setTabCompleter((TabCompleter) executor);
            }

            // Special case for vmsync command
            if (name.equals("vmsync") && getCommand(name) != null) {
                getCommand(name).setTabCompleter(new SyncCommand(this));
            }
        } else {
            getLogger().warning("Command '" + name + "' not found in plugin.yml. Skipping registration.");
        }
    }

    public FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            reloadMessagesConfig();
        }
        return messagesConfig;
    }

    public void reloadMessagesConfig() {
        if (messagesConfigFile == null) {
            messagesConfigFile = new File(getDataFolder(), "messages.yml");
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesConfigFile);

        // Load defaults from jar
        InputStream defaultStream = getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public void saveMessagesConfig() {
        if (messagesConfig == null || messagesConfigFile == null) {
            return;
        }

        try {
            getMessagesConfig().save(messagesConfigFile);
        } catch (IOException e) {
            getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }

    // Modify the saveDefaultMessagesConfig method to handle the case when messages.yml doesn't exist
    private void saveDefaultMessagesConfig() {
        if (messagesConfigFile == null) {
            messagesConfigFile = new File(getDataFolder(), "messages.yml");
        }

        if (!messagesConfigFile.exists()) {
            try {
                // Create an empty file if the resource doesn't exist
                messagesConfigFile.getParentFile().mkdirs();
                messagesConfigFile.createNewFile();

                // Create a default messages configuration
                YamlConfiguration defaultMessages = new YamlConfiguration();
                defaultMessages.set("messages.command.player-only", "&cThis command can only be used by players.");
                defaultMessages.set("messages.command.no-permission", "&cYou don't have permission to use this command.");
                defaultMessages.set("messages.toggle.enabled", "&aVeinMiner has been enabled.");
                defaultMessages.set("messages.toggle.disabled", "&cVeinMiner has been disabled.");
                defaultMessages.set("messages.reload.success", "&aVeinMiner configuration has been reloaded.");

                // Save the default configuration
                defaultMessages.save(messagesConfigFile);
                getLogger().info("Created default messages.yml file");
            } catch (IOException e) {
                getLogger().severe("Could not create messages.yml: " + e.getMessage());
            }
        }
    }

    // Add a method to update the database schema if needed
    private void updateDatabaseSchema() {
        if (databaseManager != null && !databaseManager.isFallbackMode()) {
            try {
                databaseManager.updateDatabaseSchema();
                getLogger().info("Database schema checked and updated if needed.");
            } catch (Exception e) {
                getLogger().severe("Failed to update database schema: " + e.getMessage());
            }
        }
    }
}