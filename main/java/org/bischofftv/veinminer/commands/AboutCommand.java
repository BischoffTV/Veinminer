package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AboutCommand implements CommandExecutor {

    private final Veinminer plugin;

    public AboutCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(plugin.getMessageManager().formatMessage("messages.about.header"));
        sender.sendMessage(plugin.getMessageManager().formatMessage("messages.about.version", "%version%", plugin.getDescription().getVersion()));
        sender.sendMessage(plugin.getMessageManager().formatMessage("messages.about.author"));
        sender.sendMessage(plugin.getMessageManager().formatMessage("messages.about.website"));
        return true;
    }
}