package org.bischofftv.veinminer.achievements;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementGUI {

    private final Veinminer plugin;
    private final Map<String, Integer> achievementSlots;

    public AchievementGUI(Veinminer plugin) {
        this.plugin = plugin;
        this.achievementSlots = new HashMap<>();
    }

    /**
     * Open the achievement GUI for a player
     * @param player The player
     */
    public void openAchievementGUI(Player player) {
        // Check if achievement system is enabled
        if (!plugin.getAchievementManager().isEnabled()) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.achievements.disabled"));
            return;
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Opening achievement GUI for " + player.getName());
        }

        // Create inventory with translated title
        String title = plugin.getMessageManager().getMessage("gui.achievements-title", "VeinMiner Achievements");
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', title));

        // Get player achievements
        Map<String, Integer> achievements = plugin.getAchievementManager().getPlayerAchievements(player);
        Map<String, Boolean> claimedRewards = plugin.getAchievementManager().getPlayerClaimedRewards(player);
        Map<String, Map<String, Object>> definitions = plugin.getAchievementManager().getAchievementDefinitions();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Player " + player.getName() + " has " + achievements.size() + " achievements");
            plugin.getLogger().info("[Debug] Found " + definitions.size() + " achievement definitions");
        }

        // Reset achievement slots
        achievementSlots.clear();

        // Check if there are any achievements
        if (definitions.isEmpty()) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] No achievement definitions found");
            }

            ItemStack noAchievementsItem = createItem(Material.BARRIER,
                    ChatColor.translateAlternateColorCodes('&', plugin.getMessageManager().getMessage("gui.achievements-none-title", "&cNo Achievements Available")),
                    ChatColor.translateAlternateColorCodes('&', plugin.getMessageManager().getMessage("gui.achievements-none-lore", "&7No achievements have been defined.")));
            inventory.setItem(22, noAchievementsItem);
        } else {
            // Add achievements to inventory
            int slot = 10;
            for (Map.Entry<String, Map<String, Object>> entry : definitions.entrySet()) {
                String achievementId = entry.getKey();
                Map<String, Object> achievement = entry.getValue();

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Processing achievement: " + achievementId);
                }

                // Get achievement details
                String name = (String) achievement.get("name");
                String description = (String) achievement.get("description");
                int requiredAmount = (int) achievement.get("amount");
                int progress = achievements.getOrDefault(achievementId, 0);
                boolean completed = progress >= requiredAmount;
                boolean claimed = claimedRewards.getOrDefault(achievementId, false);

                // Create lore
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + description);
                lore.add("");
                lore.add(ChatColor.YELLOW + plugin.getMessageManager().getMessage("gui.achievements-progress", "Progress: ") +
                        ChatColor.GOLD + progress + "/" + requiredAmount);

                if (completed) {
                    lore.add(ChatColor.GREEN + plugin.getMessageManager().getMessage("gui.achievements-completed", "✓ Completed!"));

                    // Add reward info
                    Map<String, Object> rewards = (Map<String, Object>) achievement.get("rewards");
                    if (rewards != null) {
                        lore.add("");
                        lore.add(ChatColor.YELLOW + plugin.getMessageManager().getMessage("gui.achievements-rewards", "Rewards:"));

                        // Money reward
                        double money = (double) rewards.getOrDefault("money", 0.0);
                        if (money > 0) {
                            lore.add(ChatColor.GOLD + "- " + money + " " + plugin.getMessageManager().getMessage("gui.achievements-money", "money"));
                        }

                        // Item rewards
                        List<String> items = (List<String>) rewards.getOrDefault("items", new ArrayList<>());
                        for (String itemString : items) {
                            String[] parts = itemString.split(":");
                            if (parts.length >= 2) {
                                try {
                                    Material material = Material.valueOf(parts[0]);
                                    int amount = Integer.parseInt(parts[1]);
                                    lore.add(ChatColor.GOLD + "- " + amount + "x " + formatMaterialName(material.name()));
                                } catch (IllegalArgumentException e) {
                                    plugin.getLogger().warning("Invalid item in achievement rewards: " + itemString);
                                }
                            }
                        }
                    }

                    // Add claim status
                    lore.add("");
                    if (claimed) {
                        lore.add(ChatColor.GREEN + plugin.getMessageManager().getMessage("gui.achievements-claimed", "Rewards claimed"));
                    } else {
                        lore.add(ChatColor.YELLOW + plugin.getMessageManager().getMessage("gui.achievements-claim", "Click to claim rewards"));
                    }
                } else {
                    lore.add(ChatColor.RED + plugin.getMessageManager().getMessage("gui.achievements-not-completed", "✗ Not completed"));
                }

                // Get material for achievement
                Material material = Material.PAPER;
                Material customIcon = plugin.getAchievementManager().getCustomIcon(achievementId);
                if (customIcon != null) {
                    material = customIcon;
                }

                // Create item
                ItemStack achievementItem = createItem(material,
                        ChatColor.GOLD + plugin.getMessageManager().getMessage("gui.achievements-title-item", "Achievement: %name%").replace("%name%", name),
                        lore);

                // Add to inventory
                inventory.setItem(slot, achievementItem);
                achievementSlots.put(achievementId, slot);

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Added achievement " + achievementId + " to slot " + slot);
                }

                // Update slot
                slot++;
                if ((slot + 1) % 9 == 0) {
                    slot += 2;
                }

                // Stop if we run out of space
                if (slot >= 45) {
                    break;
                }
            }
        }

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

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Opening achievement GUI for " + player.getName() + " with " + achievementSlots.size() + " achievements");
        }

        player.openInventory(inventory);
    }

    /**
     * Handle inventory click event
     * @param event The inventory click event
     */
    public void handleInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Back button
        if (slot == 49) {
            player.closeInventory();
            plugin.getMainGUI().openMainGUI(player);
            return;
        }

        // Check if clicked on an achievement
        for (Map.Entry<String, Integer> entry : achievementSlots.entrySet()) {
            String achievementId = entry.getKey();
            int achievementSlot = entry.getValue();

            if (slot == achievementSlot) {
                // Check if achievement is completed and rewards not claimed
                if (plugin.getAchievementManager().hasCompletedAchievement(player, achievementId) &&
                        !plugin.getAchievementManager().hasClaimedRewards(player, achievementId)) {
                    // Claim rewards
                    if (plugin.getAchievementManager().claimAchievementRewards(player, achievementId)) {
                        // Reopen GUI
                        player.closeInventory();
                        openAchievementGUI(player);
                    }
                }
                return;
            }
        }
    }

    /**
     * Format material name for display
     * @param materialName The material name
     * @return The formatted name
     */
    private String formatMaterialName(String materialName) {
        String[] words = materialName.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase());
                result.append(word.substring(1).toLowerCase());
                result.append(" ");
            }
        }

        return result.toString().trim();
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