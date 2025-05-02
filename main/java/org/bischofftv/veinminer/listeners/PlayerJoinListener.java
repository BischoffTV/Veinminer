package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {

    private final Veinminer plugin;

    public PlayerJoinListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data with a slight delay to ensure everything is ready
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Loading data for player " + player.getName());
                    }

                    // Load player data
                    plugin.getPlayerDataManager().loadPlayerData(player);

                    // Load achievements if enabled
                    if (plugin.getAchievementManager().isEnabled()) {
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Loading achievements for player " + player.getName());
                        }
                        plugin.getAchievementManager().loadPlayerAchievements(player);
                    }

                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Finished loading data for player " + player.getName());
                    }
                }
            }
        }.runTaskLater(plugin, 20L); // 1 second delay

        // Send update notification if available
        plugin.getUpdateChecker().sendUpdateNotification(player);
    }
} 