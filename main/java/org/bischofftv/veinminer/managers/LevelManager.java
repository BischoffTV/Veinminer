package org.bischofftv.veinminer.managers;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.entity.Player;

public class LevelManager {
    private final Veinminer plugin;
    private final PlayerDataManager playerDataManager;

    public LevelManager(Veinminer plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void addExperience(Player player, int amount) {
        playerDataManager.addPlayerExperience(player, amount);
        checkLevelUp(player);
    }

    private void checkLevelUp(Player player) {
        int currentLevel = playerDataManager.getPlayerLevel(player);
        int currentExp = playerDataManager.getPlayerExperience(player);
        int requiredExp = getRequiredExperience(currentLevel);

        while (currentExp >= requiredExp) {
            currentLevel++;
            currentExp -= requiredExp;
            playerDataManager.setPlayerLevel(player, currentLevel);
            playerDataManager.setPlayerExperience(player, currentExp);
            
            // Notify player of level up
            player.sendMessage(plugin.getMessageManager().getMessage("levelup", String.valueOf(currentLevel)));
            
            // Check for next level
            requiredExp = getRequiredExperience(currentLevel);
        }
    }

    public int getRequiredExperience(int level) {
        // Simple exponential experience formula: 100 * level^1.5
        return (int) (100 * Math.pow(level, 1.5));
    }

    public int getCurrentLevel(Player player) {
        return playerDataManager.getPlayerLevel(player);
    }

    public int getCurrentExperience(Player player) {
        return playerDataManager.getPlayerExperience(player);
    }

    public int getRequiredExperienceForNextLevel(Player player) {
        return getRequiredExperience(getCurrentLevel(player));
    }
} 