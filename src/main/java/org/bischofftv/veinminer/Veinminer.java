package org.bischofftv.veinminer;

import org.bischofftv.veinminer.achievements.AchievementGUI;
import org.bischofftv.veinminer.achievements.AchievementManager;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

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

@Override
public void onEnable() {
    configManager = new ConfigManager(this);
    messageManager = new MessageManager(this);
    
    configManager.loadConfig();
    
    debugMode = getConfig().getBoolean("settings.debug", false);
    
    databaseManager = new DatabaseManager(this);
    databaseManager.initialize();
    
    levelManager = new LevelManager(this);
    
    achievementManager = new AchievementManager(this);
    achievementGUI = new AchievementGUI(this);
    
    mainGUI = new MainGUI(this);

    playerDataManager = new PlayerDataManager(this);

    miningLogger = new MiningLogger(this);
    
    getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
    getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    getServer().getPluginManager().registerEvents(new AchievementListener(this), this);
    getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    
    CommandHandler commandHandler = new CommandHandler(this);
    getCommand("veinminer").setExecutor(commandHandler);
    getCommand("veinminer").setTabCompleter(new VeinMinerTabCompleter());
    
    getCommand("veinminerreload").setExecutor(new ReloadCommand(this));
    getCommand("veinminerhelp").setExecutor(new HelpCommand(this));
    getCommand("veinminerabout").setExecutor(new AboutCommand(this));
    
    getServer().getOnlinePlayers().forEach(player -> {
        playerDataManager.loadPlayerData(player);
        if (achievementManager.isEnabled()) {
            achievementManager.loadPlayerAchievements(player);
        }
    });
    
    startAutoSaveTask();
    
    getLogger().info("VeinMiner has been enabled!");
}

@Override
public void onDisable() {
    if (autoSaveTask != null) {
        autoSaveTask.cancel();
        autoSaveTask = null;
    }
    
    playerDataManager.saveAllData();
    
    if (achievementManager != null) {
        achievementManager.saveAllAchievements();
    }
    
    if (databaseManager != null) {
        databaseManager.close();
    }
    
    getLogger().info("VeinMiner has been disabled!");
}

public void startAutoSaveTask() {
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
        
        if (debugMode) {
            getLogger().info("[Debug] Auto-save completed.");
        }
    }, intervalTicks, intervalTicks);
    
    getLogger().info("Auto-save scheduled every " + intervalMinutes + " minutes.");
}

public void restartAutoSaveTask() {
    startAutoSaveTask();
}

public void debug(String message) {
    if (debugMode) {
        getLogger().info("[Debug] " + message);
    }
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
}
