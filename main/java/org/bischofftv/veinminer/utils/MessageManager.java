package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class MessageManager {

    private final Veinminer plugin;
    private FileConfiguration langConfig;

    public MessageManager(Veinminer plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Reload the message configuration
     */
    public void reload() {
        langConfig = plugin.getLangConfig();
    }

    /**
     * Get a message from the language configuration
     * @param path The path to the message
     * @param defaultMessage The default message if the path is not found
     * @return The message
     */
    public String getMessage(String path, String defaultMessage) {
        String message = langConfig.getString(path);
        if (message == null) {
            return defaultMessage;
        }
        return message;
    }

    /**
     * Format a message with the plugin prefix and color codes
     * @param path The path to the message
     * @return The formatted message
     */
    public String formatMessage(String path) {
        String prefix = getMessage("messages.prefix", "&8[&6VeinMiner&8] ");
        String message = getMessage(path, "Message not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * Format a message with the plugin prefix, color codes, and a placeholder
     * @param path The path to the message
     * @param placeholder The placeholder to replace
     * @param replacement The replacement for the placeholder
     * @return The formatted message
     */
    public String formatMessage(String path, String placeholder, String replacement) {
        String prefix = getMessage("messages.prefix", "&8[&6VeinMiner&8] ");
        String message = getMessage(path, "Message not found: " + path);
        message = message.replace(placeholder, replacement);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * Format a message with the plugin prefix, color codes, and multiple placeholders
     * @param path The path to the message
     * @param placeholdersAndReplacements The placeholders and replacements in pairs
     * @return The formatted message
     */
    public String formatMessage(String path, String... placeholdersAndReplacements) {
        if (placeholdersAndReplacements.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders and replacements must be in pairs");
        }

        String prefix = getMessage("messages.prefix", "&8[&6VeinMiner&8] ");
        String message = getMessage(path, "Message not found: " + path);

        for (int i = 0; i < placeholdersAndReplacements.length; i += 2) {
            String placeholder = placeholdersAndReplacements[i];
            String replacement = placeholdersAndReplacements[i + 1];
            message = message.replace(placeholder, replacement);
        }

        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }
}