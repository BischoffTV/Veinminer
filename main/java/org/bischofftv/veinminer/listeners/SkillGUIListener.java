package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.skills.SkillManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class SkillGUIListener implements Listener {

    private final Veinminer plugin;

    public SkillGUIListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "VeinMiner Skills")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            // Check if clicked on a skill
            if (event.getSlot() == 11) {
                // Efficiency Boost
                handleSkillUpgrade(player, SkillManager.SkillType.EFFICIENCY);
            } else if (event.getSlot() == 13) {
                // Luck Enhancement
                handleSkillUpgrade(player, SkillManager.SkillType.LUCK);
            } else if (event.getSlot() == 15) {
                // Energy Conservation
                handleSkillUpgrade(player, SkillManager.SkillType.ENERGY);
            } else if (event.getSlot() == 21) {
                // Reset Skills
                handleSkillReset(player);
            } else if (event.getSlot() == 23) {
                // Back to Main Menu
                player.closeInventory();
                plugin.getMainGUI().openMainGUI(player);
            }
        }
    }

    /**
     * Handle skill upgrade
     * @param player The player
     * @param skillType The skill type
     */
    private void handleSkillUpgrade(Player player, SkillManager.SkillType skillType) {
        boolean success = plugin.getSkillManager().upgradeSkill(player, skillType);

        if (success) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.upgraded"));
            // Reopen the skill GUI to show the updated values
            plugin.getSkillGUI().openSkillGUI(player);
        } else {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.upgrade-failed"));
        }
    }

    /**
     * Handle skill reset
     * @param player The player
     */
    private void handleSkillReset(Player player) {
        boolean success = plugin.getSkillManager().resetSkills(player);

        if (success) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.reset"));
            // Reopen the skill GUI to show the updated values
            plugin.getSkillGUI().openSkillGUI(player);
        } else {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.reset-failed"));
        }
    }
}