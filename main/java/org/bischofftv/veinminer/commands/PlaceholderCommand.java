package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to manage PlaceholderAPI integration
 */
public class PlaceholderCommand implements CommandExecutor, TabCompleter {

    private final Veinminer plugin;

    public PlaceholderCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /vmplaceholder reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("veinminer.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            if (plugin.getPlaceholderExpansion() != null) {
                plugin.getPlaceholderExpansion().forceRefreshCache();
                sender.sendMessage(ChatColor.GREEN + "PlaceholderAPI cache has been refreshed!");
            } else {
                sender.sendMessage(ChatColor.RED + "PlaceholderAPI integration is not enabled.");
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
        }

        return completions;
    }
}