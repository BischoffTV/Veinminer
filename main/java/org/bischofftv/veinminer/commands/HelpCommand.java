package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HelpCommand implements CommandExecutor {

    private final Veinminer plugin;

    public HelpCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Send each help message individually to ensure proper message retrieval
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.help.header"));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.help.toggle"));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.help.tool"));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.help.level"));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.help.achievements")); // Add achievements help
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.help.reload"));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.help.about"));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.help.usage"));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.help.gui"));

        return true;
    }
}