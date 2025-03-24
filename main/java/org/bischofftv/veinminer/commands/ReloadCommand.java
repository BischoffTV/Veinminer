package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final Veinminer plugin;

    public ReloadCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("veinminer.admin.reload")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.no-permission", "You don't have permission to use this command."));
            return true;
        }

        // Reload configurations
        plugin.reloadConfig();
        plugin.reloadLangConfig();
        plugin.reloadMessagesConfig();

        // Reload managers
        if (plugin.getConfigManager() != null) {
            plugin.getConfigManager().reload();
        }

        if (plugin.getLevelManager() != null) {
            plugin.getLevelManager().reloadLevelSettings();
        }

        // Reload achievements last to ensure database schema is updated
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().reloadAchievements();
        }

        // Restart auto-save task
        plugin.restartAutoSaveTask();

        // Send confirmation message
        sender.sendMessage(plugin.getMessageManager().formatMessage("messages.reload.success", "VeinMiner configuration has been reloaded."));
        return true;
    }
}