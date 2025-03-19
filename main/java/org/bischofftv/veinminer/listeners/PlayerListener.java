package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.achievements.AchievementManager;
import org.bischofftv.veinminer.database.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final Veinminer plugin;

    public PlayerListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getDatabaseManager() != null && !plugin.getDatabaseManager().isFallbackMode()) {
            plugin.syncDataNow();
        }

        // Load player data when they join
        plugin.getPlayerDataManager().loadPlayerData(player);

        // Load achievements with improved handling
        if (plugin.getAchievementManager().isEnabled()) {
            // Schedule immediate achievement load
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                if (plugin.isDebugMode()) {
                    plugin.debug("Loading achievements for " + player.getName() + " after join");
                }

                // Force clear any existing achievement data for this player
                UUID uuid = player.getUniqueId();
                Map<String, AchievementManager.PlayerAchievement> existingAchievements =
                        plugin.getAchievementManager().getPlayerAchievements(player);

                if (existingAchievements != null) {
                    existingAchievements.clear();
                }

                plugin.getAchievementManager().loadPlayerAchievements(player);

                // Force a synchronization after loading
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.forceSyncDataNow();
                }, 20L); // 1 second delay
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save player data when they quit
        plugin.getPlayerDataManager().savePlayerData(player);

        // Save achievements if enabled
        if (plugin.getAchievementManager().isEnabled()) {
            plugin.getAchievementManager().savePlayerAchievements(player);
        }

        // Force sync data to ensure changes are saved immediately
        plugin.forceSyncDataNow();

        // Unload player data to free up memory
        plugin.getPlayerDataManager().unloadPlayerData(player);
    }
}