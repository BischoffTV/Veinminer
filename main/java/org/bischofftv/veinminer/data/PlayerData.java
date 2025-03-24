package org.bischofftv.veinminer.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID playerUUID;
    private boolean veinMinerEnabled;
    private final Map<String, Boolean> toolsEnabled;
    private int level;
    private int experience;
    private int blocksMined;

    // Skill levels
    private int efficiencyLevel;
    private int luckLevel;
    private int energyLevel;

    public PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.veinMinerEnabled = true;
        this.toolsEnabled = new HashMap<>();
        this.toolsEnabled.put("pickaxe", true);
        this.toolsEnabled.put("axe", true);
        this.toolsEnabled.put("shovel", true);
        this.toolsEnabled.put("hoe", true);
        this.level = 1;
        this.experience = 0;
        this.blocksMined = 0;

        // Initialize skill levels
        this.efficiencyLevel = 0;
        this.luckLevel = 0;
        this.energyLevel = 0;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public boolean isVeinMinerEnabled() {
        return veinMinerEnabled;
    }

    public void setVeinMinerEnabled(boolean veinMinerEnabled) {
        this.veinMinerEnabled = veinMinerEnabled;
    }

    public boolean isToolEnabled(String toolType) {
        return toolsEnabled.getOrDefault(toolType, true);
    }

    public void setToolEnabled(String toolType, boolean enabled) {
        toolsEnabled.put(toolType, enabled);
    }

    public Map<String, Boolean> getToolsEnabled() {
        return toolsEnabled;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public void addExperience(int amount) {
        this.experience += amount;
    }

    public int getBlocksMined() {
        return blocksMined;
    }

    public void setBlocksMined(int blocksMined) {
        this.blocksMined = blocksMined;
    }

    public void addBlocksMined(int amount) {
        this.blocksMined += amount;
    }

    // Skill getters and setters
    public int getEfficiencyLevel() {
        return efficiencyLevel;
    }

    public void setEfficiencyLevel(int efficiencyLevel) {
        this.efficiencyLevel = efficiencyLevel;
    }

    public int getLuckLevel() {
        return luckLevel;
    }

    public void setLuckLevel(int luckLevel) {
        this.luckLevel = luckLevel;
    }

    public int getEnergyLevel() {
        return energyLevel;
    }

    public void setEnergyLevel(int energyLevel) {
        this.energyLevel = energyLevel;
    }

    /**
     * Get the total skill points used
     * @return The total skill points used
     */
    public int getTotalSkillPointsUsed() {
        return efficiencyLevel + luckLevel + energyLevel;
    }
}