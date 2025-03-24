package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillCommand implements CommandExecutor {

    private final Veinminer plugin;

    public SkillCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("messages.command.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getSkillManager().isEnabled()) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.disabled"));
            return true;
        }

        // Open the skill GUI
        plugin.getSkillGUI().openSkillGUI(player);
        return true;
    }
}