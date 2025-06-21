package org.bischofftv.veinminer.gui;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TopPlayersGUI {

    private final Veinminer plugin;

    // Cache for top players data
    private Map<String, List<TopPlayerData>> topPlayersCache = new HashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds

    public TopPlayersGUI(Veinminer plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the top players GUI for a player
     * @param player The player
     */
    public void openTopPlayersGUI(Player player) {
        // Check if top players GUI is enabled
        if (!plugin.getConfig().getBoolean("gui.show-top-players", true)) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.top-players.disabled"));
            return;
        }

        // Create inventory
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.GOLD + "VeinMiner Top Players");

        // Check if cache needs to be refreshed
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_DURATION) {
            refreshCache();
        }

        // Add top players by level
        if (plugin.getLevelManager().isEnabled()) {
            ItemStack levelItem = createItem(Material.EXPERIENCE_BOTTLE,
                    ChatColor.GOLD + plugin.getMessageManager().getMessage("gui.top-players.level-title", "Top Players by Level"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.top-players.level-lore", "Click to view top players by level"));
            inventory.setItem(11, levelItem);
        }

        // Add top players by achievements
        if (plugin.getAchievementManager().isEnabled()) {
            ItemStack achievementsItem = createItem(Material.GOLD_INGOT,
                    ChatColor.GOLD + plugin.getMessageManager().getMessage("gui.top-players.achievements-title", "Top Players by Achievements"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.top-players.achievements-lore", "Click to view top players by achievements"));
            inventory.setItem(13, achievementsItem);
        }

        // Add top players by blocks mined
        ItemStack blocksItem = createItem(Material.DIAMOND_PICKAXE,
                ChatColor.GOLD + plugin.getMessageManager().getMessage("gui.top-players.blocks-title", "Top Players by Blocks Mined"),
                ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.top-players.blocks-lore", "Click to view top players by blocks mined"));
        inventory.setItem(15, blocksItem);

        // Add back button
        ItemStack backButton = createItem(Material.OAK_DOOR,
                ChatColor.GREEN + plugin.getMessageManager().getMessage("gui.back-title", "Back to Main Menu"),
                ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.back-lore", "Return to the main menu"));
        inventory.setItem(49, backButton);

        // Fill empty slots with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Open the top players by level GUI for a player
     * @param player The player
     */
    public void openTopLevelPlayersGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Top Players by Level");

        List<TopPlayerData> topPlayers = topPlayersCache.getOrDefault("level", new ArrayList<>());

        // Add player heads for top players
        for (int i = 0; i < Math.min(topPlayers.size(), 10); i++) {
            TopPlayerData playerData = topPlayers.get(i);

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "#" + (i + 1) + " " + playerData.getPlayerName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Level: " + ChatColor.GREEN + playerData.getValue());
            meta.setLore(lore);

            // Try to set the player's skin
            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerData.getPlayerName()));
            } catch (Exception e) {
                // Ignore if we can't set the skin
            }

            playerHead.setItemMeta(meta);
            inventory.setItem(i + 10, playerHead);
        }

        // Add back button
        ItemStack backButton = createItem(Material.OAK_DOOR,
                ChatColor.GREEN + plugin.getMessageManager().getMessage("gui.back-title", "Back to Top Players"),
                ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.back-lore", "Return to the top players menu"));
        inventory.setItem(49, backButton);

        // Fill empty slots with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Open the top players by achievements GUI for a player
     * @param player The player
     */
    public void openTopAchievementsPlayersGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Top Players by Achievements");

        List<TopPlayerData> topPlayers = topPlayersCache.getOrDefault("achievements", new ArrayList<>());

        // Add player heads for top players
        for (int i = 0; i < Math.min(topPlayers.size(), 10); i++) {
            TopPlayerData playerData = topPlayers.get(i);

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "#" + (i + 1) + " " + playerData.getPlayerName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Achievements: " + ChatColor.GREEN + playerData.getValue());
            meta.setLore(lore);

            // Try to set the player's skin
            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerData.getPlayerName()));
            } catch (Exception e) {
                // Ignore if we can't set the skin
            }

            playerHead.setItemMeta(meta);
            inventory.setItem(i + 10, playerHead);
        }

        // Add back button
        ItemStack backButton = createItem(Material.OAK_DOOR,
                ChatColor.GREEN + plugin.getMessageManager().getMessage("gui.back-title", "Back to Top Players"),
                ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.back-lore", "Return to the top players menu"));
        inventory.setItem(49, backButton);

        // Fill empty slots with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Open the top players by blocks mined GUI for a player
     * @param player The player
     */
    public void openTopBlocksPlayersGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Top Players by Blocks Mined");

        List<TopPlayerData> topPlayers = topPlayersCache.getOrDefault("blocks", new ArrayList<>());

        // Add player heads for top players
        for (int i = 0; i < Math.min(topPlayers.size(), 10); i++) {
            TopPlayerData playerData = topPlayers.get(i);

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "#" + (i + 1) + " " + playerData.getPlayerName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Blocks Mined: " + ChatColor.GREEN + playerData.getValue());
            meta.setLore(lore);

            // Try to set the player's skin
            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerData.getPlayerName()));
            } catch (Exception e) {
                // Ignore if we can't set the skin
            }

            playerHead.setItemMeta(meta);
            inventory.setItem(i + 10, playerHead);
        }

        // Add back button
        ItemStack backButton = createItem(Material.OAK_DOOR,
                ChatColor.GREEN + plugin.getMessageManager().getMessage("gui.back-title", "Back to Top Players"),
                ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.back-lore", "Return to the top players menu"));
        inventory.setItem(49, backButton);

        // Fill empty slots with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Handle inventory click event
     * @param player The player
     * @param slot The slot that was clicked
     * @param title The title of the inventory
     */
    public void handleInventoryClick(Player player, int slot, String title) {
        if (title.equals(ChatColor.GOLD + "VeinMiner Top Players")) {
            if (slot == 11 && plugin.getLevelManager().isEnabled()) {
                // Top players by level
                openTopLevelPlayersGUI(player);
            } else if (slot == 13 && plugin.getAchievementManager().isEnabled()) {
                // Top players by achievements
                openTopAchievementsPlayersGUI(player);
            } else if (slot == 15) {
                // Top players by blocks mined
                openTopBlocksPlayersGUI(player);
            } else if (slot == 49) {
                // Back to main menu
                player.closeInventory();
                plugin.getMainGUI().openMainGUI(player);
            }
        } else if (title.equals(ChatColor.GOLD + "Top Players by Level") ||
                title.equals(ChatColor.GOLD + "Top Players by Achievements") ||
                title.equals(ChatColor.GOLD + "Top Players by Blocks Mined")) {
            if (slot == 49) {
                // Back to top players menu
                openTopPlayersGUI(player);
            }
        }
    }

    /**
     * Refresh the cache of top players
     */
    private void refreshCache() {
        if (plugin.getDatabaseManager() == null || !plugin.getDatabaseManager().checkConnection()) {
            return;
        }

        // Clear existing cache
        topPlayersCache.clear();

        // Get top players by level
        topPlayersCache.put("level", getTopPlayersByLevel(10));

        // Get top players by achievements
        topPlayersCache.put("achievements", getTopPlayersByAchievements(10));

        // Get top players by blocks mined
        topPlayersCache.put("blocks", getTopPlayersByBlocksMined(10));

        // Update cache timestamp
        lastCacheUpdate = System.currentTimeMillis();
    }

    /**
     * Get top players by level
     * @param limit The maximum number of players to return
     * @return A list of top players by level
     */
    private List<TopPlayerData> getTopPlayersByLevel(int limit) {
        List<TopPlayerData> topPlayers = new ArrayList<>();

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = plugin.getDatabaseManager().getConnection();
            if (connection == null) {
                return topPlayers;
            }

            String sql = "SELECT player_name, level FROM " + plugin.getDatabaseManager().getTablePrefix() +
                    "player_data ORDER BY level DESC, experience DESC LIMIT ?";
            statement = connection.prepareStatement(sql);
            statement.setInt(1, limit);

            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                int level = resultSet.getInt("level");

                topPlayers.add(new TopPlayerData(playerName, level));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get top players by level: " + e.getMessage());
        } finally {
            plugin.getDatabaseManager().closeResources(resultSet, statement, connection);
        }

        return topPlayers;
    }

    /**
     * Get top players by achievements completed
     * @param limit The maximum number of players to return
     * @return A list of top players by achievements completed
     */
    private List<TopPlayerData> getTopPlayersByAchievements(int limit) {
        List<TopPlayerData> topPlayers = new ArrayList<>();

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = plugin.getDatabaseManager().getConnection();
            if (connection == null) {
                return topPlayers;
            }

            String sql = "SELECT a.uuid, p.player_name, COUNT(*) as completed_count " +
                    "FROM " + plugin.getDatabaseManager().getTablePrefix() + "achievements a " +
                    "JOIN " + plugin.getDatabaseManager().getTablePrefix() + "player_data p ON a.uuid = p.uuid " +
                    "WHERE a.completed = 1 " +
                    "GROUP BY a.uuid, p.player_name " +
                    "ORDER BY completed_count DESC LIMIT ?";

            statement = connection.prepareStatement(sql);
            statement.setInt(1, limit);

            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                int completedCount = resultSet.getInt("completed_count");

                topPlayers.add(new TopPlayerData(playerName, completedCount));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get top players by achievements: " + e.getMessage());
        } finally {
            plugin.getDatabaseManager().closeResources(resultSet, statement, connection);
        }

        return topPlayers;
    }

    /**
     * Get top players by blocks mined
     * @param limit The maximum number of players to return
     * @return A list of top players by blocks mined
     */
    private List<TopPlayerData> getTopPlayersByBlocksMined(int limit) {
        List<TopPlayerData> topPlayers = new ArrayList<>();

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = plugin.getDatabaseManager().getConnection();
            if (connection == null) {
                return topPlayers;
            }

            String sql = "SELECT player_name, blocks_mined FROM " + plugin.getDatabaseManager().getTablePrefix() +
                    "player_data ORDER BY blocks_mined DESC LIMIT ?";
            statement = connection.prepareStatement(sql);
            statement.setInt(1, limit);

            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                int blocksMined = resultSet.getInt("blocks_mined");

                topPlayers.add(new TopPlayerData(playerName, blocksMined));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get top players by blocks mined: " + e.getMessage());
        } finally {
            plugin.getDatabaseManager().closeResources(resultSet, statement, connection);
        }

        return topPlayers;
    }

    /**
     * Create an item for the GUI
     * @param material The material
     * @param name The name
     * @param lore The lore
     * @return The item
     */
    private ItemStack createItem(Material material, String name, String lore) {
        List<String> loreList = new ArrayList<>();
        loreList.add(lore);
        return createItem(material, name, loreList);
    }

    /**
     * Create an item for the GUI
     * @param material The material
     * @param name The name
     * @param lore The lore
     * @return The item
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Class to store top player data
     */
    private static class TopPlayerData {
        private final String playerName;
        private final int value;

        public TopPlayerData(String playerName, int value) {
            this.playerName = playerName;
            this.value = value;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getValue() {
            return value;
        }
    }
}