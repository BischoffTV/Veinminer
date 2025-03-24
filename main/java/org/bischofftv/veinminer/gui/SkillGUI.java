package org.bischofftv.veinminer.gui;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bischofftv.veinminer.skills.SkillManager;
import org.bischofftv.veinminer.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SkillGUI {

    private final Veinminer plugin;
    private final MessageManager messageManager;

    public SkillGUI(Veinminer plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    /**
     * Open the skill GUI for a player
     * @param player The player
     */
    public void openSkillGUI(Player player) {
        if (!plugin.getSkillManager().isEnabled()) {
            player.sendMessage(messageManager.formatMessage("messages.skills.disabled"));
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "VeinMiner Skills");

        // Get player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(messageManager.formatMessage("messages.error.player-data-not-found"));
            return;
        }

        // Get skill levels
        int efficiencyLevel = playerData.getEfficiencyLevel();
        int luckLevel = playerData.getLuckLevel();
        int energyLevel = playerData.getEnergyLevel();

        // Get available skill points
        int availablePoints = plugin.getSkillManager().getAvailableSkillPoints(player);

        // Row 1: Available skill points
        ItemStack pointsItem = createItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.GOLD + "Available Skill Points: " + ChatColor.GREEN + availablePoints,
                ChatColor.GRAY + "You can spend these points to upgrade your skills.");
        inventory.setItem(4, pointsItem);

        // Row 2: Skills
        // Efficiency Boost
        ItemStack efficiencyItem = createSkillItem(
                Material.DIAMOND_PICKAXE,
                "Efficiency Boost",
                efficiencyLevel,
                plugin.getSkillManager().getMaxSkillLevel(),
                plugin.getSkillManager().getSkillEffectValue(player, SkillManager.SkillType.EFFICIENCY),
                "Chance to reduce durability consumption",
                availablePoints > 0 && efficiencyLevel < plugin.getSkillManager().getMaxSkillLevel()
        );
        inventory.setItem(11, efficiencyItem);

        // Luck Enhancement
        ItemStack luckItem = createSkillItem(
                Material.GOLD_INGOT,
                "Luck Enhancement",
                luckLevel,
                plugin.getSkillManager().getMaxSkillLevel(),
                plugin.getSkillManager().getSkillEffectValue(player, SkillManager.SkillType.LUCK),
                "Chance to get additional drops",
                availablePoints > 0 && luckLevel < plugin.getSkillManager().getMaxSkillLevel()
        );
        inventory.setItem(13, luckItem);

        // Energy Conservation
        ItemStack energyItem = createSkillItem(
                Material.COOKED_BEEF,
                "Energy Conservation",
                energyLevel,
                plugin.getSkillManager().getMaxSkillLevel(),
                plugin.getSkillManager().getSkillEffectValue(player, SkillManager.SkillType.ENERGY),
                "Chance to reduce hunger consumption",
                availablePoints > 0 && energyLevel < plugin.getSkillManager().getMaxSkillLevel()
        );
        inventory.setItem(15, energyItem);

        // Row 3: Reset button and back button
        // Reset button
        List<String> resetLore = new ArrayList<>();
        resetLore.add(ChatColor.GRAY + "Click to reset all your skill points.");

        if (plugin.getSkillManager().isResetCostEnabled() && plugin.getSkillManager().getResetCost() > 0) {
            resetLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + plugin.getSkillManager().getResetCost());
        }

        ItemStack resetItem = createItem(Material.BARRIER, ChatColor.RED + "Reset Skills", resetLore);
        inventory.setItem(21, resetItem);

        // Back button
        ItemStack backItem = createItem(Material.OAK_DOOR, ChatColor.GREEN + "Back to Main Menu",
                ChatColor.GRAY + "Click to return to the main menu.");
        inventory.setItem(23, backItem);

        // Fill empty slots with glass panes
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
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
     * Create an item for the GUI
     * @param material The material
     * @param name The name
     * @param loreString A single lore string
     * @return The item
     */
    private ItemStack createItem(Material material, String name, String loreString) {
        List<String> lore = new ArrayList<>();
        lore.add(loreString);
        return createItem(material, name, lore);
    }

    /**
     * Create a skill item for the GUI
     * @param material The material
     * @param skillName The skill name
     * @param level The current level
     * @param maxLevel The maximum level
     * @param effectValue The effect value
     * @param description The skill description
     * @param canUpgrade Whether the skill can be upgraded
     * @return The item
     */
    private ItemStack createSkillItem(Material material, String skillName, int level, int maxLevel,
                                      double effectValue, String description, boolean canUpgrade) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + skillName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Level: " + ChatColor.GREEN + level + ChatColor.GRAY + "/" + ChatColor.GREEN + maxLevel);
        lore.add(ChatColor.YELLOW + "Effect: " + ChatColor.GREEN + effectValue + "%");
        lore.add("");

        if (canUpgrade) {
            lore.add(ChatColor.GREEN + "Click to upgrade!");
        } else if (level >= maxLevel) {
            lore.add(ChatColor.RED + "Maximum level reached!");
        } else {
            lore.add(ChatColor.RED + "Not enough skill points!");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
}