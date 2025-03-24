package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

public class HelpCommand implements CommandExecutor {

    private final Veinminer plugin;

    public HelpCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "=== VeinMiner Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/vmtoggle " + ChatColor.WHITE + "- Toggle VeinMiner on/off");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer tool <type> " + ChatColor.WHITE + "- Toggle a specific tool type");
        sender.sendMessage(ChatColor.YELLOW + "/vmlevel " + ChatColor.WHITE + "- Check your VeinMiner level");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer achievements " + ChatColor.WHITE + "- View your achievements");
        sender.sendMessage(ChatColor.YELLOW + "/veinminerreload " + ChatColor.WHITE + "- Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/veinminerabout " + ChatColor.WHITE + "- View information about the plugin");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer " + ChatColor.WHITE + "- Open the main menu");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer gui " + ChatColor.WHITE + "- Open the main menu");

        return true;
    }
}