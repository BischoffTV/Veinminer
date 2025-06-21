package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final Veinminer plugin;
    private final List<String> subCommands = Arrays.asList(
            "reload", "debug", "sync", "reset", "check", "mysql", "reload-player", "repair-database"
    );

    public AdminCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender has permission
        if (!sender.hasPermission("veinminer.admin")) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
            return true;
        }

        // Check if there are any arguments
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // Handle subcommands
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                // Reload the plugin configuration
                plugin.reloadConfig();
                plugin.getMessageManager().reload();
                plugin.getConfigManager().loadConfig();
                plugin.getLevelManager().loadConfig();
                plugin.getSkillManager().loadConfig();
                plugin.getAchievementManager().loadConfig();
                plugin.restartAutoSaveTask();
                sender.sendMessage(ChatColor.GREEN + "VeinMiner configuration has been reloaded.");
                return true;

            case "debug":
                // Toggle debug mode
                boolean newDebugMode = !plugin.isDebugMode();
                plugin.setDebugMode(newDebugMode);
                sender.sendMessage(ChatColor.GREEN + "Debug mode has been " + (newDebugMode ? "enabled" : "disabled") + ".");
                return true;

            case "sync":
                // Force synchronization
                if (plugin.getDatabaseManager().isFallbackMode()) {
                    sender.sendMessage(ChatColor.RED + "Synchronization is not available in fallback mode.");
                    return true;
                }

                plugin.forceSyncDataNow();
                sender.sendMessage(ChatColor.GREEN + "Data synchronization has been forced.");
                return true;

            case "reset":
                // Reset player data
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /vmadmin reset <player>");
                    return true;
                }

                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }

                // Reset player data
                plugin.getPlayerDataManager().removePlayerData(targetPlayer.getUniqueId());
                plugin.getPlayerDataManager().loadPlayerData(targetPlayer);
                sender.sendMessage(ChatColor.GREEN + "Player data has been reset for " + targetPlayer.getName() + ".");
                return true;

            case "check":
                // Check player data
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /vmadmin check <player>");
                    return true;
                }

                Player checkPlayer = Bukkit.getPlayer(args[1]);
                if (checkPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }

                // Get player data
                org.bischofftv.veinminer.data.PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(checkPlayer.getUniqueId());
                if (playerData == null) {
                    sender.sendMessage(ChatColor.RED + "No player data found for " + checkPlayer.getName() + ".");
                    return true;
                }

                // Display player data
                sender.sendMessage(ChatColor.GREEN + "Player data for " + checkPlayer.getName() + ":");
                sender.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + playerData.getLevel());
                sender.sendMessage(ChatColor.YELLOW + "Experience: " + ChatColor.WHITE + playerData.getExperience());
                sender.sendMessage(ChatColor.YELLOW + "Blocks Mined: " + ChatColor.WHITE + playerData.getBlocksMined());
                sender.sendMessage(ChatColor.YELLOW + "VeinMiner Enabled: " + ChatColor.WHITE + playerData.isVeinMinerEnabled());
                sender.sendMessage(ChatColor.YELLOW + "Skill Points: " + ChatColor.WHITE + playerData.getSkillPoints());
                sender.sendMessage(ChatColor.YELLOW + "Efficiency Level: " + ChatColor.WHITE + playerData.getEfficiencyLevel());
                sender.sendMessage(ChatColor.YELLOW + "Luck Level: " + ChatColor.WHITE + playerData.getLuckLevel());
                sender.sendMessage(ChatColor.YELLOW + "Energy Level: " + ChatColor.WHITE + playerData.getEnergyLevel());
                return true;

            case "mysql":
                // MySQL commands
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /vmadmin mysql <reload|check>");
                    return true;
                }

                String mysqlCommand = args[1].toLowerCase();

                if (mysqlCommand.equals("reload")) {
                    // Reload MySQL connection
                    plugin.getDatabaseManager().close();
                    plugin.getDatabaseManager().initialize();
                    plugin.getDatabaseManager().createTables();
                    sender.sendMessage(ChatColor.GREEN + "MySQL connection has been reloaded.");
                    return true;
                } else if (mysqlCommand.equals("check")) {
                    // Check MySQL connection
                    boolean connected = plugin.getDatabaseManager().checkConnection();
                    sender.sendMessage(ChatColor.GREEN + "MySQL connection: " + (connected ? "Connected" : "Disconnected"));

                    // Check player data in database
                    if (args.length >= 3) {
                        Player dbPlayer = Bukkit.getPlayer(args[2]);
                        if (dbPlayer == null) {
                            sender.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
                            return true;
                        }

                        String dbData = plugin.getDatabaseManager().dumpPlayerData(dbPlayer.getUniqueId().toString());
                        sender.sendMessage(ChatColor.GREEN + "Database data:");
                        for (String line : dbData.split("\n")) {
                            sender.sendMessage(ChatColor.YELLOW + line);
                        }
                    }

                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown MySQL command: " + mysqlCommand);
                    return true;
                }

            case "reload-player":
                // Force reload player data
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /vmadmin reload-player <player>");
                    return true;
                }

                Player reloadPlayer = Bukkit.getPlayer(args[1]);
                if (reloadPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }

                // Force reload player data
                plugin.getPlayerDataManager().forceReloadPlayerData(reloadPlayer);
                sender.sendMessage(ChatColor.GREEN + "Player data has been force reloaded for " + reloadPlayer.getName() + ".");
                return true;

            case "repair-database":
                // Repair database tables
                boolean success = repairDatabase();
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Database tables have been repaired successfully.");

                    // Reload online players' data
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        plugin.getPlayerDataManager().forceReloadPlayerData(player);
                    }

                    sender.sendMessage(ChatColor.GREEN + "Player data has been reloaded for all online players.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to repair database tables. Check console for details.");
                }
                return true;

            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest subcommands
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            // Suggest players for certain subcommands
            if (args[0].equalsIgnoreCase("reset") ||
                    args[0].equalsIgnoreCase("check") ||
                    args[0].equalsIgnoreCase("reload-player")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("mysql")) {
                // Suggest MySQL subcommands
                List<String> mysqlCommands = Arrays.asList("reload", "check");
                for (String mysqlCommand : mysqlCommands) {
                    if (mysqlCommand.startsWith(args[1].toLowerCase())) {
                        completions.add(mysqlCommand);
                    }
                }
            }
        } else if (args.length == 3) {
            // Suggest players for MySQL check command
            if (args[0].equalsIgnoreCase("mysql") && args[1].equalsIgnoreCase("check")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }

    /**
     * Send the help message to a sender
     * @param sender The sender
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== VeinMiner Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin reload" + ChatColor.WHITE + " - Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin debug" + ChatColor.WHITE + " - Toggle debug mode");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin sync" + ChatColor.WHITE + " - Force data synchronization");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin reset <player>" + ChatColor.WHITE + " - Reset player data");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin check <player>" + ChatColor.WHITE + " - Check player data");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin mysql reload" + ChatColor.WHITE + " - Reload MySQL connection");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin mysql check [player]" + ChatColor.WHITE + " - Check MySQL connection and player data");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin reload-player <player>" + ChatColor.WHITE + " - Force reload player data from database");
        sender.sendMessage(ChatColor.YELLOW + "/vmadmin repair-database" + ChatColor.WHITE + " - Repair database tables");
    }

    /**
     * Repair database tables
     * @return True if successful, false otherwise
     */
    private boolean repairDatabase() {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            connection = plugin.getDatabaseManager().getConnection();
            if (connection == null) {
                plugin.getLogger().severe("Failed to repair database: No database connection");
                return false;
            }

            // Check if player_data table exists
            DatabaseMetaData metaData = connection.getMetaData();
            String tablePrefix = plugin.getDatabaseManager().getTablePrefix();
            String playerDataTable = tablePrefix + "player_data";

            resultSet = metaData.getTables(null, null, playerDataTable, null);
            boolean tableExists = resultSet.next();
            resultSet.close();

            if (tableExists) {
                // Drop the table
                statement = connection.createStatement();
                statement.executeUpdate("DROP TABLE IF EXISTS " + playerDataTable);
                statement.close();
                plugin.getLogger().info("Dropped existing player_data table");
            }

            // Check if achievements table exists
            String achievementsTable = tablePrefix + "achievements";
            resultSet = metaData.getTables(null, null, achievementsTable, null);
            tableExists = resultSet.next();
            resultSet.close();

            if (tableExists) {
                // Drop the table
                statement = connection.createStatement();
                statement.executeUpdate("DROP TABLE IF EXISTS " + achievementsTable);
                statement.close();
                plugin.getLogger().info("Dropped existing achievements table");
            }

            // Check if sync_status table exists
            String syncStatusTable = tablePrefix + "sync_status";
            resultSet = metaData.getTables(null, null, syncStatusTable, null);
            tableExists = resultSet.next();
            resultSet.close();

            if (tableExists) {
                // Drop the table
                statement = connection.createStatement();
                statement.executeUpdate("DROP TABLE IF EXISTS " + syncStatusTable);
                statement.close();
                plugin.getLogger().info("Dropped existing sync_status table");
            }

            // Recreate tables
            plugin.getDatabaseManager().createTables();
            plugin.getLogger().info("Database repair completed successfully");
            return true;

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to repair database: " + e.getMessage());
            return false;
        } finally {
            plugin.getDatabaseManager().closeResources(resultSet, statement, connection);
        }
    }
}