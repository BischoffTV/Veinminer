package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.gui.MainGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final Veinminer plugin;

    public MainCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Zeige Hilfe oder öffne GUI
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (plugin.getMainGUI() != null) {
                    plugin.getMainGUI().openMainMenu(player);
                } else {
                    sendHelpMessage(sender);
                }
            } else {
                sendHelpMessage(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelpMessage(sender);
                break;
            case "reload":
                if (!sender.hasPermission("veinminer.command.reload")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                plugin.reloadConfig();
                plugin.reloadMessagesConfig();

                if (plugin.getConfigManager() != null) {
                    plugin.getConfigManager().reload();
                }

                if (plugin.getLevelManager() != null) {
                    plugin.getLevelManager().reloadLevelSettings();
                }

                // Reload achievements to ensure new ones are registered
                if (plugin.getAchievementManager() != null) {
                    plugin.getAchievementManager().reloadAchievements();
                }

                plugin.restartAutoSaveTask();

                sender.sendMessage(ChatColor.GREEN + "VeinMiner configuration reloaded!");
                break;
            case "debug":
                if (!sender.hasPermission("veinminer.command.debug")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                boolean newDebugState = !plugin.isDebugMode();
                plugin.setDebugMode(newDebugState);
                sender.sendMessage(ChatColor.GREEN + "Debug mode " + (newDebugState ? "enabled" : "disabled") + ".");
                break;
            case "version":
                sender.sendMessage(ChatColor.GREEN + "VeinMiner v" + plugin.getDescription().getVersion());
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /veinminer help for a list of commands.");
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== VeinMiner Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer " + ChatColor.WHITE + "- Open the main menu (if GUI is enabled)");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer help " + ChatColor.WHITE + "- Show this help message");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer reload " + ChatColor.WHITE + "- Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer debug " + ChatColor.WHITE + "- Toggle debug mode");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer version " + ChatColor.WHITE + "- Show the plugin version");
        sender.sendMessage(ChatColor.YELLOW + "/vmtoggle " + ChatColor.WHITE + "- Toggle VeinMiner on/off");
        sender.sendMessage(ChatColor.YELLOW + "/vmlevel " + ChatColor.WHITE + "- Check your current level");
        sender.sendMessage(ChatColor.YELLOW + "/vmsetlevel <player> <level> " + ChatColor.WHITE + "- Set a player's level");
        sender.sendMessage(ChatColor.YELLOW + "/vmsync " + ChatColor.WHITE + "- Force data synchronization");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> subCommands = Arrays.asList("help", "reload", "debug", "version");

            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    if (subCommand.equals("reload") && !sender.hasPermission("veinminer.command.reload")) {
                        continue;
                    }
                    if (subCommand.equals("debug") && !sender.hasPermission("veinminer.command.debug")) {
                        continue;
                    }
                    completions.add(subCommand);
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }
}