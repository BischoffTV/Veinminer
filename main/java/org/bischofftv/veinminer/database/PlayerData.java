package org.bischofftv.veinminer.database;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String name;
    private int level;
    private long xp;
    private int blocksMined;

    public PlayerData(UUID uuid, String name, int level, long xp, int blocksMined) {
        this.uuid = uuid;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.blocksMined = blocksMined;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public int getBlocksMined() {
        return blocksMined;
    }

    public void setBlocksMined(int blocksMined) {
        this.blocksMined = blocksMined;
    }

    public void incrementBlocksMined() {
        this.blocksMined++;
    }

    public void incrementBlocksMined(int amount) {
        this.blocksMined += amount;
    }
}