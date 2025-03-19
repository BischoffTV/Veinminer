package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AchievementsCommand implements CommandExecutor {

    private final Veinminer plugin;

    public AchievementsCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getAchievementManager().isEnabled()) {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.system-disabled"));
            return true;
        }

        try {
            // Make sure achievements are loaded before opening GUI
            if (plugin.getAchievementManager().getPlayerAchievements(player).isEmpty()) {
                plugin.getAchievementManager().loadPlayerAchievements(player);

                // Add a small delay to ensure achievements are loaded
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    openAchievementsGUI(player);
                }, 10L); // Half second delay
            } else {
                openAchievementsGUI(player);
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening achievements GUI for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.gui.error"));
            return false;
        }
    }

    private void openAchievementsGUI(Player player) {
        try {
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                plugin.getLogger().info("[Debug] Opening achievements GUI for " + player.getName());
            }
            plugin.getAchievementGUI().openAchievementsMenu(player);
        } catch (Exception e) {
            plugin.getLogger().severe("Error in openAchievementsGUI for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.gui.error"));
        }
    }
}