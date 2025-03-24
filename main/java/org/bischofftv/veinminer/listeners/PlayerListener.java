package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public class PlayerListener implements Listener {

    private final Veinminer plugin;

    public PlayerListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data
        plugin.getPlayerDataManager().loadPlayerData(player);

        // Load player achievements if achievement system is enabled
        if (plugin.getAchievementManager().isEnabled()) {
            plugin.getAchievementManager().loadPlayerAchievements(player);

            // Check for level achievements
            if (plugin.getLevelManager().isEnabled()) {
                int level = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getLevel();
                plugin.getAchievementManager().updateLevelProgress(player, level);
            }

            // Check for skill master achievement
            if (plugin.getSkillManager().isEnabled()) {
                plugin.getAchievementManager().updateSkillMasterProgress(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save player data
        plugin.getPlayerDataManager().savePlayerData(player);

        // Save player achievements if achievement system is enabled
        if (plugin.getAchievementManager().isEnabled()) {
            plugin.getAchievementManager().savePlayerAchievements(player);
        }

        // Unload player data
        plugin.getPlayerDataManager().unloadPlayerData(player);
    }
}