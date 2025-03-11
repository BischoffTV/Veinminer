package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandHandler implements CommandExecutor {

    private final Veinminer plugin;
    private final ToggleCommand toggleCommand;
    private final LevelCommand levelCommand;

    public CommandHandler(Veinminer plugin) {
        this.plugin = plugin;
        this.toggleCommand = new ToggleCommand(plugin);
        this.levelCommand = new LevelCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("toggle") ||
                args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off") ||
                args[0].equalsIgnoreCase("tool")) {
            return toggleCommand.onCommand(sender, command, label, args);
        } else if (args[0].equalsIgnoreCase("level")) {
            return levelCommand.onCommand(sender, command, label, args);
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.unknown-subcommand"));
        return true;
    }
}

