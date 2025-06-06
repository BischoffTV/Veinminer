package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AchievementsCommand implements CommandExecutor {

    private final Veinminer plugin;

    public AchievementsCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.player-only", "This command can only be used by players."));
            return true;
        }

        Player player = (Player) sender;

        // Check if achievements are enabled
        if (!plugin.getAchievementManager().isEnabled()) {
            player.sendMessage(ChatColor.RED + "Achievements are currently disabled.");
            return true;
        }

        // Open the achievements GUI
        plugin.getAchievementGUI().openAchievementGUI(player);
        return true;
    }
}