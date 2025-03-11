package org.bischofftv.veinminer;

import org.bischofftv.veinminer.commands.*;
import org.bischofftv.veinminer.config.ConfigManager;
import org.bischofftv.veinminer.database.DatabaseManager;
import org.bischofftv.veinminer.listeners.BlockBreakListener;
import org.bischofftv.veinminer.listeners.PlayerListener;
import org.bischofftv.veinminer.logging.MiningLogger;
import org.bischofftv.veinminer.utils.LevelManager;
import org.bischofftv.veinminer.utils.MessageManager;
import org.bischofftv.veinminer.utils.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Veinminer extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private MiningLogger miningLogger;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private LevelManager levelManager;

    @Override
    public void onEnable() {
        // Initialize managers
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);

        // Load configurations
        configManager.loadConfig();

        // Initialize database and level system
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        levelManager = new LevelManager(this);

        // Initialize player data manager after database
        playerDataManager = new PlayerDataManager(this);

        // Initialize logger
        miningLogger = new MiningLogger(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Register commands
        ToggleCommand toggleCommand = new ToggleCommand(this);
        getCommand("veinminer").setExecutor(toggleCommand);
        getCommand("veinminer").setTabCompleter(new VeinMinerTabCompleter());

        getCommand("veinminerreload").setExecutor(new ReloadCommand(this));
        getCommand("veinminerhelp").setExecutor(new HelpCommand(this));
        getCommand("veinminerabout").setExecutor(new AboutCommand(this));

        getCommand("veinminer").setExecutor(new CommandHandler(this));

        // Load data for online players (in case of reload)
        getServer().getOnlinePlayers().forEach(player -> playerDataManager.loadPlayerData(player));

        getLogger().info("VeinMiner has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all player data
        playerDataManager.saveAllData();

        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("VeinMiner has been disabled!");
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
}

