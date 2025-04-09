package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SkillGUIListener implements Listener {

    private final Veinminer plugin;

    public SkillGUIListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.GOLD + "VeinMiner Skills")) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Back button
        if (slot == 27) {
            player.closeInventory();
            plugin.getMainGUI().openMainGUI(player);
            return;
        }

        // Efficiency Boost
        if (slot == 11) {
            if (plugin.getSkillManager().upgradeSkill(player, "efficiency")) {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.upgraded"));
                player.closeInventory();
                plugin.getSkillGUI().openSkillGUI(player);
            } else {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.upgrade-failed"));
            }
            return;
        }

        // Luck Enhancement
        if (slot == 13) {
            if (plugin.getSkillManager().upgradeSkill(player, "luck")) {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.upgraded"));
                player.closeInventory();
                plugin.getSkillGUI().openSkillGUI(player);
            } else {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.upgrade-failed"));
            }
            return;
        }

        // Energy Conservation
        if (slot == 15) {
            if (plugin.getSkillManager().upgradeSkill(player, "energy")) {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.upgraded"));
                player.closeInventory();
                plugin.getSkillGUI().openSkillGUI(player);
            } else {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.upgrade-failed"));
            }
            return;
        }

        // Reset Skills
        if (slot == 31) {
            if (plugin.getSkillManager().resetSkills(player)) {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.reset"));
                player.closeInventory();
                plugin.getSkillGUI().openSkillGUI(player);
            } else {
                player.sendMessage(plugin.getMessageManager().formatMessage("messages.skills.reset-failed"));
            }
        }
    }
}