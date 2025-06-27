package org.bischofftv.veinminer.listeners;

import org.bischofftv.veinminer.Veinminer;
import org.bischofftv.veinminer.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockBreakListener implements Listener {

    private final Veinminer plugin;

    public BlockBreakListener(Veinminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if the player has the base permission to use VeinMiner
        if (!hasVeinMinerPermission(player)) {
            return;
        }

        // Get player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null || !playerData.isVeinMinerEnabled()) {
            return;
        }

        // Check if player is sneaking (shift)
        if (!player.isSneaking()) {
            return;
        }

        // Get the tool in the player's hand
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }

        // Determine tool type
        String toolType = getToolType(tool.getType());
        if (toolType == null) {
            return;
        }

        // Check if the player has permission for this specific tool
        if (!hasToolPermission(player, toolType)) {
            player.sendMessage(plugin.getMessageManager().formatMessage("messages.permission.tool-not-allowed", "%tool%", toolType));
            return;
        }

        // Check if the tool is enabled for the player
        if (!playerData.isToolEnabled(toolType)) {
            return;
        }

        // Check if the block is in the allowed blocks list
        List<String> allowedBlocks = plugin.getConfig().getStringList("allowed-blocks");
        if (!allowedBlocks.contains(block.getType().toString())) {
            return;
        }

        // Check WorldGuard region if enabled
        if (plugin.getWorldGuardHook() != null && plugin.getWorldGuardHook().isEnabled()) {
            if (!plugin.getWorldGuardHook().canBreakBlock(player, block.getLocation())) {
                if (plugin.isDebugMode()) {
                    plugin.debug("WorldGuard prevented vein mining at " + block.getLocation());
                }
                return;
            }
        }

        // Get max blocks for player's level
        int maxBlocks = plugin.getLevelManager().isEnabled()
                ? plugin.getLevelManager().getMaxBlocksForLevel(playerData.getLevel())
                : plugin.getConfig().getInt("settings.max-blocks", 64);

        // Start vein mining
        Material targetMaterial = block.getType();
        Set<Block> blocksToBreak = new HashSet<>();
        Set<Block> checkedBlocks = new HashSet<>();

        // Add the initial block
        blocksToBreak.add(block);

        // Find connected blocks of the same type
        findConnectedBlocks(block, targetMaterial, blocksToBreak, checkedBlocks, maxBlocks);

        // Remove the original block as it's already being broken by the event
        blocksToBreak.remove(block);

        if (!blocksToBreak.isEmpty()) {
            // Apply durability and hunger costs
            if (!applyToolDurability(player, tool, blocksToBreak.size(), playerData)) {
                return; // Tool broke
            }

            applyHungerCost(player, blocksToBreak.size(), playerData);

            // Break the blocks
            for (Block blockToBreak : blocksToBreak) {
                // Skip if block type changed (e.g., by another plugin)
                if (blockToBreak.getType() != targetMaterial) {
                    continue;
                }

                // Break the block and drop items
                blockToBreak.breakNaturally(tool);

                // Apply luck enhancement for bonus drops
                if (plugin.getSkillManager().isEnabled()) {
                    int luckLevel = playerData.getLuckLevel();
                    double luckChance = plugin.getSkillManager().getLuckEnhancement(luckLevel);

                    if (Math.random() * 100 < luckChance) {
                        // Drop an extra item
                        blockToBreak.getWorld().dropItemNaturally(blockToBreak.getLocation(),
                                new ItemStack(targetMaterial, 1));
                    }
                }
            }

            // Update player stats
            int totalBlocksMined = blocksToBreak.size() + 1; // +1 for the original block
            plugin.getLevelManager().addBlocksMined(player, totalBlocksMined);

            // Update achievements
            if (plugin.getAchievementManager().isEnabled()) {
                plugin.getAchievementManager().updateBlockMineAchievements(player, targetMaterial.toString(), totalBlocksMined);
            }

            // Debug message
            if (plugin.isDebugMode()) {
                plugin.debug("Player " + player.getName() + " vein mined " + totalBlocksMined + " blocks of " + targetMaterial);
            }
        }
    }

    /**
     * Find connected blocks of the same type
     * @param startBlock The starting block
     * @param material The material to match
     * @param blocksToBreak Set of blocks to break
     * @param checkedBlocks Set of blocks already checked
     * @param maxBlocks Maximum number of blocks to find
     */
    private void findConnectedBlocks(Block startBlock, Material material, Set<Block> blocksToBreak, Set<Block> checkedBlocks, int maxBlocks) {
        // Stop if we've reached the maximum number of blocks
        if (blocksToBreak.size() >= maxBlocks) {
            return;
        }

        // Check if hybrid mode is enabled and if this block type is in the blacklist
        boolean hybridMode = plugin.getConfig().getBoolean("settings.hybrid-mode", false);
        List<String> hybridBlacklist = plugin.getConfig().getStringList("settings.hybrid-blacklist");
        boolean useDirectOnly = hybridBlacklist.contains(material.toString());

        // Debug logging
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] VeinMiner mode for " + material + ": " + 
                (hybridMode && !useDirectOnly ? "HYBRID (diagonal + direct)" : "DIRECT (direct only)") +
                (useDirectOnly ? " (blacklisted)" : ""));
        }

        // Determine which blocks to check based on mode
        if (hybridMode && !useDirectOnly) {
            // Hybrid mode: check all adjacent blocks (including diagonals)
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        // Skip the center block
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }

                        Block adjacentBlock = startBlock.getRelative(x, y, z);

                        // Skip if already checked
                        if (checkedBlocks.contains(adjacentBlock)) {
                            continue;
                        }

                        checkedBlocks.add(adjacentBlock);

                        // Check if it's the same material
                        if (adjacentBlock.getType() == material) {
                            blocksToBreak.add(adjacentBlock);

                            // Recursively check adjacent blocks
                            if (blocksToBreak.size() < maxBlocks) {
                                findConnectedBlocks(adjacentBlock, material, blocksToBreak, checkedBlocks, maxBlocks);
                            }
                        }
                    }
                }
            }
        } else {
            // Direct mode: check only direct adjacent blocks (no diagonals)
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        // Skip diagonals and the center block
                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) != 1) {
                            continue;
                        }

                        Block adjacentBlock = startBlock.getRelative(x, y, z);

                        // Skip if already checked
                        if (checkedBlocks.contains(adjacentBlock)) {
                            continue;
                        }

                        checkedBlocks.add(adjacentBlock);

                        // Check if it's the same material
                        if (adjacentBlock.getType() == material) {
                            blocksToBreak.add(adjacentBlock);

                            // Recursively check adjacent blocks
                            if (blocksToBreak.size() < maxBlocks) {
                                findConnectedBlocks(adjacentBlock, material, blocksToBreak, checkedBlocks, maxBlocks);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply durability cost to the tool
     * @param player The player
     * @param tool The tool
     * @param blockCount Number of blocks broken
     * @param playerData The player data
     * @return True if the tool survived, false if it broke
     */
    private boolean applyToolDurability(Player player, ItemStack tool, int blockCount, PlayerData playerData) {
        if (!plugin.getConfig().getBoolean("settings.use-durability-multiplier", true)) {
            return true;
        }

        // Skip if tool has infinite durability
        if (tool.getType().getMaxDurability() == 0 || tool.getItemMeta().isUnbreakable()) {
            return true;
        }

        // Calculate durability cost
        double multiplier = plugin.getConfig().getDouble("settings.durability-multiplier", 1.0);
        int durabilityLoss = (int) Math.ceil(blockCount * multiplier);

        // Apply efficiency boost to reduce durability loss
        if (plugin.getSkillManager().isEnabled()) {
            int efficiencyLevel = playerData.getEfficiencyLevel();
            double efficiencyChance = plugin.getSkillManager().getEfficiencyBoost(efficiencyLevel);

            // For each block, check if efficiency applies
            int blocksReduced = 0;
            for (int i = 0; i < blockCount; i++) {
                if (Math.random() * 100 < efficiencyChance) {
                    blocksReduced++;
                }
            }

            // Reduce durability loss
            durabilityLoss -= blocksReduced;
            if (durabilityLoss < 0) durabilityLoss = 0;
        }

        // Get current durability
        short maxDurability = tool.getType().getMaxDurability();
        short currentDurability = tool.getDurability();

        // Calculate new durability
        short newDurability = (short) (currentDurability + durabilityLoss);

        // Check if tool will break
        if (newDurability >= maxDurability) {
            // Tool breaks
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return false;
        } else {
            // Apply durability
            tool.setDurability(newDurability);
            return true;
        }
    }

    /**
     * Apply hunger cost to the player
     * @param player The player
     * @param blockCount Number of blocks broken
     * @param playerData The player data
     */
    private void applyHungerCost(Player player, int blockCount, PlayerData playerData) {
        if (!plugin.getConfig().getBoolean("settings.use-hunger-multiplier", true)) {
            return;
        }

        // Calculate hunger cost
        double multiplier = plugin.getConfig().getDouble("settings.hunger-multiplier", 0.1);
        float hungerLoss = (float) (blockCount * multiplier);

        // Apply energy conservation to reduce hunger loss
        if (plugin.getSkillManager().isEnabled()) {
            int energyLevel = playerData.getEnergyLevel();
            double energyChance = plugin.getSkillManager().getEnergyConservation(energyLevel);

            // For each block, check if energy conservation applies
            int blocksReduced = 0;
            for (int i = 0; i < blockCount; i++) {
                if (Math.random() * 100 < energyChance) {
                    blocksReduced++;
                }
            }

            // Reduce hunger loss
            hungerLoss -= (float) (blocksReduced * multiplier);
            if (hungerLoss < 0) hungerLoss = 0;
        }

        // Get current food level
        int foodLevel = player.getFoodLevel();
        float saturation = player.getSaturation();

        // First reduce saturation
        if (saturation > hungerLoss) {
            player.setSaturation(saturation - hungerLoss);
        } else {
            // Then reduce food level
            hungerLoss -= saturation;
            player.setSaturation(0);

            if (hungerLoss > 0) {
                int newFoodLevel = Math.max(0, foodLevel - (int) Math.ceil(hungerLoss));
                player.setFoodLevel(newFoodLevel);
            }
        }
    }

    /**
     * Check if a player has the base permission to use VeinMiner
     * @param player The player to check
     * @return True if the player has permission, false otherwise
     */
    private boolean hasVeinMinerPermission(Player player) {
        return plugin.hasPermission(player, "veinminer.use");
    }

    /**
     * Check if a player has permission to use a specific tool with VeinMiner
     * @param player The player to check
     * @param toolType The tool type (pickaxe, axe, shovel, hoe)
     * @return True if the player has permission, false otherwise
     */
    private boolean hasToolPermission(Player player, String toolType) {
        if (!plugin.getConfig().getBoolean("permissions.require-tool-permission", false)) {
            return true;
        }

        return plugin.hasPermission(player, "veinminer.tool." + toolType);
    }

    /**
     * Get the tool type from a material
     * @param material The material
     * @return The tool type, or null if not a valid tool
     */
    private String getToolType(Material material) {
        String name = material.toString().toLowerCase();

        if (name.contains("pickaxe")) {
            return "pickaxe";
        } else if (name.contains("axe") && !name.contains("pickaxe")) {
            return "axe";
        } else if (name.contains("shovel") || name.contains("spade")) {
            return "shovel";
        } else if (name.contains("hoe")) {
            return "hoe";
        }

        return null;
    }
}