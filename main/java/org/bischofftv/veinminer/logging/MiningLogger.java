package org.bischofftv.veinminer.logging;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MiningLogger {

    private final Veinminer plugin;
    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public MiningLogger(Veinminer plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "mining_logs.txt");

        if (!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create log file: " + e.getMessage());
            }
        }
    }

    public void logMiningActivity(Player player, int blocksDestroyed, Map<Material, Integer> itemsCollected, Map<Enchantment, Integer> enchantments) {
        // Log to file asynchronously
        CompletableFuture.runAsync(() -> {
            logToFile(player, blocksDestroyed, itemsCollected, enchantments);
        });

        // Log to Discord if enabled
        if (plugin.getConfigManager().isEnableDiscordLogging()) {
            CompletableFuture.runAsync(() -> {
                logToDiscord(player, blocksDestroyed, itemsCollected, enchantments);
            });
        }
    }

    private void logToFile(Player player, int blocksDestroyed, Map<Material, Integer> itemsCollected, Map<Enchantment, Integer> enchantments) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            StringBuilder logEntry = new StringBuilder();

            logEntry.append("[").append(dateFormat.format(new Date())).append("] ");
            logEntry.append("Player: ").append(player.getName()).append(", ");
            logEntry.append("Blocks: ").append(blocksDestroyed).append(", ");

            // Add items collected
            logEntry.append("Items: [");
            boolean firstItem = true;
            for (Map.Entry<Material, Integer> entry : itemsCollected.entrySet()) {
                if (!firstItem) {
                    logEntry.append(", ");
                }
                logEntry.append(entry.getKey().name()).append(": ").append(entry.getValue());
                firstItem = false;
            }
            logEntry.append("], ");

            // Add enchantments - safely handle enchantment keys
            logEntry.append("Enchantments: [");
            boolean firstEnchant = true;
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                if (!firstEnchant) {
                    logEntry.append(", ");
                }

                // Safely get enchantment name
                String enchantName;
                try {
                    enchantName = entry.getKey().getKey().toString().toLowerCase();
                } catch (Exception e) {
                    enchantName = "enchantment_" + entry.getKey().hashCode();
                }

                logEntry.append(enchantName).append(": ").append(entry.getValue());
                firstEnchant = false;
            }
            logEntry.append("]");

            writer.write(logEntry.toString());
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
        }
    }

    private void logToDiscord(Player player, int blocksDestroyed, Map<Material, Integer> itemsCollected, Map<Enchantment, Integer> enchantments) {
        String webhookUrl = plugin.getConfigManager().getDiscordWebhookUrl();
        if (webhookUrl.isEmpty()) {
            return;
        }

        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"embeds\":[{");

            // Title and color (green)
            jsonBuilder.append("\"title\":\"VeinMiner Activity Log\",");
            jsonBuilder.append("\"color\":5763719,");

            // Fields array
            jsonBuilder.append("\"fields\":[");

            // Player field (inline)
            appendField(jsonBuilder, "Player", player.getName(), true, true);

            // Blocks Destroyed field (inline)
            appendField(jsonBuilder, "Blocks Destroyed", String.valueOf(blocksDestroyed), true, true);

            // Location field
            String location = String.format("World: %s X: %d Y: %d Z: %d",
                    player.getWorld().getName(),
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ());
            appendField(jsonBuilder, "Location", location, false, true);

            // Items Collected field
            StringBuilder items = new StringBuilder();
            for (Map.Entry<Material, Integer> entry : itemsCollected.entrySet()) {
                if (items.length() > 0) {
                    items.append(" ");
                }
                items.append(entry.getKey().name()).append(": ").append(entry.getValue());
            }
            appendField(jsonBuilder, "Items Collected", items.toString(), false, true);

            // Tool field (inline)
            String tool = player.getInventory().getItemInMainHand().getType().name();
            appendField(jsonBuilder, "Tool", tool, true, true);

            // Enchantments field (inline)
            StringBuilder enchantmentStr = new StringBuilder();
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                if (enchantmentStr.length() > 0) {
                    enchantmentStr.append(" ");
                }
                enchantmentStr.append(entry.getKey().getKey().getKey())
                        .append(": ")
                        .append(entry.getValue());
            }
            appendField(jsonBuilder, "Enchantments", enchantmentStr.toString(), true, false);

            // Close JSON structure
            jsonBuilder.append("]}]}");

            // Send webhook
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                plugin.getLogger().warning("Failed to send Discord webhook. Response code: " + responseCode);

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String errorResponse = br.readLine();
                    if (errorResponse != null) {
                        plugin.getLogger().warning("Discord error response: " + errorResponse);
                    }
                }
            }

            conn.disconnect();

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send Discord webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void appendField(StringBuilder jsonBuilder, String name, String value, boolean inline, boolean addComma) {
        jsonBuilder.append("{");
        jsonBuilder.append("\"name\":\"").append(escapeJsonString(name)).append("\",");
        jsonBuilder.append("\"value\":\"").append(escapeJsonString(value)).append("\",");
        jsonBuilder.append("\"inline\":").append(inline);
        jsonBuilder.append("}");

        if (addComma) {
            jsonBuilder.append(",");
        }
    }

    private String escapeJsonString(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");
    }
}