package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.ChatColor;

import java.util.Objects;

public class GUIListener implements Listener {

    private final Veinminer plugin;

    public GUIListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();

        // Check if it's the main menu
        if (title.equals(ChatColor.GREEN + "VeinMiner Menu")) {
            // Cancel the event to prevent taking items
            event.setCancelled(true);

            // Only process clicks in the top inventory (the GUI)
            if (event.getClickedInventory() == inventory) {
                plugin.getMainGUI().handleInventoryClick(player, event.getRawSlot(), inventory);
            }
        }
        // Check if it's the achievements GUI
        else if (Objects.equals(title, plugin.getMessageManager().getMessage("messages.achievements.gui.title"))) {
            // Cancel the event to prevent taking items
            event.setCancelled(true);

            // Only process clicks in the top inventory (the GUI)
            if (event.getClickedInventory() == inventory) {
                plugin.getAchievementGUI().handleInventoryClick(player, event.getRawSlot(), inventory);
            }
        }
    }
}