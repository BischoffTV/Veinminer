package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;

/**
 * Listener for achievement-related events
 */
public class AchievementListener implements Listener {

    private final Veinminer plugin;

    public AchievementListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle block break events for block-specific achievements
     * @param event The block break event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getAchievementManager().isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Update block mine achievements
        plugin.getAchievementManager().updateBlockMineAchievements(player, blockType.toString(), 1);
    }

    /**
     * Handle player level change events for level-based achievements
     * @param event The player level change event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        if (!plugin.getAchievementManager().isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();

        // Update level achievements
        plugin.getAchievementManager().updateLevelAchievements(player, newLevel);
    }

    /**
     * Handle VeinMiner level up events
     * @param player The player
     * @param newLevel The new level
     */
    public void onVeinMinerLevelUp(Player player, int newLevel) {
        if (!plugin.getAchievementManager().isEnabled()) {
            return;
        }

        // Update level achievements
        plugin.getAchievementManager().updateLevelAchievements(player, newLevel);
    }

    /**
     * Handle skill upgrade events for skill-based achievements
     * @param player The player
     * @param skillType The skill type
     * @param newLevel The new skill level
     */
    public void onSkillUpgrade(Player player, String skillType, int newLevel) {
        if (!plugin.getAchievementManager().isEnabled() || !plugin.getSkillManager().isEnabled()) {
            return;
        }

        // Check for skill master achievement
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        int maxLevel = plugin.getSkillManager().getMaxSkillLevel();
        if (playerData.getEfficiencyLevel() >= maxLevel &&
                playerData.getLuckLevel() >= maxLevel &&
                playerData.getEnergyLevel() >= maxLevel) {

            // Find skill master achievements
            plugin.getAchievementManager().updateAchievementProgress(player, "skill_master", 1);
        }
    }
}

