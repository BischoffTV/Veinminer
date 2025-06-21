package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final Veinminer plugin;

    public ReloadCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender has permission
        if (!sender.hasPermission("veinminer.admin.reload")) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
            return true;
        }

        // Reload the plugin configuration
        plugin.reloadConfig();
        plugin.reloadLangConfig();
        plugin.getMessageManager().reload();
        plugin.getConfigManager().loadConfig();
        plugin.getLevelManager().loadConfig();
        plugin.getSkillManager().loadConfig();
        plugin.getAchievementManager().loadConfig();

        // Restart auto-save task
        plugin.restartAutoSaveTask();

        // Send success message
        sender.sendMessage(plugin.getMessageManager().formatMessage("messages.reload.success"));

        // Log reload
        plugin.getLogger().info("Plugin configuration reloaded by " + sender.getName());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }
}