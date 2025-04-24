package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class PlayerInteractListener implements Listener {

    private final Veinminer plugin;

    public PlayerInteractListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if the player has the base permission to use VeinMiner
        if (!plugin.hasPermission(player, "veinminer.use")) {
            return;
        }

        // Get player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null || !playerData.isVeinMinerEnabled()) {
            return;
        }

        // Check if player is sneaking (shift)
        if (!player.isSneaking()) {
            return;
        }

        // Get the tool in the player's hand
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }

        // Determine tool type
        String toolType = getToolType(tool.getType());
        if (toolType == null) {
            return;
        }

        // Check if the player has permission for this specific tool
        if (!hasToolPermission(player, toolType)) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.permission.tool-not-allowed", "%tool%", toolType));
            return;
        }

        // Check if the tool is enabled for the player
        if (!playerData.isToolEnabled(toolType)) {
            return;
        }
    }

    private boolean hasToolPermission(Player player, String toolType) {
        if (!plugin.getConfig().getBoolean("permissions.require-tool-permission", false)) {
            return true;
        }
        return plugin.hasPermission(player, "veinminer.tool." + toolType);
    }

    private String getToolType(Material material) {
        String name = material.toString().toLowerCase();
        if (name.contains("pickaxe")) {
            return "pickaxe";
        } else if (name.contains("axe") && !name.contains("pickaxe")) {
            return "axe";
        } else if (name.contains("shovel") || name.contains("spade")) {
            return "shovel";
        } else if (name.contains("hoe")) {
            return "hoe";
        }
        return null;
    }
} 