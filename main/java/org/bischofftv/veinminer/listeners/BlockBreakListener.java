package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.utils.VeinMiningUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BlockBreakListener implements Listener {

    private final Veinminer plugin;

    public BlockBreakListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        boolean debug = plugin.getConfig().getBoolean("settings.debug", false);

        // Debug-Ausgabe hinzufügen
        boolean isShifting = player.isSneaking();
        boolean isEnabled = plugin.getPlayerDataManager().isVeinMinerEnabled(player);
        boolean isAllowedBlock = plugin.getConfigManager().isAllowedBlock(block.getType());

        if (debug) {
            plugin.debug("[Debug] VeinMiner Check for " + player.getName() + ":");
            plugin.debug("[Debug] - Is Shifting: " + isShifting);
            plugin.debug("[Debug] - VeinMiner Enabled: " + isEnabled);
            plugin.debug("[Debug] - Block Type: " + block.getType().name());
            plugin.debug("[Debug] - Block Allowed: " + isAllowedBlock);
        }

        // Check if player is sneaking (shift)
        if (!isShifting) {
            if (debug) plugin.debug("[Debug] - Player is not sneaking, skipping vein mining");
            return;
        }

        // Check permission if required
        if (plugin.getConfig().getBoolean("permissions.require-permission", true)) {
            String permission = plugin.getConfig().getString("permissions.use-permission", "veinminer.use");
            if (!player.hasPermission(permission)) {
                if (debug) {
                    plugin.debug("[Debug] - Permission Check Failed: " + permission);
                }
                return;
            }
        }

        // Check if veinminer is enabled for this player
        if (!isEnabled) {
            if (debug) plugin.debug("[Debug] - VeinMiner is disabled for this player");
            return;
        }

        // Check if the block is allowed for vein mining
        if (!isAllowedBlock) {
            if (debug) plugin.debug("[Debug] - Block type is not allowed for vein mining: " + block.getType().name());
            return;
        }

        // Check if player is in creative mode (no need for tool checks)
        if (player.getGameMode() == GameMode.CREATIVE) {
            if (debug) plugin.debug("[Debug] - Player is in creative mode, skipping tool checks");
            performVeinMining(player, block, null);
            return;
        }

        // Get the tool the player is using
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (debug) {
            plugin.debug("[Debug] - Player tool: " + (tool != null ? tool.getType().name() : "null"));
        }

        // Check if the tool is appropriate for the block
        if (!isAppropriateToolForBlock(tool, block)) {
            if (debug) {
                plugin.debug("[Debug] - Tool not appropriate for block: " +
                        (tool != null ? tool.getType().name() : "null") + " for " + block.getType().name());
            }
            return;
        }

        // Check if veinminer is enabled for this tool type
        String toolType = getToolType(tool.getType());
        if (toolType != null && !plugin.getPlayerDataManager().isToolEnabled(player, toolType)) {
            if (debug) {
                plugin.debug("[Debug] - Tool type disabled for player: " + toolType);
            }
            return;
        }

        // Perform vein mining
        if (debug) plugin.debug("[Debug] - All checks passed, performing vein mining");
        performVeinMining(player, block, tool);
    }

    private void performVeinMining(Player player, Block startBlock, ItemStack tool) {
        boolean debug = plugin.getConfig().getBoolean("settings.debug", false);

        if (debug) {
            plugin.debug("[Debug] Starting vein mining for " + player.getName());
            plugin.debug("[Debug] Start block: " + startBlock.getType().name());
            plugin.debug("[Debug] Tool: " + (tool != null ? tool.getType().name() : "null"));
        }

        // Get max blocks based on player level or config
        int maxBlocks = plugin.getLevelManager().isEnabled()
                ? plugin.getLevelManager().getMaxBlocks(player)
                : plugin.getConfigManager().getMaxBlocks();

        if (debug) {
            plugin.debug("[Debug] Max blocks allowed: " + maxBlocks);
        }

        // Get allowed blocks from config
        Set<Material> allowedBlocks = plugin.getConfigManager().getAllowedBlocks();
        if (debug) {
            plugin.debug("[Debug] Allowed blocks count: " + allowedBlocks.size());
            plugin.debug("[Debug] Checking if " + startBlock.getType().name() + " is in allowed blocks: " +
                    allowedBlocks.contains(startBlock.getType()));
        }

        // Get connected blocks of the same type
        Collection<Block> connectedBlocks = VeinMiningUtils.findConnectedBlocks(
                startBlock,
                maxBlocks,
                allowedBlocks
        );

        if (debug) {
            plugin.debug("[Debug] Found " + connectedBlocks.size() + " connected blocks");
        }

        // Skip if only the original block was found
        if (connectedBlocks.size() <= 1) {
            if (debug) {
                plugin.debug("[Debug] Not enough connected blocks found, skipping vein mining");
            }
            return;
        }

        // Track mining statistics for logging
        int blocksDestroyed = 0;
        Map<Material, Integer> itemsCollected = new HashMap<>();
        Map<Material, Integer> blocksMined = new HashMap<>(); // Track blocks by type for achievements

        // Get enchantments from tool
        Map<Enchantment, Integer> enchantments = tool != null ? tool.getEnchantments() : new HashMap<>();

        // Use Registry to get enchantments
        Enchantment silkTouchEnchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("silk_touch"));
        Enchantment fortuneEnchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("fortune"));

        boolean hasSilkTouch = tool != null && tool.getEnchantmentLevel(silkTouchEnchant) > 0;
        int fortuneLevel = tool != null ? tool.getEnchantmentLevel(fortuneEnchant) : 0;

        // Process each connected block
        for (Block block : connectedBlocks) {
            // Skip the original block as it's handled by the normal event
            if (block.equals(startBlock)) {
                continue;
            }

            try {
                // Track block type for achievements
                Material blockType = block.getType();
                blocksMined.merge(blockType, 1, Integer::sum);

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

                if (debug) {
                    plugin.debug("[Debug] Successfully mined block: " + blockType.name());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error while processing block during vein mining: " + e.getMessage());
                if (debug) {
                    e.printStackTrace();
                }
            }
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

        // Update achievements for each block type mined
        if (plugin.getAchievementManager().isEnabled()) {
            // Update total blocks mined achievement
            plugin.getAchievementManager().updateTotalBlocksProgress(player, blocksDestroyed);

            // Update block-specific achievements
            for (Map.Entry<Material, Integer> entry : blocksMined.entrySet()) {
                plugin.getAchievementManager().updateBlockMineProgress(player, entry.getKey(), entry.getValue());

                if (debug) {
                    plugin.debug("[Debug] Updating achievement progress for " + player.getName() +
                            " - Block: " + entry.getKey().name() + ", Amount: " + entry.getValue());
                }
            }

            // Wichtig: Sofort speichern, um Datenverlust zu vermeiden
            plugin.getAchievementManager().savePlayerAchievements(player);

            // Erzwinge Synchronisierung mit anderen Servern
            plugin.forceSyncDataNow();
        }

        // Log mining activity
        plugin.getMiningLogger().logMiningActivity(player, blocksDestroyed, itemsCollected, enchantments);

        // Update bStats counter
        if (plugin.getVeinMinerUtils() != null) {
            plugin.getVeinMinerUtils().incrementBlocksMined(blocksDestroyed);
        }

        if (debug) {
            plugin.debug("[Debug] Vein mining completed for " + player.getName() +
                    ". Blocks destroyed: " + blocksDestroyed);
        }
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
        Enchantment unbreakingEnchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
        int unbreakingLevel = tool.getEnchantmentLevel(unbreakingEnchant);

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
        boolean debug = plugin.getConfig().getBoolean("settings.debug", false);

        if (tool == null || tool.getType() == Material.AIR) {
            if (debug) {
                plugin.debug("[Debug] Tool is null or AIR");
            }
            return false;
        }

        Material toolType = tool.getType();
        Material blockType = block.getType();

        if (debug) {
            plugin.debug("[Debug] Checking tool compatibility: " + toolType.name() + " for " + blockType.name());
        }

        // Example check for ores and pickaxes
        if (blockType.name().contains("ORE") || blockType == Material.STONE ||
                blockType.name().contains("DEEPSLATE") || blockType.name().contains("COPPER") ||
                blockType.name().contains("IRON") || blockType.name().contains("GOLD") ||
                blockType.name().contains("DIAMOND") || blockType.name().contains("EMERALD") ||
                blockType.name().contains("LAPIS") || blockType.name().contains("REDSTONE") ||
                blockType.name().contains("QUARTZ") || blockType.name().contains("DEBRIS")) {
            return toolType.name().contains("PICKAXE");
        }

        // Example check for wood and axes
        if (blockType.name().contains("LOG") || blockType.name().contains("WOOD") ||
                blockType.name().contains("STEM") || blockType.name().contains("HYPHAE") ||
                blockType.name().contains("PLANKS")) {
            return toolType.name().contains("AXE");
        }

        // Example check for dirt, sand, gravel and shovels
        if (blockType == Material.DIRT || blockType == Material.SAND ||
                blockType == Material.GRAVEL || blockType == Material.CLAY ||
                blockType == Material.SOUL_SAND || blockType == Material.SOUL_SOIL ||
                blockType.name().contains("POWDER") || blockType.name().contains("SNOW")) {
            return toolType.name().contains("SHOVEL");
        }

        // Check for crops and hoes
        if (blockType.name().contains("CROP") || blockType == Material.WHEAT ||
                blockType == Material.CARROTS || blockType == Material.POTATOES ||
                blockType == Material.BEETROOTS || blockType == Material.NETHER_WART ||
                blockType == Material.MELON || blockType == Material.PUMPKIN) {
            return toolType.name().contains("HOE");
        }

        if (debug) {
            plugin.debug("[Debug] Block type not recognized for tool matching: " + blockType.name());
        }

        return false;
    }

    private String getToolType(Material material) {
        if (material == null) {
            return null;
        }

        String name = material.name();
        if (name.contains("PICKAXE")) return "pickaxe";
        if (name.contains("AXE") && !name.contains("PICKAXE")) return "axe";
        if (name.contains("SHOVEL")) return "shovel";
        if (name.contains("HOE")) return "hoe";
        return null;
    }
}