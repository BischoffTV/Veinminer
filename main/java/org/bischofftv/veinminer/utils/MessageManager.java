package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class MessageManager {

    private final Veinminer plugin;

    public MessageManager(Veinminer plugin) {
        this.plugin = plugin;
    }

    /**
     * Format a message with the plugin prefix
     * @param path The path to the message in the lang.yml file
     * @return The formatted message
     */
    public String formatMessage(String path) {
        String message = getMessage(path);
        String prefix = getMessage("messages.prefix");
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * Format a message with the plugin prefix and replacements
     * @param path The path to the message in the lang.yml file
     * @param replacements The replacements to make in the message (key-value pairs)
     * @return The formatted message
     */
    public String formatMessage(String path, String... replacements) {
        String message = getMessage(path);

        // Apply replacements
        if (replacements != null && replacements.length >= 2) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    message = message.replace(replacements[i], replacements[i + 1]);
                }
            }
        }

        String prefix = getMessage("messages.prefix");
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * Format a message with the plugin prefix and replacements
     * @param path The path to the message in the lang.yml file
     * @param replacements The replacements to make in the message
     * @return The formatted message
     */
    public String formatMessage(String path, Map<String, String> replacements) {
        String message = getMessage(path);

        // Apply replacements
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        String prefix = getMessage("messages.prefix");
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * Get a message from the lang.yml file
     * @param path The path to the message
     * @return The message
     */
    public String getMessage(String path) {
        // Get from lang.yml
        FileConfiguration langConfig = plugin.getLangConfig();
        if (langConfig != null && langConfig.contains(path)) {
            return langConfig.getString(path, "Message not found: " + path);
        }

        return "Message not found: " + path;
    }

    /**
     * Get a message from the lang.yml file with replacements
     * @param path The path to the message
     * @param replacements The replacements to make in the message
     * @return The message
     */
    public String getMessage(String path, Map<String, String> replacements) {
        String message = getMessage(path);

        // Apply replacements
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        return message;
    }

    /**
     * Get a message from the lang.yml file with a default value
     * @param path The path to the message
     * @param defaultValue The default value to return if the message is not found
     * @return The message or the default value
     */
    public String getMessage(String path, String defaultValue) {
        // Get from lang.yml
        FileConfiguration langConfig = plugin.getLangConfig();
        if (langConfig != null && langConfig.contains(path)) {
            return langConfig.getString(path, defaultValue);
        }

        return defaultValue;
    }

    /**
     * Reload the messages from the lang.yml file
     */
    public void reloadMessages() {
        plugin.reloadLangConfig();
    }
}