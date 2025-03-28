package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIListener implements Listener {

    private final Veinminer plugin;

    public GUIListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    // Fix the issue with items being removable from the GUI
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // Check if it's our GUI
        if (title.equals(ChatColor.GREEN + "VeinMiner Menu")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            // Toggle VeinMiner
            if (event.getSlot() == 10) {
                boolean enabled = plugin.getPlayerDataManager().isVeinMinerEnabled(player);
                plugin.getPlayerDataManager().setVeinMinerEnabled(player, !enabled);

                // Send message from lang.yml
                if (enabled) {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.disabled"));
                } else {
                    player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.enabled"));
                }

                player.closeInventory();
                plugin.getMainGUI().openMainGUI(player);
            }

            // Tools Settings
            else if (event.getSlot() == 12) {
                player.closeInventory();
                openToolsGUI(player);
            }

            // Level Information - only if level system is enabled
            else if (event.getSlot() == 14 && plugin.getLevelManager().isEnabled()) {
                player.closeInventory();

                // Get player level data
                int level = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getLevel();
                int experience = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getExperience();
                int nextLevelExperience = plugin.getLevelManager().getXpForNextLevel(level);

                // Send level info message from lang.yml
                String message = plugin.getMessageManager().formatMessage("messages.level.info")
                        .replace("%level%", String.valueOf(level))
                        .replace("%experience%", String.valueOf(experience))
                        .replace("%next_level_experience%", String.valueOf(nextLevelExperience));

                player.sendMessage(message);
            }

            // Skills - only if skill system is enabled
            else if (event.getSlot() == 16 && plugin.getSkillManager().isEnabled()) {
                player.closeInventory();
                plugin.getSkillGUI().openSkillGUI(player);
            }

            // Achievements - only if achievement system is enabled
            else if (event.getSlot() == 19 && plugin.getAchievementManager().isEnabled()) {
                player.closeInventory();
                plugin.getAchievementGUI().openAchievementGUI(player);
            }

            // Top Players - only if enabled in config
            else if (event.getSlot() == 21 && plugin.getConfig().getBoolean("gui.show-top-players", true)) {
                player.closeInventory();
                plugin.getTopPlayersGUI().openTopPlayersGUI(player);
            }

            // About
            else if (event.getSlot() == 23) {
                player.closeInventory();
                player.performCommand("veinminerabout");
            }

            // Help
            else if (event.getSlot() == 25) {
                player.closeInventory();
                player.performCommand("veinminerhelp");
            }
        }
        // Check if it's the achievements GUI
        else if (title.equals(ChatColor.GOLD + "VeinMiner Achievements")) {
            event.setCancelled(true);
            plugin.getAchievementGUI().handleInventoryClick(event);
        }
        // Check if it's the tools GUI
        else if (title.equals(ChatColor.AQUA + "VeinMiner Tools")) {
            event.setCancelled(true);
            handleToolsGUIClick(event);
        }
    }

    /**
     * Open the tools GUI for a player
     * @param player The player
     */
    private void openToolsGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.AQUA + "VeinMiner Tools");

        // Get player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            plugin.getLogger().warning("Failed to load player data for " + player.getName());
            player.sendMessage(ChatColor.RED + "Failed to load your player data. Please try again later.");
            return;
        }

        // Pickaxe
        boolean pickaxeEnabled = playerData.isToolEnabled("pickaxe");
        inventory.setItem(10, createToolItem(Material.DIAMOND_PICKAXE, "Pickaxe", pickaxeEnabled));

        // Axe
        boolean axeEnabled = playerData.isToolEnabled("axe");
        inventory.setItem(12, createToolItem(Material.DIAMOND_AXE, "Axe", axeEnabled));

        // Shovel
        boolean shovelEnabled = playerData.isToolEnabled("shovel");
        inventory.setItem(14, createToolItem(Material.DIAMOND_SHOVEL, "Shovel", shovelEnabled));

        // Hoe
        boolean hoeEnabled = playerData.isToolEnabled("hoe");
        inventory.setItem(16, createToolItem(Material.DIAMOND_HOE, "Hoe", hoeEnabled));

        // Back button
        inventory.setItem(22, createItem(Material.OAK_DOOR, ChatColor.GREEN + "Back to Main Menu",
                ChatColor.GRAY + "Return to the main menu"));

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
     * Create a tool item for the tools GUI
     * @param material The material
     * @param toolName The tool name
     * @param enabled Whether the tool is enabled
     * @return The item
     */
    private ItemStack createToolItem(Material material, String toolName, boolean enabled) {
        if (enabled) {
            return createItem(material,
                    ChatColor.GREEN + toolName + ": Enabled",
                    ChatColor.GRAY + "Click to disable for " + toolName);
        } else {
            return createItem(material,
                    ChatColor.RED + toolName + ": Disabled",
                    ChatColor.GRAY + "Click to enable for " + toolName);
        }
    }

    /**
     * Handle clicks in the tools GUI
     * @param event The inventory click event
     */
    private void handleToolsGUIClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Back button
        if (event.getSlot() == 22 && clickedItem.getType() == Material.OAK_DOOR) {
            player.closeInventory();
            plugin.getMainGUI().openMainGUI(player);
            return;
        }

        // Tool toggle
        String toolType = null;
        switch (event.getSlot()) {
            case 10: toolType = "pickaxe"; break;
            case 12: toolType = "axe"; break;
            case 14: toolType = "shovel"; break;
            case 16: toolType = "hoe"; break;
        }

        if (toolType != null) {
            boolean enabled = plugin.getPlayerDataManager().isToolEnabled(player, toolType);
            plugin.getPlayerDataManager().setToolEnabled(player, toolType, !enabled);

            // Send message
            if (enabled) {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.tool-disabled", "%tool%", toolType));
            } else {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.toggle.tool-enabled", "%tool%", toolType));
            }

            // Reopen the GUI
            player.closeInventory();
            openToolsGUI(player);
        }
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