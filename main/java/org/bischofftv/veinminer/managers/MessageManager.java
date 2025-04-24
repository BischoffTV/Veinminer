package org.bischofftv.veinminer.managers;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final Veinminer plugin;
    private FileConfiguration messagesConfig;
    private final Map<String, String> messages;

    public MessageManager(Veinminer plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        loadMessages();
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load all messages from config
        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messages.put(key, ChatColor.translateAlternateColorCodes('&', messagesConfig.getString(key)));
            }
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "Message not found: " + key);
    }

    public String getMessage(String key, Object... args) {
        String message = getMessage(key);
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return message;
    }

    public String formatMessage(String key) {
        return getMessage(key);
    }

    public String formatMessage(String key, String placeholder, String value) {
        return getMessage(key).replace(placeholder, value);
    }

    public String formatMessage(String key, String... placeholdersAndValues) {
        if (placeholdersAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders and values must be in pairs");
        }

        String message = getMessage(key);
        for (int i = 0; i < placeholdersAndValues.length; i += 2) {
            String placeholder = placeholdersAndValues[i];
            String value = placeholdersAndValues[i + 1];
            message = message.replace(placeholder, value);
        }
        return message;
    }

    /**
     * Reload the message configuration
     */
    public void reload() {
        loadMessages();
    }
} 