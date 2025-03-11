package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.database.DatabaseManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class LevelCommand implements CommandExecutor {

    private final Veinminer plugin;

    public LevelCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getLevelManager().isEnabled()) {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.level.system-disabled"));
            return true;
        }

        DatabaseManager.PlayerData playerData = plugin.getLevelManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.level.data-not-loaded"));
            return true;
        }

        int currentLevel = playerData.getLevel();
        int currentXp = playerData.getXp();
        int maxBlocks = plugin.getLevelManager().getMaxBlocks(player);

        // Get XP needed for next level
        int maxLevel = plugin.getLevelManager().getMaxLevel();
        int nextLevelXp = plugin.getLevelManager().getXpForNextLevel(currentLevel);
        int xpNeeded = nextLevelXp - currentXp;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("level", String.valueOf(currentLevel));
        placeholders.put("max_blocks", String.valueOf(maxBlocks));
        placeholders.put("current_xp", String.valueOf(currentXp));

        // Send header
        player.sendMessage(plugin.getMessageManager().getMessage("messages.level.info.header"));

        // Send level info
        player.sendMessage(plugin.getMessageManager().getMessage("messages.level.info.level", placeholders));
        player.sendMessage(plugin.getMessageManager().getMessage("messages.level.info.max-blocks", placeholders));
        player.sendMessage(plugin.getMessageManager().getMessage("messages.level.info.current-xp", placeholders));

        // Send next level info if not max level
        if (currentLevel < maxLevel) {
            placeholders.put("next_level", String.valueOf(currentLevel + 1));
            placeholders.put("xp_needed", String.valueOf(xpNeeded));
            player.sendMessage(plugin.getMessageManager().getMessage("messages.level.info.next-level", placeholders));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.level.info.max-level"));
        }

        return true;
    }
}

