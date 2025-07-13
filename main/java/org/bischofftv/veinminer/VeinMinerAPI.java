package org.bischofftv.veinminer;

import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface VeinMinerAPI {
    // Get the current VeinMiner level of a player
    CompletableFuture<Integer> getPlayerLevel(UUID playerId);

    // Get the number of blocks mined by a player (by block type)
    CompletableFuture<Map<String, Integer>> getPlayerBlockStats(UUID playerId);

    // Check if a player has completed a specific achievement
    CompletableFuture<Boolean> hasAchievement(UUID playerId, String achievementId);

    // Get a list of completed achievements for a player
    CompletableFuture<List<String>> getCompletedAchievements(UUID playerId);

    // Check if VeinMiner is enabled for a player
    CompletableFuture<Boolean> isVeinMinerEnabled(UUID playerId);

    // Enable or disable VeinMiner for a player
    CompletableFuture<Void> setVeinMinerEnabled(UUID playerId, boolean enabled);

    // Register a listener for block mined events
    void addBlockMinedListener(BlockMinedListener listener);

    // Register a listener for level up events
    void addLevelUpListener(LevelUpListener listener);

    // Register a listener for achievement completed events
    void addAchievementCompletedListener(AchievementCompletedListener listener);

    // Listener interfaces
    interface BlockMinedListener {
        void onBlockMined(Player player, Block block, int amount);
    }
    interface LevelUpListener {
        void onLevelUp(Player player, int newLevel);
    }
    interface AchievementCompletedListener {
        void onAchievementCompleted(Player player, String achievementId);
    }
} 