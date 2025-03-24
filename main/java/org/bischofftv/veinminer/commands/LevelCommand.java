package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LevelCommand implements CommandExecutor {

    private final Veinminer plugin;

    public LevelCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.player-only", "This command can only be used by players."));
            return true;
        }

        Player player = (Player) sender;

        // Check if level system is enabled
        if (!plugin.getLevelManager().isEnabled()) {
            player.sendMessage(ChatColor.RED + "Level system is currently disabled.");
            return true;
        }

        // Get player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Failed to load your player data. Please try again later.");
            return true;
        }

        // Get level information
        int level = playerData.getLevel();
        int experience = playerData.getExperience();
        int nextLevelExperience = plugin.getLevelManager().getXpForNextLevel(level + 1);

        // Send level information
        String message = plugin.getMessageManager().formatMessage("messages.level.info")
                .replace("%level%", String.valueOf(level))
                .replace("%experience%", String.valueOf(experience))
                .replace("%next_level_experience%", String.valueOf(nextLevelExperience));

        player.sendMessage(message);
        return true;
    }
}