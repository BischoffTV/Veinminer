package org.bischofftv.veinminer.utils;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.atomic.AtomicInteger;

public class VeinMinerUtils {

    private final Veinminer plugin;
    private final AtomicInteger blocksMinedToday = new AtomicInteger(0);
    private long lastResetTime = System.currentTimeMillis();

    public VeinMinerUtils(Veinminer plugin) {
        this.plugin = plugin;
    }

    /**
     * Increment the blocks mined counter
     * @param count The number of blocks to add
     */
    public void incrementBlocksMined(int count) {
        // Check if we need to reset the counter (once per day)
        checkAndResetCounter();

        // Increment the counter
        blocksMinedToday.addAndGet(count);

        if (plugin.isDebugMode()) {
            plugin.debug("Incremented blocks mined counter by " + count + ". New total: " + blocksMinedToday.get());
        }
    }

    /**
     * Get the total blocks mined today
     * @return The total blocks mined today
     */
    public int getTotalBlocksMinedToday() {
        // Check if we need to reset the counter
        checkAndResetCounter();

        return blocksMinedToday.get();
    }

    /**
     * Check if we need to reset the counter (once per day)
     */
    private void checkAndResetCounter() {
        long currentTime = System.currentTimeMillis();
        long oneDayInMillis = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

        if (currentTime - lastResetTime > oneDayInMillis) {
            blocksMinedToday.set(0);
            lastResetTime = currentTime;

            if (plugin.isDebugMode()) {
                plugin.debug("Reset blocks mined counter for new day");
            }
        }
    }

    /**
     * Add an enchantment to an item using the Registry system
     * @param item The item to enchant
     * @param enchantmentName The name of the enchantment (e.g., "unbreaking")
     * @param level The level of the enchantment
     * @param ignoreLevelRestriction Whether to ignore level restrictions
     * @return True if the enchantment was added, false otherwise
     */
    public boolean addEnchantment(ItemStack item, String enchantmentName, int level, boolean ignoreLevelRestriction) {
        if (item == null) return false;

        Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantmentName));
        if (enchantment == null) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        boolean result = meta.addEnchant(enchantment, level, ignoreLevelRestriction);
        item.setItemMeta(meta);
        return result;
    }

    /**
     * Get the level of an enchantment on an item using the Registry system
     * @param item The item to check
     * @param enchantmentName The name of the enchantment (e.g., "unbreaking")
     * @return The level of the enchantment, or 0 if not present
     */
    public int getEnchantmentLevel(ItemStack item, String enchantmentName) {
        if (item == null) return 0;

        Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantmentName));
        if (enchantment == null) return 0;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        return meta.getEnchantLevel(enchantment);
    }

    /**
     * Check if an item has an enchantment using the Registry system
     * @param item The item to check
     * @param enchantmentName The name of the enchantment (e.g., "unbreaking")
     * @return True if the item has the enchantment, false otherwise
     */
    public boolean hasEnchantment(ItemStack item, String enchantmentName) {
        if (item == null) return false;

        Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantmentName));
        if (enchantment == null) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.hasEnchant(enchantment);
    }

    /**
     * Calculate the damage to apply to a tool based on the unbreaking enchantment
     * @param tool The tool to calculate damage for
     * @param damage The base damage to apply
     * @return The actual damage to apply
     */
    public int calculateToolDamage(ItemStack tool, int damage) {
        if (tool == null || tool.getType() == Material.AIR) return 0;

        int unbreakingLevel = getEnchantmentLevel(tool, "unbreaking");
        if (unbreakingLevel <= 0) return damage;

        // Apply unbreaking formula: 100/(unbreaking+1)% chance to take damage
        int actualDamage = 0;
        for (int i = 0; i < damage; i++) {
            if (Math.random() * 100 < (100.0 / (unbreakingLevel + 1))) {
                actualDamage++;
            }
        }

        return actualDamage;
    }
}