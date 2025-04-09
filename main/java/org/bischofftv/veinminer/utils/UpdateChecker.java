package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for checking for plugin updates
 */
public class UpdateChecker {

    private final Veinminer plugin;
    private final int resourceId;
    private String latestVersion;
    private boolean updateAvailable;

    /**
     * Constructor
     * @param plugin The plugin instance
     * @param resourceId The Spigot resource ID
     */
    public UpdateChecker(Veinminer plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
        this.latestVersion = plugin.getDescription().getVersion();
        this.updateAvailable = false;
    }

    /**
     * Check for updates
     */
    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String currentVersion = plugin.getDescription().getVersion();
                String spigotVersion = getSpigotVersion();

                if (spigotVersion != null && !spigotVersion.isEmpty()) {
                    latestVersion = spigotVersion;
                    updateAvailable = !currentVersion.equalsIgnoreCase(spigotVersion);

                    if (updateAvailable) {
                        plugin.getLogger().info("A new update is available! Current version: " + currentVersion + ", Latest version: " + spigotVersion);
                        plugin.getLogger().info("Download the latest version from: https://www.spigotmc.org/resources/" + resourceId);
                    } else {
                        plugin.getLogger().info("You are running the latest version of VeinMiner!");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    /**
     * Get the latest version from Spigot
     * @return The latest version
     * @throws IOException If an I/O error occurs
     */
    private String getSpigotVersion() throws IOException {
        URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setInstanceFollowRedirects(true);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String version = reader.readLine();

            // Validate version format (e.g., 1.0.0)
            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)+");
            Matcher matcher = pattern.matcher(version);

            if (matcher.matches()) {
                return version;
            } else {
                plugin.getLogger().warning("Received invalid version format from Spigot API: " + version);
                return null;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to connect to Spigot API: " + e.getMessage());
            throw e;
        } finally {
            connection.disconnect();
        }
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
}