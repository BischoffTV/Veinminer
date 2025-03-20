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
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final Veinminer plugin;

    public AdminCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("veinminer.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "debug":
                toggleDebugMode(sender);
                break;
            case "reload":
                reloadPlugin(sender);
                break;
            case "sync":
                syncData(sender);
                break;
            case "forcesync":
                forceSyncData(sender);
                break;
            case "bstats":
                checkBStats(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void toggleDebugMode(CommandSender sender) {
        boolean newDebugMode = !plugin.isDebugMode();
        plugin.setDebugMode(newDebugMode);
        sender.sendMessage(ChatColor.GREEN + "Debug mode " + (newDebugMode ? "enabled" : "disabled") + ".");
    }

    private void reloadPlugin(CommandSender sender) {
        plugin.getConfigManager().reloadConfig();
        plugin.restartAutoSaveTask();
        sender.sendMessage(ChatColor.GREEN + "VeinMiner configuration reloaded.");
    }

    private void syncData(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Synchronizing data...");
        plugin.syncDataNow();
        sender.sendMessage(ChatColor.GREEN + "Data synchronization triggered.");
    }

    private void forceSyncData(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Forcing data synchronization across all servers...");
        plugin.forceSyncDataNow();
        sender.sendMessage(ChatColor.GREEN + "Forced data synchronization triggered.");
    }

    private void checkBStats(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== bStats Information ===");
        sender.sendMessage(ChatColor.YELLOW + "Plugin ID: " + ChatColor.WHITE + "25161");
        sender.sendMessage(ChatColor.YELLOW + "Server UUID: " + ChatColor.WHITE + plugin.getBStatsServerUUID());

        // Show current metrics
        sender.sendMessage(ChatColor.YELLOW + "Current Metrics:");
        sender.sendMessage(ChatColor.YELLOW + "- Database Type: " + ChatColor.WHITE +
                (plugin.getDatabaseManager() != null ?
                        (plugin.getDatabaseManager().isFallbackMode() ? "SQLite" : "MySQL") :
                        "Unknown"));

        sender.sendMessage(ChatColor.YELLOW + "- Discord Integration: " + ChatColor.WHITE +
                (plugin.getConfigManager() != null ?
                        (plugin.getConfigManager().isEnableDiscordLogging() ? "Enabled" : "Disabled") :
                        "Unknown"));

        sender.sendMessage(ChatColor.YELLOW + "- Achievement System: " + ChatColor.WHITE +
                (plugin.getAchievementManager() != null ?
                        (plugin.getAchievementManager().isEnabled() ? "Enabled" : "Disabled") :
                        "Unknown"));

        sender.sendMessage(ChatColor.YELLOW + "- Blocks Mined Today: " + ChatColor.WHITE +
                (plugin.getVeinMinerUtils() != null ?
                        plugin.getVeinMinerUtils().getTotalBlocksMinedToday() :
                        "0"));

        sender.sendMessage(ChatColor.YELLOW + "- Active Players: " + ChatColor.WHITE +
                plugin.getServer().getOnlinePlayers().size());

        // Increment blocks mined for testing
        if (plugin.getVeinMinerUtils() != null) {
            plugin.getVeinMinerUtils().incrementBlocksMined(1);
            sender.sendMessage(ChatColor.GREEN + "Incremented blocks mined counter by 1 for testing.");
        }

        sender.sendMessage(ChatColor.YELLOW + "Dashboard: " + ChatColor.WHITE +
                "https://bstats.org/plugin/bukkit/VeinMiner/25161");

        sender.sendMessage(ChatColor.GREEN + "Note: It may take up to 30 minutes for new data to appear on the dashboard.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== VeinMiner Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin debug" + ChatColor.WHITE + " - Toggle debug mode");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin reload" + ChatColor.WHITE + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin sync" + ChatColor.WHITE + " - Synchronize data");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin forcesync" + ChatColor.WHITE + " - Force data synchronization");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin bstats" + ChatColor.WHITE + " - Check bStats integration");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("veinminer.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = Arrays.asList("debug", "reload", "sync", "forcesync", "bstats");
            String input = args[0].toLowerCase();
            return completions.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}