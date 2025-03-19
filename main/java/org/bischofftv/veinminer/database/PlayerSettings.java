package org.bischofftv.veinminer.database;

import java.util.UUID;

public class PlayerSettings {
    private final UUID uuid;
    private boolean enabled;
    private boolean pickaxeEnabled;
    private boolean axeEnabled;
    private boolean shovelEnabled;
    private boolean hoeEnabled;

    public PlayerSettings(UUID uuid, boolean enabled, boolean pickaxeEnabled, boolean axeEnabled, boolean shovelEnabled, boolean hoeEnabled) {
        this.uuid = uuid;
        this.enabled = enabled;
        this.pickaxeEnabled = pickaxeEnabled;
        this.axeEnabled = axeEnabled;
        this.shovelEnabled = shovelEnabled;
        this.hoeEnabled = hoeEnabled;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPickaxeEnabled() {
        return pickaxeEnabled;
    }

    public void setPickaxeEnabled(boolean pickaxeEnabled) {
        this.pickaxeEnabled = pickaxeEnabled;
    }

    public boolean isAxeEnabled() {
        return axeEnabled;
    }

    public void setAxeEnabled(boolean axeEnabled) {
        this.axeEnabled = axeEnabled;
    }

    public boolean isShovelEnabled() {
        return shovelEnabled;
    }

    public void setShovelEnabled(boolean shovelEnabled) {
        this.shovelEnabled = shovelEnabled;
    }

    public boolean isHoeEnabled() {
        return hoeEnabled;
    }

    public void setHoeEnabled(boolean hoeEnabled) {
        this.hoeEnabled = hoeEnabled;
    }

    public boolean isToolEnabled(String toolType) {
        switch (toolType.toLowerCase()) {
            case "pickaxe":
                return pickaxeEnabled;
            case "axe":
                return axeEnabled;
            case "shovel":
                return shovelEnabled;
            case "hoe":
                return hoeEnabled;
            default:
                return false;
        }
    }

    public void setToolEnabled(String toolType, boolean enabled) {
        switch (toolType.toLowerCase()) {
            case "pickaxe":
                pickaxeEnabled = enabled;
                break;
            case "axe":
                axeEnabled = enabled;
                break;
            case "shovel":
                shovelEnabled = enabled;
                break;
            case "hoe":
                hoeEnabled = enabled;
                break;
        }
    }
}