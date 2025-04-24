package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

    private final Veinminer plugin;

    public InventoryClickListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Handle GUI clicks
        String title = event.getView().getTitle();
        
        if (title.equals(plugin.getMessageManager().getMessage("gui.main-title", "VeinMiner Menu"))) {
            // Main GUI handling is done in GUIListener
            event.setCancelled(true);
        } else if (title.equals(plugin.getMessageManager().getMessage("gui.achievements-title", "VeinMiner Achievements"))) {
            // Achievements GUI handling
            event.setCancelled(true);
            plugin.getAchievementGUI().handleInventoryClick(event);
        } else if (title.equals(plugin.getMessageManager().getMessage("gui.skills-title", "VeinMiner Skills"))) {
            // Skills GUI handling
            event.setCancelled(true);
            // Skills GUI handling is done in GUIListener
        }
    }
} 