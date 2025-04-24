package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final Veinminer plugin;

    public PlayerJoinListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Send update notification if available
        plugin.getUpdateChecker().sendUpdateNotification(event.getPlayer());
    }
} 