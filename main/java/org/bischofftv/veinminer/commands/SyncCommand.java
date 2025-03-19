package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SyncCommand implements CommandExecutor, TabCompleter {

    private final Veinminer plugin;

    public SyncCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("veinminer.command.sync")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("schema")) {
            // Force schema update
            if (plugin.getDatabaseManager().isFallbackMode()) {
                sender.sendMessage(ChatColor.RED + "Cannot update schema in fallback mode.");
                return true;
            }

            try {
                plugin.getDatabaseManager().updateDatabaseSchema();
                sender.sendMessage(ChatColor.GREEN + "Database schema update triggered. Check console for more information.");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Error updating database schema: " + e.getMessage());
            }
            return true;
        }

        // Regular sync command
        if (plugin.getDatabaseManager().isFallbackMode()) {
            sender.sendMessage(ChatColor.RED + "Cannot sync data in fallback mode. Database connection is not available.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Forcing data synchronization...");

        try {
            plugin.forceSyncDataNow();
            sender.sendMessage(ChatColor.GREEN + "Data synchronization complete!");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error during synchronization: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("veinminer.command.sync")) {
                if ("schema".startsWith(args[0].toLowerCase())) {
                    completions.add("schema");
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }
}