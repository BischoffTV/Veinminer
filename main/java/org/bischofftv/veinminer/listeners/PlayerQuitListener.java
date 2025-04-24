package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final Veinminer plugin;

    public PlayerQuitListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save player data when they quit
        plugin.getPlayerDataManager().savePlayerSettings(player);
        
        // Save achievement data if enabled
        if (plugin.getAchievementManager().isEnabled()) {
            plugin.getAchievementManager().savePlayerAchievements(player.getUniqueId());
        }
    }
} 