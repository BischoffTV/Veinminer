package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

    private final Veinminer plugin;

    public CommandHandler(Veinminer plugin) {
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

        // Check if the player has permission to use the command
        if (!plugin.hasPermission(player, "veinminer.command.use")) {
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
                plugin.getMainGUI().openMainGUI(player);
                return true;

            case "toggle":
                if (!plugin.hasPermission(player, "veinminer.command.toggle")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }
                toggleVeinMiner(player);
                return true;

            case "tool":
                if (!plugin.hasPermission(player, "veinminer.command.tool")) {
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
                if (!plugin.hasPermission(player, "veinminer.tool." + toolType)) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.permission.tool-not-allowed", "%tool%", toolType));
                    return true;
                }

                toggleTool(player, toolType);
                return true;

            case "achievements":
                if (!plugin.hasPermission(player, "veinminer.command.achievements")) {
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
                if (!plugin.hasPermission(player, "veinminer.command.help")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }

                sendHelpMessage(player);
                return true;

            case "reload":
                if (!plugin.hasPermission(player, "veinminer.admin.reload")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }

                plugin.reloadConfig();
                plugin.getMessageManager().reload();
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.reload.success"));
                return true;

            case "about":
                if (!plugin.hasPermission(player, "veinminer.command.about")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }

                sendAboutMessage(player);
                return true;

            case "setlevel":
                if (!plugin.hasPermission(player, "veinminer.admin.setlevel")) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
                    return true;
                }

                if (!plugin.getLevelManager().isEnabled()) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.level.disabled"));
                    return true;
                }

                if (args.length < 3) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-args"));
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.usage", "%command%", "/veinminer setlevel <player> <level>"));
                    return true;
                }

                handleSetLevel(player, args[1], args[2]);
                return true;

            default:
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-args"));
                return true;
        }
    }

    /**
     * Toggle VeinMiner for a player
     * @param player The player
     */
    private void toggleVeinMiner(Player player) {
        boolean enabled = plugin.getPlayerDataManager().isVeinMinerEnabled(player);
        plugin.getPlayerDataManager().setVeinMinerEnabled(player, !enabled);

        // Speichere die Einstellungen sofort
        plugin.getPlayerDataManager().savePlayerSettings(player);

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

        // Speichere die Einstellungen sofort
        plugin.getPlayerDataManager().savePlayerSettings(player);

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
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.about.author"));
        player.sendMessage(plugin.getMessageManager().formatMessage("messages.about.website"));
    }

    /**
     * Handle the setlevel subcommand
     * @param sender The command sender
     * @param playerName The target player name
     * @param levelStr The level string
     */
    private void handleSetLevel(CommandSender sender, String playerName, String levelStr) {
        // Get target player
        org.bukkit.entity.Player targetPlayer = org.bukkit.Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.player-not-found", "%player%", playerName));
            return;
        }

        // Parse level
        int level;
        try {
            level = Integer.parseInt(levelStr);
            if (level < 1) {
                sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-level"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.invalid-level"));
            return;
        }

        // Set level
        org.bischofftv.veinminer.data.PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(targetPlayer.getUniqueId());
        if (playerData != null) {
            playerData.setLevel(level);
            // XP für das gesetzte Level aus der Config setzen
            int xpForLevel = 0;
            try {
                // Hole LevelManager und die XP-Map
                Object levelManager = plugin.getLevelManager();
                java.lang.reflect.Field xpPerLevelField = levelManager.getClass().getDeclaredField("xpPerLevel");
                xpPerLevelField.setAccessible(true);
                java.util.Map xpPerLevel = (java.util.Map) xpPerLevelField.get(levelManager);
                if (xpPerLevel.containsKey(level)) {
                    xpForLevel = (int) xpPerLevel.get(level);
                }
            } catch (Exception e) {
                // Fallback: XP bleibt 0
            }
            playerData.setExperience(xpForLevel);
            // Kein sofortiges Speichern mehr!
            // plugin.getPlayerDataManager().savePlayerData(targetPlayer.getUniqueId());

            // Send messages
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.level.set",
                    "%player%", targetPlayer.getName(),
                    "%level%", String.valueOf(level)));

            if (sender != targetPlayer) {
                targetPlayer.sendMessage(plugin.getMessageManager().formatMessage("messages.level.set-target",
                        "%level%", String.valueOf(level)));
            }

            // Check for level-based achievements
            if (plugin.getAchievementManager().isEnabled()) {
                plugin.getAchievementManager().updateLevelAchievements(targetPlayer, level);
            }
        } else {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.error.player-data-not-found"));
        }
    }
}