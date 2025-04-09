package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LevelSetCommand implements CommandExecutor, TabCompleter {

    private final Veinminer plugin;

    public LevelSetCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender has permission
        if (!sender.hasPermission("veinminer.admin.setlevel")) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
            return true;
        }

        // Check if the level system is enabled
        if (!plugin.getLevelManager().isEnabled()) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.level.disabled"));
            return true;
        }

        // Check arguments
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-args"));
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.usage", "%command%", "/vmsetlevel <player> <level>"));
            return true;
        }

        // Get target player
        String playerName = args[0];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.player-not-found", "%player%", playerName));
            return true;
        }

        // Parse level
        int level;
        try {
            level = Integer.parseInt(args[1]);
            if (level < 1) {
                sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-level"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-level"));
            return true;
        }

        // Set level
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(targetPlayer.getUniqueId());
        if (playerData != null) {
            playerData.setLevel(level);

            // Send messages
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.level.set",
                    "%player%", targetPlayer.getName(),
                    "%level%", String.valueOf(level)));

            if (sender != targetPlayer) {
                targetPlayer.sendMessage(plugin.getMessageManager().formatMessage("messages.level.set-target",
                        "%level%", String.valueOf(level)));
            }

            // Check for level-based achievements
            if (plugin.getAchievementManager().isEnabled()) {
                plugin.getAchievementManager().updateLevelAchievements(targetPlayer, level);
            }
        } else {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.error.player-data-not-found"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            // Suggest some level values
            for (int i = 1; i <= 10; i++) {
                completions.add(String.valueOf(i));
            }
        }

        return completions;
    }
}