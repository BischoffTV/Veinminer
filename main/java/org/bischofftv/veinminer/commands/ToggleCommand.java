package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCommand implements CommandExecutor {

    private final Veinminer plugin;

    public ToggleCommand(Veinminer plugin) {
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

        // Check if the player has permission to use the toggle command
        if (!hasPermission(player, "veinminer.command.toggle")) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.command.no-permission"));
            return true;
        }

        // Toggle VeinMiner for the player
        boolean enabled = plugin.getPlayerDataManager().isVeinMinerEnabled(player);
        plugin.getPlayerDataManager().setVeinMinerEnabled(player, !enabled);

        // Speichere die Einstellungen sofort
        plugin.getPlayerDataManager().savePlayerSettings(player);

        // Send a message to the player indicating the new state
        if (!enabled) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.enabled"));
        } else {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.disabled"));
        }

        return true;
    }

    /**
     * Check if a player has a permission
     * @param player The player to check
     * @param permission The permission to check
     * @return True if the player has permission, false otherwise
     */
    private boolean hasPermission(Player player, String permission) {
        if (plugin.hasPermission(player, "veinminer.admin")) {
            return true;
        }
        return plugin.hasPermission(player, permission);
    }
}