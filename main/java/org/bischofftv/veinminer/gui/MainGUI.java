package org.bischofftv.veinminer.gui;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MainGUI {

    private final Veinminer plugin;

    public MainGUI(Veinminer plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the main GUI for a player
     * @param player The player
     */
    public void openMainGUI(Player player) {
        // Create inventory with title from lang.yml
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getMessageManager().getMessage("gui.main-title", "VeinMiner Menu"));
        Inventory inventory = Bukkit.createInventory(null, 36, ChatColor.GREEN + "VeinMiner Menu");

        // Get player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            plugin.getLogger().warning("Failed to load player data for " + player.getName());
            player.sendMessage(ChatColor.RED + plugin.getMessageManager().getMessage("messages.error.player-data-not-found", "Failed to load your player data. Please try again later."));
            return;
        }

        boolean veinMinerEnabled = playerData.isVeinMinerEnabled();

        // Toggle VeinMiner item (green or red wool based on status)
        if (veinMinerEnabled) {
            inventory.setItem(10, createItem(Material.LIME_WOOL,
                    ChatColor.GREEN + plugin.getMessageManager().getMessage("gui.toggle-enabled", "VeinMiner: Enabled"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.toggle-lore-enabled", "Click to disable VeinMiner"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.toggle-hint-1", "When enabled, hold shift while mining"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.toggle-hint-2", "to activate VeinMiner.")));
        } else {
            inventory.setItem(10, createItem(Material.RED_WOOL,
                    ChatColor.RED + plugin.getMessageManager().getMessage("gui.toggle-disabled", "VeinMiner: Disabled"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.toggle-lore-disabled", "Click to enable VeinMiner"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.toggle-hint-1", "When enabled, hold shift while mining"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.toggle-hint-2", "to activate VeinMiner.")));
        }

        // Tools Settings
        inventory.setItem(12, createItem(Material.DIAMOND_PICKAXE,
                ChatColor.AQUA + plugin.getMessageManager().getMessage("gui.tools-title", "Tools Settings"),
                ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.tools-lore", "Configure which tools to use")));

        // Level Information - only show if level system is enabled
        if (plugin.getLevelManager().isEnabled()) {
            int level = playerData.getLevel();
            int xp = playerData.getExperience();
            int maxBlocks = plugin.getLevelManager().getMaxBlocksForLevel(level);
            int nextLevelXp = plugin.getLevelManager().getXpForNextLevel(level);

            // Create level information item with the exact format from the screenshot
            List<String> levelLore = new ArrayList<>();
            levelLore.add(ChatColor.YELLOW + plugin.getMessageManager().getMessage("gui.level-level", "Level: ") + ChatColor.GOLD + level);
            levelLore.add(ChatColor.YELLOW + plugin.getMessageManager().getMessage("gui.level-xp", "XP: ") + ChatColor.GOLD + xp);
            levelLore.add(ChatColor.YELLOW + plugin.getMessageManager().getMessage("gui.level-max-blocks", "Max Blocks: ") + ChatColor.GOLD + maxBlocks);
            levelLore.add(ChatColor.YELLOW + plugin.getMessageManager().getMessage("gui.level-next", "XP for Level ") + (level + 1) + ": " + ChatColor.GOLD + nextLevelXp);
            levelLore.add(ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.level-click", "Click for detailed level information"));

            ItemStack levelItem = createItem(Material.EXPERIENCE_BOTTLE,
                    ChatColor.DARK_PURPLE + plugin.getMessageManager().getMessage("gui.level-title", "Level Information"),
                    levelLore);
            inventory.setItem(14, levelItem);
        }

        // Skills - only show if skill system is enabled
        if (plugin.getSkillManager().isEnabled()) {
            inventory.setItem(16, createItem(Material.ENCHANTED_BOOK,
                    ChatColor.GOLD + plugin.getMessageManager().getMessage("gui.skills-title", "Skills"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.skills-lore", "Manage your VeinMiner skills")));
        }

        // Achievements - only show if achievement system is enabled
        if (plugin.getAchievementManager().isEnabled()) {
            inventory.setItem(19, createItem(Material.GOLD_INGOT,
                    ChatColor.GOLD + plugin.getMessageManager().getMessage("gui.achievements-title", "Achievements"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.achievements-lore", "View your achievements")));
        }

        // Top Players - add to slot 20 to maintain the pattern
        if (plugin.getConfig().getBoolean("gui.show-top-players", true)) {
            inventory.setItem(21, createItem(Material.PLAYER_HEAD,
                    ChatColor.AQUA + plugin.getMessageManager().getMessage("gui.top-players-title", "Top Players"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.top-players-lore", "View top players")));
        }

        // About
        if (plugin.getConfig().getBoolean("gui.show-about", true)) {
            inventory.setItem(23, createItem(Material.OAK_SIGN,
                    ChatColor.WHITE + plugin.getMessageManager().getMessage("gui.about-title", "About"),
                    ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.about-lore", "Information about VeinMiner")));
        }

        // Help
        inventory.setItem(25, createItem(Material.APPLE,
                ChatColor.RED + plugin.getMessageManager().getMessage("gui.help-title", "Help"),
                ChatColor.GRAY + plugin.getMessageManager().getMessage("gui.help-lore", "View help information")));

        // Fill empty slots with gray glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Open the main menu for a player (alias for openMainGUI)
     * @param player The player
     */
    public void openMainMenu(Player player) {
        openMainGUI(player);
    }

    private ItemStack createItem(Material material, String name, String lore) {
        List<String> loreList = new ArrayList<>();
        loreList.add(lore);
        return createItem(material, name, loreList);
    }

    /**
     * Create an item with multiple lore lines
     * @param material The material
     * @param name The name
     * @param loreLines The lore lines
     * @return The item
     */
    private ItemStack createItem(Material material, String name, String... loreLines) {
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(line);
        }
        return createItem(material, name, lore);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}

