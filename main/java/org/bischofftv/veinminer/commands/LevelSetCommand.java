package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LevelSetCommand implements CommandExecutor {

    private final Veinminer plugin;

    public LevelSetCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("veinminer.admin.setlevel")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.no-permission", "You don't have permission to use this command."));
            return true;
        }

        // Check if level system is enabled
        if (!plugin.getLevelManager().isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Level system is currently disabled.");
            return true;
        }

        // Check arguments
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /vmsetlevel <player> <level>");
            return true;
        }

        // Get target player
        String playerName = args[0];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.player-not-found", "Player not found: %player%").replace("%player%", playerName));
            return true;
        }

        // Parse level
        int level;
        try {
            level = Integer.parseInt(args[1]);
            if (level < 1) {
                sender.sendMessage(ChatColor.RED + "Level must be at least 1.");
                return true;
            }
            if (level > plugin.getLevelManager().getMaxLevel()) {
                sender.sendMessage(ChatColor.RED + "Level cannot be higher than " + plugin.getLevelManager().getMaxLevel() + ".");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid level. Please enter a number.");
            return true;
        }

        // Set player level
        plugin.getLevelManager().setPlayerLevel(targetPlayer, level);

        // Send confirmation message
        String message = plugin.getMessageManager().formatMessage("messages.level.set")
                .replace("%player%", targetPlayer.getName())
                .replace("%level%", String.valueOf(level));

        sender.sendMessage(message);
        return true;
    }
}