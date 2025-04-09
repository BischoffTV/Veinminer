package org.bischofftv.veinminer.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private UUID uuid;
    private String playerName;
    private boolean veinMinerEnabled;
    private int level;
    private int experience;
    private long blocksMined;
    private Map<String, Boolean> enabledTools;
    private int skillPoints;

    // Skill levels
    private int efficiencyLevel;
    private int luckLevel;
    private int energyLevel;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.veinMinerEnabled = false; // Set to false by default
        this.level = 1;
        this.experience = 0;
        this.blocksMined = 0;
        this.skillPoints = 0;

        // Initialize skill levels
        this.efficiencyLevel = 0;
        this.luckLevel = 0;
        this.energyLevel = 0;

        // Initialize all tools as disabled by default
        this.enabledTools = new HashMap<>();
        this.enabledTools.put("pickaxe", false);
        this.enabledTools.put("axe", false);
        this.enabledTools.put("shovel", false);
        this.enabledTools.put("hoe", false);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isVeinMinerEnabled() {
        return veinMinerEnabled;
    }

    public void setVeinMinerEnabled(boolean veinMinerEnabled) {
        this.veinMinerEnabled = veinMinerEnabled;
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

    public long getBlocksMined() {
        return blocksMined;
    }

    public void setBlocksMined(long blocksMined) {
        this.blocksMined = blocksMined;
    }

    public void addBlocksMined(int amount) {
        this.blocksMined += amount;
    }

    public Map<String, Boolean> getEnabledTools() {
        return enabledTools;
    }

    public void setEnabledTools(Map<String, Boolean> enabledTools) {
        this.enabledTools = enabledTools;
    }

    public boolean isToolEnabled(String toolName) {
        return enabledTools.getOrDefault(toolName, false);
    }

    public void setToolEnabled(String toolName, boolean enabled) {
        this.enabledTools.put(toolName, enabled);
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public void addSkillPoints(int points) {
        this.skillPoints += points;
    }

    public boolean useSkillPoints(int points) {
        if (skillPoints >= points) {
            skillPoints -= points;
            return true;
        }
        return false;
    }

    // Skill level getters and setters
    public int getEfficiencyLevel() {
        return efficiencyLevel;
    }

    public void setEfficiencyLevel(int level) {
        this.efficiencyLevel = level;
    }

    public int getLuckLevel() {
        return luckLevel;
    }

    public void setLuckLevel(int level) {
        this.luckLevel = level;
    }

    public int getEnergyLevel() {
        return energyLevel;
    }

    public void setEnergyLevel(int level) {
        this.energyLevel = level;
    }
}