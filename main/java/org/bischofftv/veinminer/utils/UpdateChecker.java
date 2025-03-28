package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker implements Listener {

    private final Veinminer plugin;
    private final int resourceId;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(Veinminer plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;

        // Register the listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Check for updates
        checkForUpdates();
    }

    /**
     * Check for updates from Spigot
     */
    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String currentVersion = plugin.getDescription().getVersion();
                latestVersion = getLatestVersionFromSpigot(); // Korrigierter Methodenaufruf
                //getLatestVersion(); // Entferne diese Zeile

                if (latestVersion != null && !latestVersion.isEmpty()) {
                    // Compare versions
                    if (compareVersions(latestVersion, currentVersion) > 0) {
                        updateAvailable = true;
                        plugin.getLogger().info("A new update is available: " + latestVersion + " (Current: " + currentVersion + ")");
                        plugin.getLogger().info("Download it at: https://www.spigotmc.org/resources/veinminer-ultimate-mining-enhancement.123199/");
                    } else {
                        plugin.getLogger().info("You are running the latest version of VeinMiner: " + currentVersion);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
                if (plugin.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Get the latest version from Spigot
     * @return The latest version
     * @throws IOException If an error occurs
     */
    private String getLatestVersionFromSpigot() throws IOException { // Umbenannte Methode
        URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "VeinMiner/" + plugin.getDescription().getVersion());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return reader.readLine();
        }
    }

    /**
     * Compare two version strings
     * @param version1 The first version
     * @param version2 The second version
     * @return 1 if version1 is newer, -1 if version2 is newer, 0 if they are equal
     */
    private int compareVersions(String version1, String version2) {
        // Extract version numbers using regex
        Pattern pattern = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?");
        Matcher matcher1 = pattern.matcher(version1);
        Matcher matcher2 = pattern.matcher(version2);

        if (!matcher1.find() || !matcher2.find()) {
            return version1.compareTo(version2); // Fallback to string comparison
        }

        // Compare major version
        int major1 = Integer.parseInt(matcher1.group(1));
        int major2 = Integer.parseInt(matcher2.group(1));
        if (major1 != major2) {
            return major1 > major2 ? 1 : -1;
        }

        // Compare minor version
        int minor1 = matcher1.group(2) != null ? Integer.parseInt(matcher1.group(2)) : 0;
        int minor2 = matcher2.group(2) != null ? Integer.parseInt(matcher2.group(2)) : 0;
        if (minor1 != minor2) {
            return minor1 > minor2 ? 1 : -1;
        }

        // Compare patch version
        int patch1 = matcher1.group(3) != null ? Integer.parseInt(matcher1.group(3)) : 0;
        int patch2 = matcher2.group(3) != null ? Integer.parseInt(matcher2.group(3)) : 0;
        if (patch1 != patch2) {
            return patch1 > patch2 ? 1 : -1;
        }

        // Compare build version
        int build1 = matcher1.group(4) != null ? Integer.parseInt(matcher1.group(4)) : 0;
        int build2 = matcher2.group(4) != null ? Integer.parseInt(matcher2.group(4)) : 0;
        if (build1 != build2) {
            return build1 > build2 ? 1 : -1;
        }

        return 0; // Versions are equal
    }

    /**
     * Check if an update is available
     * @return True if an update is available, false otherwise
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * Get the latest version
     * @return The latest version
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Notify a player about an update
     * @param player The player to notify
     */
    public void notifyPlayer(Player player) {
        if (updateAvailable && player.isOp()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "=== VeinMiner Update Available ===");
            player.sendMessage(ChatColor.YELLOW + "Current version: " + ChatColor.RED + plugin.getDescription().getVersion());
            player.sendMessage(ChatColor.YELLOW + "Latest version: " + ChatColor.GREEN + latestVersion);
            player.sendMessage(ChatColor.YELLOW + "Download: " + ChatColor.AQUA + "https://www.spigotmc.org/resources/veinminer-ultimate-mining-enhancement.123199/");
            player.sendMessage("");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            // Delay the notification to ensure it's seen after join messages
            Bukkit.getScheduler().runTaskLater(plugin, () -> notifyPlayer(player), 40L); // 2 seconds delay
        }
    }
}