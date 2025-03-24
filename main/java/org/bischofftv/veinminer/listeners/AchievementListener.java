package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class AchievementListener implements Listener {

    private final Veinminer plugin;

    public AchievementListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GOLD + "VeinMiner Achievements")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();

            // Check if clicked on back button
            if (event.getSlot() == 49 && event.getCurrentItem().getType() == Material.OAK_DOOR) {
                player.closeInventory();
                plugin.getMainGUI().openMainGUI(player);
                return;
            }

            // Check if clicked on an achievement
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

                // Find the achievement by name
                for (Map.Entry<String, Map<String, Object>> entry : plugin.getAchievementManager().getAchievementDefinitions().entrySet()) {
                    String achievementId = entry.getKey();
                    Map<String, Object> achievementData = entry.getValue();
                    String achievementName = (String) achievementData.get("name");

                    if (displayName.equals(achievementName)) {
                        // Try to claim rewards
                        plugin.getAchievementManager().claimAchievementRewards(player, achievementId);

                        // Refresh the GUI
                        plugin.getAchievementGUI().openAchievementGUI(player);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getAchievementManager().isEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Load player achievements
        plugin.getAchievementManager().loadPlayerAchievements(player);
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        if (!plugin.getAchievementManager().isEnabled() || !plugin.getLevelManager().isEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Get player data
        int level = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getLevel();

        // Update level achievements
        plugin.getAchievementManager().updateLevelProgress(player, level);
    }
}