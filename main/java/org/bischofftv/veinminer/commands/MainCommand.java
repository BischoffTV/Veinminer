package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
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
    private final List<String> subCommands = Arrays.asList("gui", "toggle", "tool", "achievements", "help", "about", "reload");

    public MainCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Check if the player has permission
        if (!hasPermission(player, "veinminer.command.use")) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
            return true;
        }

        // If no arguments, open the main GUI
        if (args.length == 0) {
            plugin.getMainGUI().openMainGUI(player);
            return true;
        }

        // Handle subcommands
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gui":
                if (!hasPermission(player, "veinminer.command.gui")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }
                plugin.getMainGUI().openMainGUI(player);
                return true;

            case "toggle":
                if (!hasPermission(player, "veinminer.command.toggle")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }
                toggleVeinMiner(player);
                return true;

            case "tool":
                if (!hasPermission(player, "veinminer.command.tool")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-args"));
                    return true;
                }

                String toolType = args[1].toLowerCase();
                if (!isValidTool(toolType)) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-tool"));
                    return true;
                }

                // Check if the player has permission for this specific tool
                if (!hasPermission(player, "veinminer.tool." + toolType)) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.permission.tool-not-allowed", "%tool%", toolType));
                    return true;
                }

                toggleTool(player, toolType);
                return true;

            case "achievements":
                if (!hasPermission(player, "veinminer.command.achievements")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }

                if (!plugin.getAchievementManager().isEnabled()) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.achievements.disabled"));
                    return true;
                }

                plugin.getAchievementGUI().openAchievementGUI(player);
                return true;

            case "help":
                if (!hasPermission(player, "veinminer.command.help")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }

                sendHelpMessage(player);
                return true;

            case "reload":
                if (!hasPermission(player, "veinminer.admin.reload")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }

                plugin.reloadConfig();
                plugin.getMessageManager().reload();
                plugin.getConfigManager().loadConfig();
                plugin.getLevelManager().loadConfig();
                plugin.getSkillManager().loadConfig();
                plugin.getAchievementManager().loadConfig();
                plugin.restartAutoSaveTask();
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.reload.success"));
                return true;

            case "about":
                if (!hasPermission(player, "veinminer.command.about")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }

                sendAboutMessage(player);
                return true;

            default:
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-args"));
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tool")) {
            // Suggest tool types
            List<String> tools = Arrays.asList("pickaxe", "axe", "shovel", "hoe");
            for (String tool : tools) {
                if (tool.startsWith(args[1].toLowerCase())) {
                    completions.add(tool);
                }
            }
        }

        return completions;
    }

    /**
     * Check if a player has a permission
     * @param player The player to check
     * @param permission The permission to check
     * @return True if the player has permission, false otherwise
     */
    private boolean hasPermission(Player player, String permission) {
        if (player.hasPermission("veinminer.admin")) {
            return true;
        }

        return player.hasPermission(permission);
    }

    /**
     * Toggle VeinMiner for a player
     * @param player The player
     */
    private void toggleVeinMiner(Player player) {
        boolean enabled = plugin.getPlayerDataManager().isVeinMinerEnabled(player);
        plugin.getPlayerDataManager().setVeinMinerEnabled(player, !enabled);

        if (enabled) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.disabled"));
        } else {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.enabled"));
        }
    }

    /**
     * Toggle a tool for a player
     * @param player The player
     * @param toolType The tool type
     */
    private void toggleTool(Player player, String toolType) {
        boolean enabled = plugin.getPlayerDataManager().isToolEnabled(player, toolType);
        plugin.getPlayerDataManager().setToolEnabled(player, toolType, !enabled);

        if (enabled) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.tool-disabled", "%tool%", toolType));
        } else {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.tool-enabled", "%tool%", toolType));
        }
    }

    /**
     * Check if a tool type is valid
     * @param toolType The tool type
     * @return True if valid, false otherwise
     */
    private boolean isValidTool(String toolType) {
        return toolType.equals("pickaxe") || toolType.equals("axe") ||
                toolType.equals("shovel") || toolType.equals("hoe");
    }

    /**
     * Send the help message to a player
     * @param player The player
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.help.header"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.help.command", "%command%", "veinminer", "%description%", "Open the main menu"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.help.command", "%command%", "veinminer gui", "%description%", "Open the main menu"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.help.command", "%command%", "veinminer toggle", "%description%", "Toggle VeinMiner on/off"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.help.command", "%command%", "veinminer tool <type>", "%description%", "Toggle a specific tool type"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.help.command", "%command%", "veinminer achievements", "%description%", "View your achievements"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.help.command", "%command%", "veinminer help", "%description%", "View this help message"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.help.command", "%command%", "veinminer about", "%description%", "View information about the plugin"));

        if (player.hasPermission("veinminer.admin.reload")) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.help.command", "%command%", "veinminer reload", "%description%", "Reload the plugin configuration"));
        }
    }

    /**
     * Send the about message to a player
     * @param player The player
     */
    private void sendAboutMessage(Player player) {
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.about.header"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.about.version", "%version%", plugin.getDescription().getVersion()));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.about.author", "%author%", "BischoffTV"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.about.website", "%website%", "https://github.com/BischoffTV/Veinminer"));
    }
}