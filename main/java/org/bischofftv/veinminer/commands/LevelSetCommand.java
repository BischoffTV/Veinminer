package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.database.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class LevelSetCommand implements CommandExecutor {

    private final Veinminer plugin;

    public LevelSetCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    // Korrigiere die Methode onCommand, um die Mehrdeutigkeit zu vermeiden
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("veinminer.admin.setlevel")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.no-permission"));
            return true;
        }

        // Check if level system is enabled
        if (!plugin.getLevelManager().isEnabled()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.level.system-disabled"));
            return true;
        }

        // Check arguments
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.level.set.usage"));
            return true;
        }

        // Get target player
        String playerName = args[0];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", playerName);
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.level.set.player-not-found", placeholders));
            return true;
        }

        // Parse level
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.level.set.invalid-level"));
            return true;
        }

        // Validate level
        int maxLevel = plugin.getLevelManager().getMaxLevel();
        plugin.getLogger().info("Setting level for " + targetPlayer.getName() +
                " - Requested level: " + level +
                ", Max level: " + maxLevel);

        // Log all available levels
        plugin.getLogger().info("Available levels in config:");
        Map<Integer, Long> levels = plugin.getLevelManager().getXpPerLevel();
        for (Map.Entry<Integer, Long> entry : levels.entrySet()) {
            plugin.getLogger().info("- Level " + entry.getKey() + ": " + entry.getValue() + " XP");
        }

        // Force maxLevel to be at least 10 for troubleshooting
        if (maxLevel < 10) {
            plugin.getLogger().warning("Max level is less than 10! Forcing to 10 for troubleshooting.");
            maxLevel = 10;
        }

        if (level < 1 || level > maxLevel) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max_level", String.valueOf(maxLevel));
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.level.set.level-out-of-range", placeholders));
            return true;
        }

        // Set the player's level using the LevelManager
        boolean success = plugin.getLevelManager().setPlayerLevel(targetPlayer, level);

        if (success) {
            // Send success messages
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetPlayer.getName());
            placeholders.put("level", String.valueOf(level));

            sender.sendMessage(plugin.getMessageManager().getMessage("messages.level.set.success", placeholders));

            // Notify target player if different from sender
            if (sender != targetPlayer) {
                Map<String, String> playerPlaceholders = new HashMap<>();
                playerPlaceholders.put("level", String.valueOf(level));
                playerPlaceholders.put("admin", sender.getName());
                targetPlayer.sendMessage(plugin.getMessageManager().getMessage("messages.level.set.notify", playerPlaceholders));
            }

            // Force synchronization
            plugin.forceSyncDataNow();
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.level.set.failed"));
        }

        return true;
    }
}