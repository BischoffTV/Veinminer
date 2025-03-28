package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TopPlayersGUIListener implements Listener {

    private final Veinminer plugin;

    public TopPlayersGUIListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // Check if it's one of our GUIs
        if (title.equals(ChatColor.GOLD + "VeinMiner Top Players") ||
                title.equals(ChatColor.GOLD + "Top Players by Level") ||
                title.equals(ChatColor.GOLD + "Top Players by Achievements") ||
                title.equals(ChatColor.GOLD + "Top Players by Blocks Mined")) {

            event.setCancelled(true);

            if (event.getCurrentItem() == null) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            // Handle the click
            plugin.getTopPlayersGUI().handleInventoryClick(player, slot, title);
        }
    }
}