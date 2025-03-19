package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements CommandExecutor {

    private final Veinminer plugin;

    public ReloadCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("veinminer.reload")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.no-permission"));
            return true;
        }

        // Save all player data before reloading
        plugin.getPlayerDataManager().saveAllData();

        // Reload configuration
        plugin.getConfigManager().reloadConfig();
        plugin.getMessageManager().reloadMessages();

        // Reinitialize level manager to load new level settings
        plugin.getLevelManager().reloadLevelSettings();

        // Reload achievement settings and force reload for all players
        if (plugin.getAchievementManager().isEnabled()) {
            plugin.getAchievementManager().loadSettings();

            // Force reload achievements for all online players
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                // Clear existing achievement data for this player
                plugin.getAchievementManager().clearPlayerAchievements(player);
                // Load achievements again
                plugin.getAchievementManager().loadPlayerAchievements(player);
            }

            sender.sendMessage(ChatColor.GREEN + "Achievements have been reloaded for all online players.");
        }

        // Restart auto-save task with new interval
        plugin.restartAutoSaveTask();

        // Force sync to ensure changes are propagated
        plugin.forceSyncDataNow();

        sender.sendMessage(plugin.getMessageManager().getMessage("messages.reload.success"));

        return true;
    }
}