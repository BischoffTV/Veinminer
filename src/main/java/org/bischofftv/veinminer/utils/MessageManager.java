package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    
    private final Veinminer plugin;
    private YamlConfiguration langConfig;
    private final Map<String, String> messageCache;
    
    public MessageManager(Veinminer plugin) {
        this.plugin = plugin;
        this.messageCache = new HashMap<>();
        loadLanguageConfig();
    }
    
    private void loadLanguageConfig() {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        
        if (!langFile.exists()) {
            try (InputStream in = plugin.getResource("lang.yml")) {
                Files.copy(in, langFile.toPath());
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create lang.yml: " + e.getMessage());
                return;
            }
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        messageCache.clear();
    }
    
    public void reloadMessages() {
        loadLanguageConfig();
    }
    
    public String getMessage(String path) {
        return messageCache.computeIfAbsent(path, k -> {
            String message = langConfig.getString(path);
            if (message == null) {
                plugin.getLogger().warning("Missing message for path: " + path);
                return "Missing message: " + path;
            }
            return ChatColor.translateAlternateColorCodes('&', message);
        });
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        
        return message;
    }
}
