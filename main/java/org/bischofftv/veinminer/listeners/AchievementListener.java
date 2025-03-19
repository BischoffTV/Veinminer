package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.inventory.Inventory;

import java.util.Objects;

public class AchievementListener implements Listener {

    private final Veinminer plugin;

    public AchievementListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        // Check if it's the achievements GUI
        if (Objects.equals(event.getView().getTitle(),
                plugin.getMessageManager().getMessage("messages.achievements.gui.title"))) {
            event.setCancelled(true);

            if (event.getRawSlot() >= 0 && event.getRawSlot() < inventory.getSize()) {
                plugin.getAchievementGUI().handleInventoryClick(player, event.getRawSlot(), inventory);
            }
        }
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();

        // Update level-based achievements
        plugin.getAchievementManager().updateLevelProgress(player, newLevel);
    }
}