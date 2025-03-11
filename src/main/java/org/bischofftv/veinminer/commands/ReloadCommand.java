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

        sender.sendMessage(plugin.getMessageManager().getMessage("messages.reload.success"));

        return true;
    }
}

