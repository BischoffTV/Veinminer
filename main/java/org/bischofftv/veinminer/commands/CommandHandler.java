package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler implements CommandExecutor {

    private final Veinminer plugin;
    private final ToggleCommand toggleCommand;
    private final LevelCommand levelCommand;
    private final LevelSetCommand levelSetCommand;
    private final AchievementsCommand achievementsCommand;

    public CommandHandler(Veinminer plugin) {
        this.plugin = plugin;
        this.toggleCommand = new ToggleCommand(plugin);
        this.levelCommand = new LevelCommand(plugin);
        this.levelSetCommand = new LevelSetCommand(plugin);
        this.achievementsCommand = new AchievementsCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If no arguments and sender is a player, open the main GUI
        if (args.length == 0 && sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getMainGUI().openMainMenu(player);
            return true;
        }

        // Handle subcommands
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();

            // Admin-Befehle
            if (sender.hasPermission("veinminer.admin") &&
                    (subCommand.equals("check") || subCommand.equals("mysql") ||
                            subCommand.equals("debug") || subCommand.equals("config") ||
                            subCommand.equals("sync") || subCommand.equals("reload") ||
                            subCommand.equals("help"))) {
                return plugin.getAdminCommand().onCommand(sender, command, label, args);
            }

            switch (subCommand) {
                case "toggle":
                case "on":
                case "off":
                case "tool":
                    return toggleCommand.onCommand(sender, command, label, args);

                case "level":
                    return levelCommand.onCommand(sender, command, label, args);

                case "setlevel":
                    // Remove the first argument and pass the rest to the level set command
                    String[] newArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                    return levelSetCommand.onCommand(sender, command, label, newArgs);

                case "achievements":
                    return achievementsCommand.onCommand(sender, command, label, args);

                default:
                    sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.unknown-subcommand", "Unknown subcommand. Use /veinminer help for a list of commands."));
                    return true;
            }
        }

        return true;
    }
}