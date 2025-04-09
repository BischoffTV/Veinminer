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

public class SkillGUI {

    private final Veinminer plugin;

    public SkillGUI(Veinminer plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the skill GUI for a player
     * @param player The player
     */
    public void openSkillGUI(Player player) {
        // Check if skill system is enabled
        if (!plugin.getSkillManager().isEnabled()) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.disabled"));
            return;
        }

        // Create inventory
        Inventory inventory = Bukkit.createInventory(null, 36, ChatColor.GOLD + "VeinMiner Skills");

        // Get player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            plugin.getLogger().warning("Failed to load player data for " + player.getName());
            player.sendMessage(ChatColor.RED + "Failed to load your player data. Please try again later.");
            return;
        }

        // Skill points display
        int skillPoints = playerData.getSkillPoints();
        ItemStack skillPointsItem = createItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.GOLD + "Skill Points: " + skillPoints,
                ChatColor.GRAY + "You have " + skillPoints + " skill points to spend");
        inventory.setItem(4, skillPointsItem);

        // Efficiency Boost skill
        int efficiencyLevel = playerData.getEfficiencyLevel();
        double efficiencyBoost = plugin.getSkillManager().getEfficiencyBoost(efficiencyLevel);
        List<String> efficiencyLore = new ArrayList<>();
        efficiencyLore.add(ChatColor.GRAY + "Current Level: " + ChatColor.GOLD + efficiencyLevel + "/" + plugin.getSkillManager().getMaxSkillLevel());
        efficiencyLore.add(ChatColor.GRAY + "Effect: " + ChatColor.GOLD + efficiencyBoost + "% chance to reduce durability consumption");

        if (efficiencyLevel < plugin.getSkillManager().getMaxSkillLevel() && skillPoints > 0) {
            double nextBoost = plugin.getSkillManager().getEfficiencyBoost(efficiencyLevel + 1);
            efficiencyLore.add(ChatColor.GRAY + "Next Level: " + ChatColor.GOLD + nextBoost + "% chance");
            efficiencyLore.add(ChatColor.GREEN + "Click to upgrade (1 skill point)");
        } else if (efficiencyLevel >= plugin.getSkillManager().getMaxSkillLevel()) {
            efficiencyLore.add(ChatColor.GOLD + "Maximum level reached");
        } else {
            efficiencyLore.add(ChatColor.RED + "Not enough skill points");
        }

        ItemStack efficiencyItem = createItem(Material.DIAMOND_PICKAXE,
                ChatColor.AQUA + "Efficiency Boost",
                efficiencyLore);
        inventory.setItem(11, efficiencyItem);

        // Luck Enhancement skill
        int luckLevel = playerData.getLuckLevel();
        double luckBoost = plugin.getSkillManager().getLuckEnhancement(luckLevel);
        List<String> luckLore = new ArrayList<>();
        luckLore.add(ChatColor.GRAY + "Current Level: " + ChatColor.GOLD + luckLevel + "/" + plugin.getSkillManager().getMaxSkillLevel());
        luckLore.add(ChatColor.GRAY + "Effect: " + ChatColor.GOLD + luckBoost + "% chance to get additional drops");

        if (luckLevel < plugin.getSkillManager().getMaxSkillLevel() && skillPoints > 0) {
            double nextBoost = plugin.getSkillManager().getLuckEnhancement(luckLevel + 1);
            luckLore.add(ChatColor.GRAY + "Next Level: " + ChatColor.GOLD + nextBoost + "% chance");
            luckLore.add(ChatColor.GREEN + "Click to upgrade (1 skill point)");
        } else if (luckLevel >= plugin.getSkillManager().getMaxSkillLevel()) {
            luckLore.add(ChatColor.GOLD + "Maximum level reached");
        } else {
            luckLore.add(ChatColor.RED + "Not enough skill points");
        }

        ItemStack luckItem = createItem(Material.GOLD_INGOT,
                ChatColor.YELLOW + "Luck Enhancement",
                luckLore);
        inventory.setItem(13, luckItem);

        // Energy Conservation skill
        int energyLevel = playerData.getEnergyLevel();
        double energyBoost = plugin.getSkillManager().getEnergyConservation(energyLevel);
        List<String> energyLore = new ArrayList<>();
        energyLore.add(ChatColor.GRAY + "Current Level: " + ChatColor.GOLD + energyLevel + "/" + plugin.getSkillManager().getMaxSkillLevel());
        energyLore.add(ChatColor.GRAY + "Effect: " + ChatColor.GOLD + energyBoost + "% chance to reduce hunger consumption");

        if (energyLevel < plugin.getSkillManager().getMaxSkillLevel() && skillPoints > 0) {
            double nextBoost = plugin.getSkillManager().getEnergyConservation(energyLevel + 1);
            energyLore.add(ChatColor.GRAY + "Next Level: " + ChatColor.GOLD + nextBoost + "% chance");
            energyLore.add(ChatColor.GREEN + "Click to upgrade (1 skill point)");
        } else if (energyLevel >= plugin.getSkillManager().getMaxSkillLevel()) {
            energyLore.add(ChatColor.GOLD + "Maximum level reached");
        } else {
            energyLore.add(ChatColor.RED + "Not enough skill points");
        }

        ItemStack energyItem = createItem(Material.COOKED_BEEF,
                ChatColor.RED + "Energy Conservation",
                energyLore);
        inventory.setItem(15, energyItem);

        // Reset skills button
        List<String> resetLore = new ArrayList<>();
        resetLore.add(ChatColor.GRAY + "Reset all your skills and");
        resetLore.add(ChatColor.GRAY + "get your skill points back");

        if (plugin.getConfig().getBoolean("reset.cost-enabled", true)) {
            double cost = plugin.getConfig().getDouble("reset.cost", 1000.0);
            resetLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + cost);
        }

        ItemStack resetItem = createItem(Material.BARRIER,
                ChatColor.DARK_RED + "Reset Skills",
                resetLore);
        inventory.setItem(31, resetItem);

        // Back button
        ItemStack backButton = createItem(Material.OAK_DOOR,
                ChatColor.GREEN + "Back to Main Menu",
                ChatColor.GRAY + "Return to the main menu");
        inventory.setItem(27, backButton);

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
     * Create an item with a name and lore
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
     * Create an item with a name and lore
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
}