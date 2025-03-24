package org.bischofftv.veinminer.achievements;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementGUI {

    private final Veinminer plugin;
    private final Map<String, Material> achievementIcons;

    public AchievementGUI(Veinminer plugin) {
        this.plugin = plugin;
        this.achievementIcons = new HashMap<>();
        initializeIcons();
    }

    private void initializeIcons() {
        // Default icons for achievement types
        achievementIcons.put("BLOCK_MINE", Material.DIAMOND_PICKAXE);
        achievementIcons.put("TOTAL_BLOCKS", Material.IRON_PICKAXE);
        achievementIcons.put("LEVEL", Material.EXPERIENCE_BOTTLE);
        achievementIcons.put("SKILL_MASTER", Material.ENCHANTED_BOOK);

        // Specific achievement icons - set defaults first
        achievementIcons.put("mine_coal", Material.COAL_ORE);
        achievementIcons.put("mine_iron", Material.IRON_ORE);
        achievementIcons.put("mine_gold", Material.GOLD_ORE);
        achievementIcons.put("mine_gold_2", Material.GOLD_BLOCK);
        achievementIcons.put("mine_diamond", Material.DIAMOND_ORE);
        achievementIcons.put("mine_diamond_2", Material.DIAMOND_BLOCK);
        achievementIcons.put("mine_ancient_debris", Material.ANCIENT_DEBRIS);
        achievementIcons.put("total_blocks", Material.STONE);
        achievementIcons.put("total_blocks_2", Material.DIAMOND_PICKAXE);
        achievementIcons.put("total_blocks_advanced", Material.NETHERITE_PICKAXE);
        achievementIcons.put("reach_level_5", Material.EXPERIENCE_BOTTLE);
        achievementIcons.put("reach_level_10", Material.ENCHANTED_GOLDEN_APPLE);
        achievementIcons.put("skill_master", Material.ENCHANTED_BOOK);

        // Try to load custom icons from config
        ConfigurationSection iconsSection = plugin.getConfig().getConfigurationSection("achievement-system.icons");
        if (iconsSection != null) {
            for (String achievementId : iconsSection.getKeys(false)) {
                String materialName = iconsSection.getString(achievementId);
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    achievementIcons.put(achievementId, material);
                    if (plugin.isDebugMode()) {
                        plugin.debug("Loaded custom icon for achievement " + achievementId + ": " + material.name());
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material for achievement icon: " + materialName);
                }
            }
        }

        // Debug output of all loaded icons
        if (plugin.isDebugMode()) {
            plugin.debug("Loaded achievement icons:");
            for (Map.Entry<String, Material> entry : achievementIcons.entrySet()) {
                plugin.debug("  " + entry.getKey() + ": " + entry.getValue().name());
            }
        }
    }

    /**
     * Open the achievements GUI for a player
     * @param player The player
     */
    public void openAchievementGUI(Player player) {
        if (!plugin.getAchievementManager().isEnabled()) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.achievements.disabled"));
            return;
        }

        String title = ChatColor.GOLD + "VeinMiner Achievements";
        Inventory inventory = Bukkit.createInventory(null, 54, title);

        // Get player achievements
        Map<String, Integer> playerAchievements = plugin.getAchievementManager().getPlayerAchievements(player);
        Map<String, Map<String, Object>> achievementDefinitions = plugin.getAchievementManager().getAchievementDefinitions();

        if (achievementDefinitions.isEmpty()) {
            // No achievements defined
            ItemStack noAchievementsItem = createItem(
                    Material.BARRIER,
                    ChatColor.translateAlternateColorCodes('&', plugin.getMessageManager().getMessage("gui.achievements-none-title", "&cNo Achievements Available")),
                    ChatColor.translateAlternateColorCodes('&', plugin.getMessageManager().getMessage("gui.achievements-none-lore", "&7No achievements have been defined."))
            );
            inventory.setItem(22, noAchievementsItem);
        } else {
            int slot = 0;
            plugin.getLogger().info("Placing " + achievementDefinitions.size() + " achievements in GUI");

            // Sort achievements by type for better organization
            Map<String, List<Map.Entry<String, Map<String, Object>>>> groupedAchievements = new HashMap<>();

            // Group achievements by type
            for (Map.Entry<String, Map<String, Object>> entry : achievementDefinitions.entrySet()) {
                String type = (String) entry.getValue().get("type");
                if (!groupedAchievements.containsKey(type)) {
                    groupedAchievements.put(type, new ArrayList<>());
                }
                groupedAchievements.get(type).add(entry);
            }

            // Process each type in a specific order
            List<String> typeOrder = Arrays.asList("BLOCK_MINE", "TOTAL_BLOCKS", "LEVEL", "SKILL_MASTER");
            for (String type : typeOrder) {
                if (groupedAchievements.containsKey(type)) {
                    for (Map.Entry<String, Map<String, Object>> entry : groupedAchievements.get(type)) {
                        String achievementId = entry.getKey();
                        Map<String, Object> achievementData = entry.getValue();

                        String name = (String) achievementData.get("name");
                        String description = (String) achievementData.get("description");
                        int requiredAmount = (int) achievementData.get("amount");

                        // Get player progress
                        int progress = playerAchievements.getOrDefault(achievementId, 0);
                        boolean completed = progress >= requiredAmount;

                        // Get icon
                        Material iconMaterial = achievementIcons.getOrDefault(achievementId,
                                achievementIcons.getOrDefault(type, Material.PAPER));

                        // Check if the achievement has been claimed
                        boolean claimed = plugin.getAchievementManager().hasClaimedAchievement(player, achievementId);

                        // Create item
                        ItemStack achievementItem = createAchievementItem(
                                iconMaterial,
                                name,
                                description,
                                progress,
                                requiredAmount,
                                completed,
                                claimed,
                                achievementData,
                                achievementId  // Pass the achievement ID
                        );

                        inventory.setItem(slot++, achievementItem);

                        // Only show 45 achievements max (9x5)
                        if (slot >= 45) {
                            plugin.getLogger().info("Reached max displayable achievements (45)");
                            break;
                        }
                    }

                    if (slot >= 45) break;
                }
            }

            // Add any remaining achievements that weren't in the ordered types
            for (Map.Entry<String, List<Map.Entry<String, Map<String, Object>>>> group : groupedAchievements.entrySet()) {
                if (!typeOrder.contains(group.getKey())) {
                    for (Map.Entry<String, Map<String, Object>> entry : group.getValue()) {
                        String achievementId = entry.getKey();
                        Map<String, Object> achievementData = entry.getValue();

                        String name = (String) achievementData.get("name");
                        String description = (String) achievementData.get("description");
                        String type = (String) achievementData.get("type");
                        int requiredAmount = (int) achievementData.get("amount");

                        // Get player progress
                        int progress = playerAchievements.getOrDefault(achievementId, 0);
                        boolean completed = progress >= requiredAmount;

                        // Get icon
                        Material iconMaterial = achievementIcons.getOrDefault(achievementId,
                                achievementIcons.getOrDefault(type, Material.PAPER));

                        // Check if the achievement has been claimed
                        boolean claimed = plugin.getAchievementManager().hasClaimedAchievement(player, achievementId);

                        // Create item
                        ItemStack achievementItem = createAchievementItem(
                                iconMaterial,
                                name,
                                description,
                                progress,
                                requiredAmount,
                                completed,
                                claimed,
                                achievementData,
                                achievementId  // Pass the achievement ID
                        );

                        inventory.setItem(slot++, achievementItem);

                        // Only show 45 achievements max (9x5)
                        if (slot >= 45) {
                            break;
                        }
                    }

                    if (slot >= 45) break;
                }
            }

            plugin.getLogger().info("Placed " + slot + " achievements in GUI");
        }

        // Add back button
        ItemStack backButton = createItem(
                Material.OAK_DOOR,
                ChatColor.GREEN + "Back to Main Menu",
                ChatColor.GRAY + "Return to the main menu"
        );
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
     * @param event The inventory click event
     */
    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItemStack = event.getCurrentItem();

        if (clickedItemStack == null || clickedItemStack.getType() == Material.AIR) {
            return;
        }

        // Check if clicked on back button
        if (event.getSlot() == 49 && clickedItemStack.getType() == Material.OAK_DOOR) {
            player.closeInventory();
            plugin.getMainGUI().openMainGUI(player);
            return;
        }

        // Check if clicked on an achievement
        ItemMeta meta = clickedItemStack.getItemMeta();
        if (meta != null && meta.hasLore() && !meta.getLore().isEmpty()) {
            List<String> lore = meta.getLore();
            // Extract achievement ID from the first lore line
            String firstLine = lore.get(0);
            if (firstLine.startsWith(ChatColor.BLACK + "ID:")) {
                String achievementId = firstLine.substring((ChatColor.BLACK + "ID:").length());
                // Try to claim the achievement
                boolean claimed = plugin.getAchievementManager().claimAchievementRewards(player, achievementId);
                if (claimed) {
                    // Refresh the GUI
                    player.closeInventory();
                    openAchievementGUI(player);
                }
            }
        }
    }

    /**
     * Create an achievement item for the GUI
     * @param material The material
     * @param name The achievement name
     * @param description The achievement description
     * @param progress The player's progress
     * @param requiredAmount The required amount to complete
     * @param completed Whether the achievement is completed
     * @param achievementData Additional achievement data
     * @return The item
     */
    private ItemStack createAchievementItem(Material material, String name, String description,
                                            int progress, int requiredAmount, boolean completed,
                                            boolean claimed, Map<String, Object> achievementData,
                                            String achievementId) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();

        // Store the achievement ID as a persistent data tag (only in 1.14+)
        // We'll use lore parsing as a fallback in our click handler

        // Set name
        meta.setDisplayName((completed ? ChatColor.GREEN : ChatColor.RED) + name);

        // Create lore
        List<String> lore = new ArrayList<>();
        // First line is hidden achievement ID for identification when clicked
        lore.add(ChatColor.BLACK + "ID:" + achievementId);
        lore.add(ChatColor.GRAY + description);
        lore.add("");

        // Add progress
        int displayProgress = completed ? requiredAmount : progress; // Cap progress at required amount if completed
        String progressText = ChatColor.YELLOW + plugin.getMessageManager().getMessage("gui.achievements-progress", "Progress: ") +
                (completed ? ChatColor.GREEN : ChatColor.RED) +
                displayProgress + ChatColor.GRAY + "/" + ChatColor.GREEN + requiredAmount;
        lore.add(progressText);

        // Add completion status
        lore.add(completed ?
                ChatColor.GREEN + plugin.getMessageManager().getMessage("gui.achievements-completed", "✓ Completed!") :
                ChatColor.RED + plugin.getMessageManager().getMessage("gui.achievements-not-completed", "✗ Not completed"));

        // Always add rewards section
        lore.add("");
        lore.add(ChatColor.GOLD + plugin.getMessageManager().getMessage("gui.achievements-rewards", "Rewards:"));

        if (achievementData.containsKey("rewards")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rewards = (Map<String, Object>) achievementData.get("rewards");

            if (rewards.containsKey("money")) {
                double money = (double) rewards.get("money");
                lore.add(ChatColor.YELLOW + "• " + money + " " + plugin.getMessageManager().getMessage("gui.achievements-money", "money"));
            }

            if (rewards.containsKey("items")) {
                @SuppressWarnings("unchecked")
                List<String> items = (List<String>) rewards.get("items");
                for (String itemStr : items) {
                    String[] parts = itemStr.split(":");
                    String itemName = parts[0];
                    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    lore.add(ChatColor.YELLOW + "• " + amount + "x " + formatItemName(itemName));
                }
            }
        }

        // Add claim button for completed achievements that haven't been claimed yet
        if (completed && !claimed) {
            lore.add("");
            lore.add(ChatColor.GREEN + "» " + ChatColor.BOLD + plugin.getMessageManager().getMessage("gui.achievements-claim", "Click to claim rewards") + ChatColor.GREEN + " «");
        } else if (completed && claimed) {
            lore.add("");
            lore.add(ChatColor.GRAY + "✓ " + plugin.getMessageManager().getMessage("gui.achievements-claimed", "Rewards claimed"));
        }

        meta.setLore(lore);
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    /**
     * Format an item name to be more readable
     * @param itemName The item name
     * @return The formatted item name
     */
    private String formatItemName(String itemName) {
        String[] words = itemName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Create a simple item for the GUI
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
     * Create a simple item for the GUI
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
     * Get the icon material for an achievement type
     * @param type The achievement type
     * @return The icon material
     */
    public Material getTypeIcon(String type) {
        return achievementIcons.getOrDefault(type, Material.PAPER);
    }
}