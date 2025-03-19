package org.bischofftv.veinminer.gui;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.database.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainGUI {

    private final Veinminer plugin;

    public MainGUI(Veinminer plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        // Create inventory with 3 rows (27 slots)
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.GREEN + "VeinMiner Menu");

        // Fill inventory with gray stained glass panes
        ItemStack filler = createFillerItem();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Create items for the menu
        // Create toggle indicator block based on enabled status
        boolean isEnabled = plugin.getPlayerDataManager().isVeinMinerEnabled(player);
        Material toggleBlockMaterial = isEnabled ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        ItemStack toggleBlock = createItem(toggleBlockMaterial, ChatColor.GREEN + "VeinMiner: " +
                        (isEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"),
                ChatColor.YELLOW + "Click to " + (isEnabled ? "disable" : "enable") + " VeinMiner",
                ChatColor.GRAY + "When enabled, hold shift while mining",
                ChatColor.GRAY + "to activate VeinMiner.");

        ItemStack pickaxeItem = createToolItem(player, "pickaxe", Material.DIAMOND_PICKAXE);
        ItemStack axeItem = createToolItem(player, "axe", Material.DIAMOND_AXE);
        ItemStack shovelItem = createToolItem(player, "shovel", Material.DIAMOND_SHOVEL);
        ItemStack hoeItem = createToolItem(player, "hoe", Material.DIAMOND_HOE);

        ItemStack levelItem = createLevelItem(player);
        ItemStack achievementsItem = createAchievementsItem();

        // Create About item if enabled in config
        ItemStack aboutItem = null;
        if (plugin.getConfig().getBoolean("gui.show-about", true)) {
            aboutItem = createAboutItem();
        }

        // Place items in inventory
        inventory.setItem(10, toggleBlock);  // VeinMiner toggle
        inventory.setItem(11, pickaxeItem); // Pickaxe settings
        inventory.setItem(12, axeItem);    // Axe settings
        inventory.setItem(13, shovelItem); // Shovel settings
        inventory.setItem(14, hoeItem);    // Hoe settings
        inventory.setItem(15, levelItem);  // Level info
        inventory.setItem(16, achievementsItem); // Achievements

        // Place About item if enabled
        if (aboutItem != null) {
            inventory.setItem(22, aboutItem); // About VeinMiner
        }

        // Open inventory for player
        player.openInventory(inventory);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToolItem(Player player, String toolType, Material material) {
        boolean isEnabled = plugin.getPlayerDataManager().isToolEnabled(player, toolType);
        String capitalizedTool = toolType.substring(0, 1).toUpperCase() + toolType.substring(1);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName((isEnabled ? ChatColor.GOLD : ChatColor.RED) + capitalizedTool + ": " +
                (isEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Click to " + (isEnabled ? "disable" : "enable") + " for " + capitalizedTool);

        meta.setLore(lore);

        // Add enchant glow if enabled
        if (isEnabled) {
            meta.addEnchant(Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking")), 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLevelItem(Player player) {
        PlayerData playerData = plugin.getLevelManager().getPlayerData(player.getUniqueId());
        int level = playerData != null ? playerData.getLevel() : 1;
        long xp = playerData != null ? playerData.getXp() : 0;
        int maxBlocks = plugin.getLevelManager().getMaxBlocksForLevel(level);
        long xpForNextLevel = plugin.getLevelManager().getXpForNextLevel(level);

        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Level Information");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Level: " + ChatColor.GOLD + level);
        lore.add(ChatColor.YELLOW + "XP: " + ChatColor.GOLD + xp);
        lore.add(ChatColor.YELLOW + "Max Blocks: " + ChatColor.GOLD + maxBlocks);
        lore.add(ChatColor.YELLOW + "XP for Level " + (level + 1) + ": " + ChatColor.GOLD + (xpForNextLevel - xp));
        lore.add("");
        lore.add(ChatColor.GRAY + "Click for detailed level information");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createAchievementsItem() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Achievements");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "View and claim your achievements");
        lore.add("");
        lore.add(ChatColor.GRAY + "Click to open achievements menu");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createAboutItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "About VeinMiner");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Version: " + plugin.getDescription().getVersion());
        lore.add(ChatColor.YELLOW + "Author: BischoffTV");
        lore.add("");
        lore.add(ChatColor.GRAY + "A plugin that allows players to mine connected blocks of the same type at once.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Click for more information");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    public void handleInventoryClick(Player player, int slot, Inventory inventory) {
        // Ignore clicks on filler items
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        ItemStack clickedItem = inventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS) {
            return;
        }

        switch (slot) {
            case 10: // VeinMiner toggle
                boolean currentState = plugin.getPlayerDataManager().isVeinMinerEnabled(player);
                plugin.getPlayerDataManager().setVeinMinerEnabled(player, !currentState);
                openMainMenu(player); // Refresh the menu
                break;

            case 11: // Pickaxe toggle
                toggleTool(player, "pickaxe");
                openMainMenu(player); // Refresh the menu
                break;

            case 12: // Axe toggle
                toggleTool(player, "axe");
                openMainMenu(player); // Refresh the menu
                break;

            case 13: // Shovel toggle
                toggleTool(player, "shovel");
                openMainMenu(player); // Refresh the menu
                break;

            case 14: // Hoe toggle
                toggleTool(player, "hoe");
                openMainMenu(player); // Refresh the menu
                break;

            case 15: // Level info
                player.closeInventory();
                player.performCommand("veinminer level");
                break;

            case 16: // Achievements
                player.closeInventory();
                player.performCommand("veinminer achievements");
                break;

            case 22: // About (if enabled)
                if (plugin.getConfig().getBoolean("gui.show-about", true)) {
                    // Just show more detailed about info or link to website
                    player.sendMessage(ChatColor.GREEN + "=== VeinMiner ===");
                    player.sendMessage(ChatColor.YELLOW + "Version: " + plugin.getDescription().getVersion());
                    player.sendMessage(ChatColor.YELLOW + "Author: BischoffTV");
                    player.sendMessage(ChatColor.YELLOW + "Description: " + plugin.getDescription().getDescription());
                    player.sendMessage(ChatColor.YELLOW + "Hold shift while mining to activate VeinMiner!");
                    player.closeInventory();
                }
                break;
        }
    }

    private void toggleTool(Player player, String toolType) {
        boolean currentState = plugin.getPlayerDataManager().isToolEnabled(player, toolType);
        plugin.getPlayerDataManager().setToolEnabled(player, toolType, !currentState);

        // Send message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tool", toolType);

        if (!currentState) {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.tool-enabled", placeholders));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.tool-disabled", placeholders));
        }
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1, (short) 7); // Gray stained glass pane
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}