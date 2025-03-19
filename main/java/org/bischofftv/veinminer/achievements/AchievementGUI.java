package org.bischofftv.veinminer.achievements;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.achievements.AchievementManager.Achievement;
import org.bischofftv.veinminer.achievements.AchievementManager.PlayerAchievement;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class AchievementGUI {

    private final Veinminer plugin;
    private final int ITEMS_PER_PAGE = 45;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public AchievementGUI(Veinminer plugin) {
        this.plugin = plugin;
    }

    public void openAchievementsMenu(Player player) {
        if (!plugin.getAchievementManager().isEnabled()) {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.system-disabled"));
            return;
        }

        openAchievementsMenu(player, 0);
    }

    // Update the openAchievementsMenu method to create a more visually appealing GUI
    public void openAchievementsMenu(Player player, int page) {
        if (!plugin.getAchievementManager().isEnabled()) {
            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.system-disabled"));
            return;
        }

        boolean debug = plugin.getConfig().getBoolean("settings.debug", false);

        // Store current page
        playerPages.put(player.getUniqueId(), page);

        // Get player achievements
        Map<String, PlayerAchievement> playerAchievements = plugin.getAchievementManager().getPlayerAchievements(player);
        List<Achievement> achievements = new ArrayList<>(plugin.getAchievementManager().getAchievements());

        if (debug) {
            plugin.debug("Opening achievements menu for " + player.getName());
            plugin.debug("Player achievements loaded: " + (playerAchievements != null));
            plugin.debug("Total configured achievements: " + achievements.size());
            if (playerAchievements != null) {
                plugin.debug("Player achievement entries: " + playerAchievements.size());
                // Log each achievement for debugging
                for (Map.Entry<String, PlayerAchievement> entry : playerAchievements.entrySet()) {
                    PlayerAchievement pa = entry.getValue();
                    plugin.debug("Achievement: " + entry.getKey() +
                            ", Progress: " + pa.getProgress() +
                            ", Claimed: " + pa.isClaimed());
                }
            }
        }

        if (achievements.isEmpty()) {
            plugin.getLogger().warning("No achievements loaded! Check your config.yml");
            player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.none-configured"));
            return;
        }

        if (playerAchievements == null || playerAchievements.isEmpty()) {
            if (debug) {
                plugin.debug("No player achievements found for " + player.getName() +
                        ". Attempting to reload...");
            }
            plugin.getAchievementManager().loadPlayerAchievements(player);

            // Use an array to make the variable effectively final for the lambda
            final Map<String, PlayerAchievement>[] playerAchievementsArray = new Map[1];
            final int finalPage = page;

            // Wait a short time for achievements to load
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                playerAchievementsArray[0] = plugin.getAchievementManager().getPlayerAchievements(player);
                if (playerAchievementsArray[0] == null || playerAchievementsArray[0].isEmpty()) {
                    if (debug) {
                        plugin.debug("Failed to load player achievements for " + player.getName());
                    }
                    player.sendMessage(plugin.getMessageManager().getMessage("messages.achievements.load-failed"));
                    return;
                }
                // Retry opening the menu after loading
                openAchievementsMenu(player, finalPage);
            }, 20L); // Wait 1 second
            return;
        }

        // Create inventory
        String title = plugin.getMessageManager().getMessage("messages.achievements.gui.title");
        Inventory inventory = Bukkit.createInventory(null, 54, title);

        // Fill the inventory with a glass pane border
        ItemStack borderPane = createBorderItem();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderPane); // Top row
            inventory.setItem(45 + i, borderPane); // Bottom row
        }
        for (int i = 0; i < 5; i++) {
            inventory.setItem(9 * i, borderPane); // Left column
            inventory.setItem(9 * i + 8, borderPane); // Right column
        }

        // Add achievement stats item
        inventory.setItem(4, createStatsItem(player, playerAchievements, achievements));

        // Calculate total pages and achievement slots
        int achievementSlots = 28; // 7x4 grid in the middle
        int totalPages = (int) Math.ceil((double) achievements.size() / achievementSlots);
        if (totalPages == 0) totalPages = 1;

        // Validate page number
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        // Sort achievements by type and completion status
        sortAchievements(achievements, playerAchievements);

        // Calculate start and end indices for this page
        int startIndex = page * achievementSlots;
        int endIndex = Math.min(startIndex + achievementSlots, achievements.size());

        if (debug) {
            plugin.debug("Page " + (page + 1) + " of " + totalPages);
            plugin.debug("Displaying achievements from index " + startIndex + " to " + endIndex);
        }

        // Add achievement items to the middle grid
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Achievement achievement = achievements.get(i);
            PlayerAchievement playerAchievement = playerAchievements.get(achievement.getId());

            if (playerAchievement == null) {
                if (debug) {
                    plugin.debug("Missing player achievement data for: " + achievement.getId());
                }
                // Create default achievement data
                playerAchievement = new AchievementManager.PlayerAchievement(achievement.getId(), 0, false);
                playerAchievements.put(achievement.getId(), playerAchievement);
            }

            // Calculate the slot in the middle grid
            int row = slot / 7 + 1; // +1 to skip the top border row
            int col = slot % 7 + 1; // +1 to skip the left border column
            int inventorySlot = row * 9 + col;

            ItemStack item = createAchievementItem(achievement, playerAchievement);
            inventory.setItem(inventorySlot, item);

            if (debug) {
                plugin.debug("Added achievement '" + achievement.getName() +
                        "' to slot " + inventorySlot +
                        " (Progress: " + playerAchievement.getProgress() +
                        "/" + achievement.getAmount() +
                        ", Claimed: " + playerAchievement.isClaimed() + ")");
            }

            slot++;
        }

        // Add navigation items
        if (page > 0) {
            ItemStack prevPage = createNavigationItem(Material.ARROW,
                    plugin.getMessageManager().getMessage("messages.achievements.gui.previous-page"));
            inventory.setItem(47, prevPage);
        }

        if (page < totalPages - 1) {
            ItemStack nextPage = createNavigationItem(Material.ARROW,
                    plugin.getMessageManager().getMessage("messages.achievements.gui.next-page"));
            inventory.setItem(51, nextPage);
        }

        // Add page indicator
        ItemStack pageIndicator = createPageIndicator(page + 1, totalPages);
        inventory.setItem(49, pageIndicator);

        // Add back button
        ItemStack backButton = createNavigationItem(Material.DARK_OAK_DOOR,
                ChatColor.YELLOW + "Back to Main Menu");
        inventory.setItem(45, backButton);

        // Add close button
        ItemStack closeButton = createNavigationItem(Material.BARRIER,
                plugin.getMessageManager().getMessage("messages.achievements.gui.close"));
        inventory.setItem(53, closeButton);

        // Open inventory
        player.openInventory(inventory);
    }

    // Add these new methods to support the improved GUI

    private void sortAchievements(List<Achievement> achievements, Map<String, PlayerAchievement> playerAchievements) {
        achievements.sort((a1, a2) -> {
            PlayerAchievement pa1 = playerAchievements.get(a1.getId());
            PlayerAchievement pa2 = playerAchievements.get(a2.getId());

            if (pa1 == null || pa2 == null) {
                return a1.getName().compareTo(a2.getName());
            }

            // First sort by type
            int typeCompare = a1.getType().compareTo(a2.getType());
            if (typeCompare != 0) {
                return typeCompare;
            }

            // Then by completion status
            boolean a1Completed = pa1.getProgress() >= a1.getAmount();
            boolean a2Completed = pa2.getProgress() >= a2.getAmount();

            if (a1Completed && !a2Completed) return -1;
            if (!a1Completed && a2Completed) return 1;

            // Then by claim status for completed achievements
            if (a1Completed && a2Completed) {
                if (!pa1.isClaimed() && pa2.isClaimed()) return -1;
                if (pa1.isClaimed() && !pa2.isClaimed()) return 1;
            }

            // Then by progress percentage for incomplete achievements
            if (!a1Completed && !a2Completed) {
                double a1Progress = (double) pa1.getProgress() / a1.getAmount();
                double a2Progress = (double) pa2.getProgress() / a2.getAmount();

                if (a1Progress > a2Progress) return -1;
                if (a1Progress < a2Progress) return 1;
            }

            // Finally by name
            return a1.getName().compareTo(a2.getName());
        });
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem(Player player, Map<String, PlayerAchievement> playerAchievements, List<Achievement> achievements) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + player.getName() + "'s " + ChatColor.YELLOW + "Achievement Stats");

        List<String> lore = new ArrayList<>();

        // Count achievements by status
        int total = achievements.size();
        int completed = 0;
        int claimed = 0;
        int inProgress = 0;

        for (Achievement achievement : achievements) {
            PlayerAchievement playerAchievement = playerAchievements.get(achievement.getId());
            if (playerAchievement != null) {
                if (playerAchievement.isClaimed()) {
                    claimed++;
                    completed++;
                } else if (playerAchievement.getProgress() >= achievement.getAmount()) {
                    completed++;
                } else if (playerAchievement.getProgress() > 0) {
                    inProgress++;
                }
            }
        }

        int notStarted = total - completed - inProgress;
        int percentComplete = total > 0 ? (claimed * 100) / total : 0;

        lore.add(ChatColor.WHITE + "Total Achievements: " + ChatColor.YELLOW + total);
        lore.add(ChatColor.WHITE + "Completed & Claimed: " + ChatColor.GREEN + claimed);
        lore.add(ChatColor.WHITE + "Completed (Unclaimed): " + ChatColor.GOLD + (completed - claimed));
        lore.add(ChatColor.WHITE + "In Progress: " + ChatColor.AQUA + inProgress);
        lore.add(ChatColor.WHITE + "Not Started: " + ChatColor.RED + notStarted);
        lore.add("");
        lore.add(ChatColor.WHITE + "Completion: " + ChatColor.YELLOW + percentComplete + "%");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPageIndicator(int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Page " + currentPage + " of " + totalPages);
        item.setItemMeta(meta);
        return item;
    }

    // Update the createAchievementItem method to use more distinctive items
    private ItemStack createAchievementItem(Achievement achievement, PlayerAchievement playerAchievement) {
        ItemStack item;
        List<String> lore = new ArrayList<>();

        // Add achievement type indicator
        String typeDisplay = formatAchievementType(achievement.getType());
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + typeDisplay);

        // Add description
        lore.add(ChatColor.GRAY + achievement.getDescription());
        lore.add("");

        // Determine item type and status based on progress
        if (playerAchievement.isClaimed()) {
            // Completed and claimed
            item = new ItemStack(Material.EMERALD_BLOCK);
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "COMPLETED & CLAIMED");
        } else if (playerAchievement.getProgress() >= achievement.getAmount()) {
            // Completed but not claimed
            item = new ItemStack(Material.GOLD_BLOCK);
            lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "READY TO CLAIM!");
            lore.add(ChatColor.YELLOW + "Click to claim your rewards!");
        } else if (playerAchievement.getProgress() > 0) {
            // In progress
            double progressPercent = (double) playerAchievement.getProgress() / achievement.getAmount() * 100;

            if (progressPercent >= 75) {
                item = new ItemStack(Material.LIME_CONCRETE);
            } else if (progressPercent >= 50) {
                item = new ItemStack(Material.YELLOW_CONCRETE);
            } else if (progressPercent >= 25) {
                item = new ItemStack(Material.ORANGE_CONCRETE);
            } else {
                item = new ItemStack(Material.RED_CONCRETE);
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("progress", String.valueOf(playerAchievement.getProgress()));
            placeholders.put("total", String.valueOf(achievement.getAmount()));
            placeholders.put("percent", String.format("%.1f", progressPercent));

            lore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "IN PROGRESS: " +
                    ChatColor.WHITE + playerAchievement.getProgress() + "/" + achievement.getAmount() +
                    ChatColor.GRAY + " (" + String.format("%.1f", progressPercent) + "%)");
        } else {
            // Not started
            item = new ItemStack(Material.BEDROCK);
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "LOCKED");
            lore.add(ChatColor.GRAY + "Start working on this achievement!");
        }

        // Add rewards info
        lore.add("");
        lore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "REWARDS:");

        if (achievement.getMoney() > 0 && plugin.getAchievementManager().isEconomyEnabled()) {
            lore.add(ChatColor.GOLD + "• " + achievement.getMoney() + " money");
        }

        for (String itemString : achievement.getItems()) {
            String[] parts = itemString.split(":");
            if (parts.length >= 2) {
                try {
                    Material material = Material.valueOf(parts[0]);
                    int amount = Integer.parseInt(parts[1]);
                    lore.add(ChatColor.GOLD + "• " + amount + "x " + formatMaterialName(material.name()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid items
                }
            }
        }

        // Set item meta
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + achievement.getName());
        meta.setLore(lore);

        // Add enchant glow to completed items
        if (playerAchievement.isClaimed() || playerAchievement.getProgress() >= achievement.getAmount()) {
            meta.addEnchant(Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking")), 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String formatAchievementType(String type) {
        switch (type.toUpperCase()) {
            case "BLOCK_MINE":
                return "Block Mining";
            case "TOTAL_BLOCKS":
                return "Total Blocks";
            case "LEVEL":
                return "Level Achievement";
            default:
                return type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase().replace("_", " ");
        }
    }

    private ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private String formatMaterialName(String name) {
        // Make sure to handle null or empty names
        if (name == null || name.isEmpty()) {
            return "Unknown";
        }
        String[] words = name.toLowerCase().split("_");
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

    // Update the handleInventoryClick method to match the new layout
    public void handleInventoryClick(Player player, int slot, Inventory inventory) {
        final UUID uuid = player.getUniqueId();
        final int currentPage = playerPages.getOrDefault(uuid, 0);
        final boolean debug = plugin.getConfig().getBoolean("settings.debug", false);

        // Check if it's a navigation item
        if (slot == 45) {
            // Back button
            player.closeInventory();
            plugin.getMainGUI().openMainMenu(player);
            return;
        } else if (slot == 47) {
            // Previous page
            openAchievementsMenu(player, currentPage - 1);
            return;
        } else if (slot == 51) {
            // Next page
            openAchievementsMenu(player, currentPage + 1);
            return;
        } else if (slot == 53) {
            // Close
            player.closeInventory();
            return;
        }

        // Check if it's an achievement item (in the middle grid)
        if (isAchievementSlot(slot)) {
            // Convert GUI slot to achievement index
            int gridRow = slot / 9 - 1; // -1 to account for top border
            int gridCol = slot % 9 - 1; // -1 to account for left border
            int gridIndex = gridRow * 7 + gridCol;
            int achievementIndex = currentPage * 28 + gridIndex; // 28 is the number of achievement slots per page

            // Use arrays to make variables effectively final for the lambda
            final List<Achievement>[] achievementsArray = new List[1];
            final Map<String, PlayerAchievement>[] playerAchievementsArray = new Map[1];

            achievementsArray[0] = new ArrayList<>(plugin.getAchievementManager().getAchievements());
            playerAchievementsArray[0] = plugin.getAchievementManager().getPlayerAchievements(player);

            if (debug) {
                plugin.debug("[Debug] Player clicked on achievement slot " + slot +
                        ", grid index " + gridIndex +
                        ", achievement index " + achievementIndex);
                plugin.debug("[Debug] Total achievements: " + achievementsArray[0].size());
                plugin.debug("[Debug] Player achievements: " + (playerAchievementsArray[0] != null ? playerAchievementsArray[0].size() : "null"));
            }

            // Sort achievements the same way as in openAchievementsMenu
            sortAchievements(achievementsArray[0], playerAchievementsArray[0]);

            if (achievementIndex >= 0 && achievementIndex < achievementsArray[0].size()) {
                Achievement achievement = achievementsArray[0].get(achievementIndex);

                if (debug) {
                    plugin.debug("[Debug] Selected achievement: " + achievement.getId() + " - " + achievement.getName());

                    PlayerAchievement pa = playerAchievementsArray[0].get(achievement.getId());
                    if (pa != null) {
                        plugin.debug("[Debug] Achievement progress: " + pa.getProgress() + "/" + achievement.getAmount() +
                                ", Claimed: " + pa.isClaimed());
                    } else {
                        plugin.debug("[Debug] No player achievement data found for this achievement");
                    }
                }

                // Try to claim the achievement
                boolean claimed = plugin.getAchievementManager().claimAchievement(player, achievement.getId());

                if (debug) {
                    plugin.debug("[Debug] Claim result: " + claimed);
                }

                if (claimed) {
                    // Refresh the GUI
                    openAchievementsMenu(player, currentPage);
                }
            }
        }
    }

    private boolean isAchievementSlot(int slot) {
        // Check if the slot is in the middle grid (not in the border)
        int row = slot / 9;
        int col = slot % 9;

        return row >= 1 && row <= 4 && col >= 1 && col <= 7;
    }
}