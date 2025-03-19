package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class AboutCommand implements CommandExecutor {

    private final Veinminer plugin;

    public AboutCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("version", plugin.getDescription().getVersion());
        placeholders.put("description", plugin.getDescription().getDescription());

        sender.sendMessage(plugin.getMessageManager().getMessage("messages.about.header"));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.about.version", placeholders));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.about.author"));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.about.description", placeholders));
        sender.sendMessage(plugin.getMessageManager().getMessage("messages.about.usage"));

        return true;
    }
}