package org.bischofftv.veinminer;

import org.bischofftv.veinminer.achievements.AchievementManager;
import org.bischofftv.veinminer.achievements.AchievementGUI;
import org.bischofftv.veinminer.commands.*;
import org.bischofftv.veinminer.config.ConfigManager;
import org.bischofftv.veinminer.database.DatabaseManager;
import org.bischofftv.veinminer.gui.MainGUI;
import org.bischofftv.veinminer.gui.SkillGUI;
import org.bischofftv.veinminer.gui.TopPlayersGUI;
import org.bischofftv.veinminer.hooks.WorldGuardHook;
import org.bischofftv.veinminer.listeners.*;
import org.bischofftv.veinminer.logging.MiningLogger;
import org.bischofftv.veinminer.placeholders.VeinMinerPlaceholders;
import org.bischofftv.veinminer.skills.SkillManager;
import org.bischofftv.veinminer.utils.*;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
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
    private SkillGUI skillGUI;
    private SkillManager skillManager;
    private TopPlayersGUI topPlayersGUI;
    private VeinMinerPlaceholders placeholderExpansion;
    private BukkitTask autoSaveTask;
    private boolean debugMode;
    private VeinMinerUtils veinMinerUtils;
    private WorldGuardHook worldGuardHook;
    private AdminCommand adminCommand;
    private UpdateChecker updateChecker;

    private FileConfiguration messagesConfig = null;
    private File messagesConfigFile = null;
    private FileConfiguration langConfig = null;
    private File langConfigFile = null;

    // Spigot resource ID for update checker
    private static final int RESOURCE_ID = 123199;

    /**
     * Get the lang configuration
     * @return The lang configuration
     */
    public FileConfiguration getLangConfig() {
        if (langConfig == null) {
            reloadLangConfig();
        }
        return langConfig;
    }

    /**
     * Reload the lang configuration
     */
    public void reloadLangConfig() {
        if (langConfigFile == null) {
            langConfigFile = new File(getDataFolder(), "lang.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langConfigFile);

        // Load defaults from jar
        InputStream defaultStream = getResource("lang.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            langConfig.setDefaults(defaultConfig);
        }
    }

    /**
     * Save the lang configuration
     */
    public void saveLangConfig() {
        if (langConfig == null || langConfigFile == null) {
            return;
        }

        try {
            getLangConfig().save(langConfigFile);
        } catch (IOException e) {
            getLogger().severe("Could not save lang.yml: " + e.getMessage());
        }
    }

    /**
     * Save the default lang configuration
     */
    public void saveDefaultLangConfig() {
        if (langConfigFile == null) {
            langConfigFile = new File(getDataFolder(), "lang.yml");
        }

        if (!langConfigFile.exists()) {
            saveResource("lang.yml", false);
        }
    }

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Print the plugin version
        getLogger().info("Enabling Veinminer v" + getDescription().getVersion());

        try {
            // Save default lang.yml if it doesn't exist
            saveDefaultLangConfig();
        } catch (Exception e) {
            getLogger().warning("Failed to load lang.yml: " + e.getMessage());
            getLogger().warning("Will use default messages instead");
        }

        try {
            // Save default messages.yml if it doesn't exist (for backward compatibility)
            saveDefaultMessagesConfig();
        } catch (Exception e) {
            getLogger().warning("Failed to load messages.yml: " + e.getMessage());
            getLogger().warning("Will use lang.yml instead");
        }

        // Initialize ConfigManager first
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize VeinMinerUtils
        this.veinMinerUtils = new VeinMinerUtils(this);

        // Initialize bStats with explicit try-catch to see any errors
        try {
            getLogger().info("Initializing bStats metrics...");
            // Create bStats metrics instance with plugin ID 25161
            Metrics metrics = new Metrics(this, 25161);

            // Add custom charts
            metrics.addCustomChart(new SimplePie("database_type", () -> {
                if (databaseManager != null) {
                    return databaseManager.isFallbackMode() ? "SQLite" : "MySQL";
                }
                return "Unknown";
            }));

            metrics.addCustomChart(new SimplePie("discord_integration", () -> {
                if (configManager != null) {
                    return configManager.isEnableDiscordLogging() ? "Enabled" : "Disabled";
                }
                return "Unknown";
            }));

            metrics.addCustomChart(new SimplePie("achievement_system", () -> {
                if (achievementManager != null) {
                    return achievementManager.isEnabled() ? "Enabled" : "Disabled";
                }
                return "Unknown";
            }));

            // Add WorldGuard integration chart
            metrics.addCustomChart(new SimplePie("worldguard_integration", () -> {
                if (worldGuardHook != null) {
                    return worldGuardHook.isEnabled() ? "Enabled" : "Disabled";
                }
                return "Not Installed";
            }));

            // Add Skill system chart
            metrics.addCustomChart(new SimplePie("skill_system", () -> {
                if (skillManager != null) {
                    return skillManager.isEnabled() ? "Enabled" : "Disabled";
                }
                return "Disabled";
            }));

            metrics.addCustomChart(new SingleLineChart("blocks_mined", () -> {
                if (veinMinerUtils != null) {
                    return veinMinerUtils.getTotalBlocksMinedToday();
                }
                return 0;
            }));

            metrics.addCustomChart(new SingleLineChart("active_players", () ->
                    Bukkit.getOnlinePlayers().size()));

            getLogger().info("bStats metrics initialized successfully with plugin ID 25161");
        } catch (Throwable e) {
            getLogger().warning("Failed to initialize bStats: " + e.getMessage());
            e.printStackTrace();
        }

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
        this.skillManager = new SkillManager(this);
        this.skillGUI = new SkillGUI(this);
        this.topPlayersGUI = new TopPlayersGUI(this);
        this.miningLogger = new MiningLogger(this);

        // Initialize WorldGuard hook
        this.worldGuardHook = new WorldGuardHook(this);

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

        // Debug: Print loaded configuration sections
        if (debugMode) {
            debug("Checking configuration sections:");
            debug("- level-system exists: " + (getConfig().getConfigurationSection("level-system") != null));
            debug("- achievement-system exists: " + (getConfig().getConfigurationSection("achievement-system") != null));
            debug("- achievements exists: " + (getConfig().getConfigurationSection("achievements") != null));
            if (getConfig().getConfigurationSection("achievements") != null) {
                debug("- achievements keys: " + String.join(", ", getConfig().getConfigurationSection("achievements").getKeys(false)));
            }
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
        safeRegisterCommand("vmskill", new SkillCommand(this));
        safeRegisterCommand("vmplaceholder", new PlaceholderCommand(this));

        // Also register the tab completer for the main command
        if (getCommand("veinminer") != null) {
            getCommand("veinminer").setTabCompleter(new VeinMinerTabCompleter(this));
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new AchievementListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new SkillGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new TopPlayersGUIListener(this), this);

        // Add this line to the onEnable method, after initializing the other commands
        adminCommand = new AdminCommand(this);

        // Register the vmadmin command
        if (getCommand("vmadmin") != null) {
            getCommand("vmadmin").setExecutor(adminCommand);
            getCommand("vmadmin").setTabCompleter(adminCommand);
        } else {
            getLogger().warning("Command 'vmadmin' not found in plugin.yml. Admin command will not be available.");
        }

        // Load data for online players (in case of reload)
        getServer().getOnlinePlayers().forEach(player -> {
            playerDataManager.loadPlayerData(player);
            if (achievementManager.isEnabled()) {
                achievementManager.loadPlayerAchievements(player);
            }
        });

        // Start auto-save task if enabled
        startAutoSaveTask();

        // Initialize update checker
        if (getConfig().getBoolean("settings.check-for-updates", true)) {
            this.updateChecker = new UpdateChecker(this, RESOURCE_ID);
        }

        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found! Registering placeholders...");

            // Unregister any existing expansion first to avoid duplicates
            if (this.placeholderExpansion != null) {
                try {
                    this.placeholderExpansion.unregister();
                } catch (Exception e) {
                    getLogger().warning("Failed to unregister existing PlaceholderAPI expansion: " + e.getMessage());
                }
            }

            // Create and register the new expansion
            this.placeholderExpansion = new VeinMinerPlaceholders(this);

            // Register with a delay to ensure everything is loaded
            getServer().getScheduler().runTaskLater(this, () -> {
                if (this.placeholderExpansion.register()) {
                    getLogger().info("Successfully registered VeinMiner placeholders!");
                } else {
                    getLogger().warning("Failed to register VeinMiner placeholders!");
                }
            }, 40L); // 2 seconds delay
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholders will not be available.");
        }

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

        // Unregister PlaceholderAPI expansion if it exists
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
            } catch (Exception e) {
                getLogger().warning("Failed to unregister PlaceholderAPI expansion: " + e.getMessage());
            }
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

            // Add a delayed second synchronization to ensure changes are applied
            getServer().getScheduler().runTaskLater(this, () -> {
                if (debugMode) {
                    getLogger().info("[Debug] Running follow-up synchronization...");
                }
                databaseManager.synchronizeData();
            }, 10L); // 0.5 seconds later
        }
    }

    // Add a method to force synchronization
    public void forceSyncDataNow() {
        if (databaseManager != null) {
            if (debugMode) {
                getLogger().info("[Debug] Forcing data synchronization across all servers...");
            }
            databaseManager.forceSyncNow();
        }
    }

    // Start auto-save task including synchronization
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

        // Additional task for more frequent synchronization (every 30 seconds)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (databaseManager != null && !databaseManager.isFallbackMode()) {
                if (debugMode) {
                    getLogger().info("[Debug] Running frequent synchronization check...");
                }
                databaseManager.synchronizeData();
            }

            // Also refresh PlaceholderAPI cache if it exists
            if (placeholderExpansion != null) {
                placeholderExpansion.forceRefreshCache();
                if (debugMode) {
                    getLogger().info("[Debug] Refreshed PlaceholderAPI cache.");
                }
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

    /**
     * Set the debug mode
     * @param debugMode true to enable debug mode, false to disable it
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        getLogger().info("Debug mode has been " + (debugMode ? "enabled" : "disabled") + ".");
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

    public SkillGUI getSkillGUI() {
        return skillGUI;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    /**
     * Get the top players GUI
     * @return The top players GUI
     */
    public TopPlayersGUI getTopPlayersGUI() {
        return topPlayersGUI;
    }

    /**
     * Get the PlaceholderAPI expansion
     * @return The PlaceholderAPI expansion
     */
    public VeinMinerPlaceholders getPlaceholderExpansion() {
        return placeholderExpansion;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public VeinMinerUtils getVeinMinerUtils() {
        return veinMinerUtils;
    }

    /**
     * Get the WorldGuard hook
     * @return The WorldGuard hook
     */
    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    /**
     * Get the update checker
     * @return The update checker
     */
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    // Add this getter method
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

    // Save default messages.yml if it doesn't exist
    public void saveDefaultMessagesConfig() {
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

    // Update database schema if needed
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

    /**
     * Get the bStats server UUID for this server
     * @return The bStats server UUID or "Unknown" if not available
     */
    public String getBStatsServerUUID() {
        File bStatsConfigFile = new File("plugins/bStats/config.yml");
        if (bStatsConfigFile.exists()) {
            YamlConfiguration bStatsConfig = YamlConfiguration.loadConfiguration(bStatsConfigFile);
            return bStatsConfig.getString("serverUuid", "Unknown");
        }
        return "Unknown";
    }
}