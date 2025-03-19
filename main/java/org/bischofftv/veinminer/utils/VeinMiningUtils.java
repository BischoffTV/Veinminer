package org.bischofftv.veinminer.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class VeinMiningUtils {

    public static Collection<Block> findConnectedBlocks(Block startBlock, int maxBlocks, Set<Material> allowedMaterials) {
        Set<Block> connectedBlocks = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        Material targetType = startBlock.getType();
        if (!allowedMaterials.contains(targetType)) {
            return connectedBlocks;
        }

        queue.add(startBlock);
        connectedBlocks.add(startBlock);

        while (!queue.isEmpty() && connectedBlocks.size() < maxBlocks) {
            Block currentBlock = queue.poll();

            // Check all 6 adjacent blocks (not diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        // Skip diagonals and the block itself
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) {
                            continue;
                        }

                        Block adjacentBlock = currentBlock.getRelative(dx, dy, dz);

                        // Check if the block is of the same type and not already processed
                        if (adjacentBlock.getType() == targetType && !connectedBlocks.contains(adjacentBlock)) {
                            connectedBlocks.add(adjacentBlock);
                            queue.add(adjacentBlock);

                            // Stop if we've reached the maximum number of blocks
                            if (connectedBlocks.size() >= maxBlocks) {
                                break;
                            }
                        }
                    }
                    if (connectedBlocks.size() >= maxBlocks) break;
                }
                if (connectedBlocks.size() >= maxBlocks) break;
            }
        }

        return connectedBlocks;
    }

    public static Collection<ItemStack> getSilkTouchDrops(Block block) {
        // With Silk Touch, we just get the block itself
        return Collections.singletonList(new ItemStack(block.getType()));
    }

    public static Collection<ItemStack> getFortuneDrops(Block block, int fortuneLevel) {
        Material blockType = block.getType();
        List<ItemStack> drops = new ArrayList<>();
        Random random = new Random();

        // Handle different block types specifically
        switch (blockType) {
            // Coal Ores
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                int coalAmount = 1;
                if (fortuneLevel > 0) {
                    coalAmount = 1 + random.nextInt(fortuneLevel + 2) - 1;
                    if (coalAmount < 1) coalAmount = 1;
                }
                drops.add(new ItemStack(Material.COAL, coalAmount));
                break;

            // Diamond Ores
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                int diamondAmount = 1;
                if (fortuneLevel > 0) {
                    diamondAmount = 1 + random.nextInt(fortuneLevel + 2) - 1;
                    if (diamondAmount < 1) diamondAmount = 1;
                }
                drops.add(new ItemStack(Material.DIAMOND, diamondAmount));
                break;

            // Emerald Ores
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                int emeraldAmount = 1;
                if (fortuneLevel > 0) {
                    emeraldAmount = 1 + random.nextInt(fortuneLevel + 2) - 1;
                    if (emeraldAmount < 1) emeraldAmount = 1;
                }
                drops.add(new ItemStack(Material.EMERALD, emeraldAmount));
                break;

            // Lapis Ores
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                int lapisBaseAmount = 4 + random.nextInt(5); // 4-8 lapis normally
                int lapisAmount = lapisBaseAmount;
                if (fortuneLevel > 0) {
                    for (int i = 0; i < lapisBaseAmount; i++) {
                        if (random.nextDouble() < 0.3 * fortuneLevel) {
                            lapisAmount++;
                        }
                    }
                }
                drops.add(new ItemStack(Material.LAPIS_LAZULI, lapisAmount));
                break;

            // Redstone Ores
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                int redstoneBaseAmount = 4 + random.nextInt(2); // 4-5 redstone normally
                int redstoneAmount = redstoneBaseAmount;
                if (fortuneLevel > 0) {
                    for (int i = 0; i < redstoneBaseAmount; i++) {
                        if (random.nextDouble() < 0.3 * fortuneLevel) {
                            redstoneAmount++;
                        }
                    }
                }
                drops.add(new ItemStack(Material.REDSTONE, redstoneAmount));
                break;

            // Nether Quartz Ore
            case NETHER_QUARTZ_ORE:
                int quartzAmount = 1;
                if (fortuneLevel > 0) {
                    quartzAmount = 1 + random.nextInt(fortuneLevel + 2) - 1;
                    if (quartzAmount < 1) quartzAmount = 1;
                }
                drops.add(new ItemStack(Material.QUARTZ, quartzAmount));
                break;

            // Nether Gold Ore
            case NETHER_GOLD_ORE:
                int goldNuggetBaseAmount = 2 + random.nextInt(4); // 2-5 gold nuggets normally
                int goldNuggetAmount = goldNuggetBaseAmount;
                if (fortuneLevel > 0) {
                    for (int i = 0; i < fortuneLevel; i++) {
                        goldNuggetAmount += random.nextInt(2); // Add 0-1 per fortune level
                    }
                }
                drops.add(new ItemStack(Material.GOLD_NUGGET, goldNuggetAmount));
                break;

            // Copper Ores
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                int copperBaseAmount = 2 + random.nextInt(3); // 2-4 raw copper normally
                int copperAmount = copperBaseAmount;
                if (fortuneLevel > 0) {
                    for (int i = 0; i < fortuneLevel; i++) {
                        if (random.nextDouble() < 0.4) {
                            copperAmount += random.nextInt(2) + 1; // Add 1-2 per fortune level with 40% chance
                        }
                    }
                }
                drops.add(new ItemStack(Material.RAW_COPPER, copperAmount));
                break;

            // Iron Ores
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                int ironAmount = 1;
                if (fortuneLevel > 0) {
                    ironAmount = 1 + random.nextInt(fortuneLevel + 1);
                }
                drops.add(new ItemStack(Material.RAW_IRON, ironAmount));
                break;

            // Gold Ores
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
                int goldAmount = 1;
                if (fortuneLevel > 0) {
                    goldAmount = 1 + random.nextInt(fortuneLevel + 1);
                }
                drops.add(new ItemStack(Material.RAW_GOLD, goldAmount));
                break;

            // Ancient Debris (doesn't benefit from fortune)
            case ANCIENT_DEBRIS:
                drops.add(new ItemStack(Material.ANCIENT_DEBRIS, 1));
                break;

            // Default case for other blocks
            default:
                // For blocks not specifically handled, use the block's natural drops
                for (ItemStack drop : block.getDrops()) {
                    drops.add(drop);
                }
                break;
        }

        return drops;
    }
}