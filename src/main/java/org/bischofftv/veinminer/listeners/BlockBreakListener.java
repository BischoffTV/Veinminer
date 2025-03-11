package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.utils.VeinMiningUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BlockBreakListener implements Listener {

    private final Veinminer plugin;

    public BlockBreakListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if player is sneaking (shift)
        if (!player.isSneaking()) {
            return;
        }

        // Check permission if required
        if (plugin.getConfig().getBoolean("permissions.require-permission", true)) {
            String permission = plugin.getConfig().getString("permissions.use-permission", "veinminer.use");
            if (!player.hasPermission(permission)) {
                return;
            }
        }

        // Check if veinminer is enabled for this player
        if (!plugin.getPlayerDataManager().isVeinMinerEnabled(player)) {
            return;
        }

        // Check if the block is allowed for vein mining
        if (!plugin.getConfigManager().isAllowedBlock(block.getType())) {
            return;
        }

        // Check if player is in creative mode (no need for tool checks)
        if (player.getGameMode() == GameMode.CREATIVE) {
            performVeinMining(player, block, null);
            return;
        }

        // Get the tool the player is using
        ItemStack tool = player.getInventory().getItemInMainHand();

        // Check if the tool is appropriate for the block
        if (!isAppropriateToolForBlock(tool, block)) {
            return;
        }

        // Check if veinminer is enabled for this tool type
        String toolType = getToolType(tool.getType());
        if (toolType != null && !plugin.getPlayerDataManager().isToolEnabled(player, toolType)) {
            return;
        }

        // Perform vein mining
        performVeinMining(player, block, tool);
    }

    private void performVeinMining(Player player, Block startBlock, ItemStack tool) {
        // Get max blocks based on player level or config
        int maxBlocks = plugin.getLevelManager().isEnabled()
                ? plugin.getLevelManager().getMaxBlocks(player)
                : plugin.getConfigManager().getMaxBlocks();

        // Get connected blocks of the same type
        Collection<Block> connectedBlocks = VeinMiningUtils.findConnectedBlocks(
                startBlock,
                maxBlocks,
                plugin.getConfigManager().getAllowedBlocks()
        );

        // Skip if only the original block was found
        if (connectedBlocks.size() <= 1) {
            return;
        }

        // Track mining statistics for logging
        int blocksDestroyed = 0;
        Map<Material, Integer> itemsCollected = new HashMap<>();

        // Get enchantments from tool
        Map<Enchantment, Integer> enchantments = tool != null ? tool.getEnchantments() : new HashMap<>();
        boolean hasSilkTouch = enchantments.containsKey(Enchantment.SILK_TOUCH);
        int fortuneLevel = enchantments.getOrDefault(Enchantment.FORTUNE, 0);

        // Process each connected block
        for (Block block : connectedBlocks) {
            // Skip the original block as it's handled by the normal event
            if (block.equals(startBlock)) {
                continue;
            }

            // Break the block and collect drops
            Collection<ItemStack> drops;
            if (hasSilkTouch) {
                drops = VeinMiningUtils.getSilkTouchDrops(block);
            } else {
                drops = VeinMiningUtils.getFortuneDrops(block, fortuneLevel);
            }

            // Add drops to player's inventory
            for (ItemStack drop : drops) {
                // Track items for logging
                itemsCollected.merge(drop.getType(), drop.getAmount(), Integer::sum);

                // Add to player's inventory or drop on ground if inventory is full
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }

            // Set the block to air
            block.setType(Material.AIR);
            blocksDestroyed++;
        }

        // Apply tool damage if not in creative mode
        if (player.getGameMode() != GameMode.CREATIVE && tool != null) {
            applyToolDamage(player, tool, blocksDestroyed);
        }

        // Apply hunger if configured
        if (plugin.getConfigManager().isUseHungerMultiplier()) {
            applyHunger(player, blocksDestroyed);
        }

        // Update player level data
        if (plugin.getLevelManager().isEnabled()) {
            plugin.getLevelManager().addBlocksMined(player, blocksDestroyed);
        }

        // Log mining activity
        plugin.getMiningLogger().logMiningActivity(player, blocksDestroyed, itemsCollected, enchantments);
    }

    private void applyToolDamage(Player player, ItemStack tool, int blocksDestroyed) {
        if (!(tool.getItemMeta() instanceof Damageable)) {
            return;
        }

        // Calculate damage to apply
        int damage = blocksDestroyed;
        if (plugin.getConfigManager().isUseDurabilityMultiplier()) {
            damage = (int) Math.ceil(damage * plugin.getConfigManager().getDurabilityMultiplier());
        }

        // Apply unbreaking enchantment reduction
        int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreakingLevel > 0) {
            double reduction = 1.0 / (unbreakingLevel + 1);
            damage = (int) Math.ceil(damage * reduction);
        }

        // Apply damage to tool
        Damageable meta = (Damageable) tool.getItemMeta();
        int currentDurability = meta.getDamage();
        int newDurability = currentDurability + damage;

        // Check if tool should break
        if (newDurability >= tool.getType().getMaxDurability()) {
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), "entity.item.break", 1.0f, 1.0f);
        } else {
            meta.setDamage(newDurability);
            tool.setItemMeta(meta);
        }
    }

    private void applyHunger(Player player, int blocksDestroyed) {
        double hungerToApply = blocksDestroyed * plugin.getConfigManager().getHungerMultiplier();
        int foodLevel = player.getFoodLevel();
        int newFoodLevel = Math.max(0, (int) (foodLevel - hungerToApply));
        player.setFoodLevel(newFoodLevel);
    }

    private boolean isAppropriateToolForBlock(ItemStack tool, Block block) {
        // This is a simplified check - in a real plugin you'd want to check
        // if the tool is appropriate for the block type
        Material toolType = tool.getType();
        Material blockType = block.getType();

        // Example check for ores and pickaxes
        if (blockType.name().contains("ORE") || blockType == Material.STONE) {
            return toolType.name().contains("PICKAXE");
        }

        // Example check for wood and axes
        if (blockType.name().contains("LOG") || blockType.name().contains("WOOD")) {
            return toolType.name().contains("AXE");
        }

        // Example check for dirt, sand, gravel and shovels
        if (blockType == Material.DIRT || blockType == Material.SAND || blockType == Material.GRAVEL) {
            return toolType.name().contains("SHOVEL");
        }

        return false;
    }

    private String getToolType(Material material) {
        String name = material.name();
        if (name.contains("PICKAXE")) return "pickaxe";
        if (name.contains("AXE") && !name.contains("PICKAXE")) return "axe";
        if (name.contains("SHOVEL")) return "shovel";
        if (name.contains("HOE")) return "hoe";
        return null;
    }
}