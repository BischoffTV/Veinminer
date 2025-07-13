package org.bischofftv.veinminer;

import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class VeinMinerAPIImpl implements VeinMinerAPI {
    private final Veinminer plugin;
    private final List<BlockMinedListener> blockMinedListeners = new CopyOnWriteArrayList<>();
    private final List<LevelUpListener> levelUpListeners = new CopyOnWriteArrayList<>();
    private final List<AchievementCompletedListener> achievementCompletedListeners = new CopyOnWriteArrayList<>();

    public VeinMinerAPIImpl(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Integer> getPlayerLevel(UUID playerId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int level = 1;
            if (plugin.getPlayerDataManager() != null && plugin.getPlayerDataManager().getPlayerData(playerId) != null) {
                level = plugin.getPlayerDataManager().getPlayerData(playerId).getLevel();
            }
            future.complete(level);
        });
        return future;
    }

    @Override
    public CompletableFuture<Map<String, Integer>> getPlayerBlockStats(UUID playerId) {
        CompletableFuture<Map<String, Integer>> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Integer> stats = new HashMap<>();
            if (plugin.getPlayerDataManager() != null && plugin.getPlayerDataManager().getPlayerData(playerId) != null) {
                // Beispiel: Nur Gesamtanzahl aller Blöcke
                stats.put("total", (int) plugin.getPlayerDataManager().getPlayerData(playerId).getBlocksMined());
                // TODO: Nach Blocktyp differenzieren, falls unterstützt
            }
            future.complete(stats);
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> hasAchievement(UUID playerId, String achievementId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean result = false;
            if (plugin.getAchievementManager() != null && plugin.getPlayerDataManager() != null) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) {
                    result = plugin.getAchievementManager().hasCompletedAchievement(player, achievementId);
                }
            }
            future.complete(result);
        });
        return future;
    }

    @Override
    public CompletableFuture<List<String>> getCompletedAchievements(UUID playerId) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> completed = new ArrayList<>();
            if (plugin.getAchievementManager() != null && plugin.getPlayerDataManager() != null) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) {
                    for (String id : plugin.getAchievementManager().getAchievementDefinitions().keySet()) {
                        if (plugin.getAchievementManager().hasCompletedAchievement(player, id)) {
                            completed.add(id);
                        }
                    }
                }
            }
            future.complete(completed);
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> isVeinMinerEnabled(UUID playerId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean enabled = false;
            if (plugin.getPlayerDataManager() != null && plugin.getPlayerDataManager().getPlayerData(playerId) != null) {
                enabled = plugin.getPlayerDataManager().getPlayerData(playerId).isVeinMinerEnabled();
            }
            future.complete(enabled);
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> setVeinMinerEnabled(UUID playerId, boolean enabled) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getPlayerDataManager() != null && plugin.getPlayerDataManager().getPlayerData(playerId) != null) {
                plugin.getPlayerDataManager().getPlayerData(playerId).setVeinMinerEnabled(enabled);
            }
            future.complete(null);
        });
        return future;
    }

    @Override
    public void addBlockMinedListener(BlockMinedListener listener) {
        blockMinedListeners.add(listener);
    }

    @Override
    public void addLevelUpListener(LevelUpListener listener) {
        levelUpListeners.add(listener);
    }

    @Override
    public void addAchievementCompletedListener(AchievementCompletedListener listener) {
        achievementCompletedListeners.add(listener);
    }

    // Methods to fire events (to be called from plugin internals)
    public void fireBlockMined(Player player, Block block, int amount) {
        for (BlockMinedListener listener : blockMinedListeners) {
            listener.onBlockMined(player, block, amount);
        }
    }
    public void fireLevelUp(Player player, int newLevel) {
        for (LevelUpListener listener : levelUpListeners) {
            listener.onLevelUp(player, newLevel);
        }
    }
    public void fireAchievementCompleted(Player player, String achievementId) {
        for (AchievementCompletedListener listener : achievementCompletedListeners) {
            listener.onAchievementCompleted(player, achievementId);
        }
    }
} 