package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerListener implements Listener {

    private final Veinminer plugin;

    public PlayerListener(Veinminer plugin) {
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
                    // Load player data
                    plugin.getPlayerDataManager().loadPlayerData(player);

                    // Load achievements if enabled
                    if (plugin.getAchievementManager().isEnabled()) {
                        plugin.getAchievementManager().loadPlayerAchievements(player);
                    }

                    // Debug log
                    if (plugin.isDebugMode()) {
                        plugin.debug("Loaded data for player " + player.getName());
                    }
                }
            }
        }.runTaskLater(plugin, 10L); // 0.5 second delay

        // Check for updates if player has permission
        if (plugin.getUpdateChecker() != null && player.hasPermission("veinminer.admin")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        plugin.getUpdateChecker().checkForUpdates();

                        // If there's an update, notify the player
                        if (plugin.getUpdateChecker().isUpdateAvailable()) {
                            player.sendMessage(plugin.getMessageManager().formatMessage("messages.update.available",
                                    "%current%", plugin.getDescription().getVersion(),
                                    "%latest%", plugin.getUpdateChecker().getLatestVersion()));
                        }
                    }
                }
            }.runTaskLaterAsynchronously(plugin, 60L); // 3 second delay
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save player data
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

        // Save achievements if enabled
        if (plugin.getAchievementManager().isEnabled()) {
            plugin.getAchievementManager().savePlayerAchievements(player.getUniqueId());
        }

        // Remove player data from memory
        plugin.getPlayerDataManager().removePlayerData(player.getUniqueId());

        // Debug log
        if (plugin.isDebugMode()) {
            plugin.debug("Saved and removed data for player " + player.getName());
        }
    }
}