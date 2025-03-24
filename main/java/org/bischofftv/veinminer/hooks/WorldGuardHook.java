package org.bischofftv.veinminer.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardHook {

    private final Veinminer plugin;
    private boolean worldGuardEnabled = false;
    private WorldGuardPlugin worldGuardPlugin = null;

    public WorldGuardHook(Veinminer plugin) {
        this.plugin = plugin;
        setupWorldGuard();
    }

    private void setupWorldGuard() {
        Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");

        if (wgPlugin != null && wgPlugin instanceof WorldGuardPlugin && wgPlugin.isEnabled()) {
            worldGuardPlugin = (WorldGuardPlugin) wgPlugin;
            worldGuardEnabled = true;
            plugin.getLogger().info("WorldGuard integration enabled!");
        } else {
            plugin.getLogger().info("WorldGuard not found or not enabled. Region protection will not be available.");
            worldGuardEnabled = false;
        }
    }

    /**
     * Checks if a player can break a block according to WorldGuard
     *
     * @param player The player trying to break the block
     * @param block The block to check
     * @return true if the player can break the block, false otherwise
     */
    public boolean canBreak(Player player, Block block) {
        // If WorldGuard is not enabled, allow breaking
        if (!worldGuardEnabled || worldGuardPlugin == null) {
            return true;
        }

        try {
            // Get the WorldGuard player
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

            // Get the WorldGuard region container
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();

            // Check if the player can break blocks at this location
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(block.getLocation());

            // Check the BUILD flag (which controls block breaking)
            return query.testBuild(loc, localPlayer, Flags.BLOCK_BREAK);

        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard protection: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            // If there's an error, deny breaking to be safe
            return false;
        }
    }

    /**
     * Checks if WorldGuard integration is enabled
     *
     * @return true if WorldGuard integration is enabled, false otherwise
     */
    public boolean isEnabled() {
        return worldGuardEnabled;
    }
}