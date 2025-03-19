package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ToggleCommand implements CommandExecutor {

    private final Veinminer plugin;

    public ToggleCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Check permission if required
        if (plugin.getConfig().getBoolean("permissions.require-permission", true)) {
            String permission = plugin.getConfig().getString("permissions.use-permission", "veinminer.use");
            if (!player.hasPermission(permission)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("messages.command.no-permission"));
                return true;
            }
        }

        if (args.length == 0) {
            // Toggle veinminer on/off
            boolean newState = !plugin.getPlayerDataManager().isVeinMinerEnabled(player);
            plugin.getPlayerDataManager().setVeinMinerEnabled(player, newState);

            if (newState) {
                player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.enabled"));
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.disabled"));
            }
            return true;
        }

        if (args.length >= 1) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "on":
                    plugin.getPlayerDataManager().setVeinMinerEnabled(player, true);
                    player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.enabled"));
                    return true;

                case "off":
                    plugin.getPlayerDataManager().setVeinMinerEnabled(player, false);
                    player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.disabled"));
                    return true;

                case "toggle":
                    boolean newState = !plugin.getPlayerDataManager().isVeinMinerEnabled(player);
                    plugin.getPlayerDataManager().setVeinMinerEnabled(player, newState);

                    if (newState) {
                        player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.enabled"));
                    } else {
                        player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.disabled"));
                    }
                    return true;

                case "tool":
                    if (args.length < 2) {
                        player.sendMessage(plugin.getMessageManager().getMessage("messages.command.usage-tool"));
                        return true;
                    }

                    String toolType = args[1].toLowerCase();
                    if (!isValidToolType(toolType)) {
                        player.sendMessage(plugin.getMessageManager().getMessage("messages.command.invalid-tool-type"));
                        return true;
                    }

                    boolean toolEnabled = !plugin.getPlayerDataManager().isToolEnabled(player, toolType);
                    plugin.getPlayerDataManager().setToolEnabled(player, toolType, toolEnabled);

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("tool", toolType);

                    if (toolEnabled) {
                        player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.tool-enabled", placeholders));
                    } else {
                        player.sendMessage(plugin.getMessageManager().getMessage("messages.toggle.tool-disabled", placeholders));
                    }
                    return true;

                default:
                    player.sendMessage(plugin.getMessageManager().getMessage("messages.command.unknown-subcommand"));
                    return true;
            }
        }

        return false;
    }

    private boolean isValidToolType(String toolType) {
        return toolType.equals("pickaxe") ||
                toolType.equals("axe") ||
                toolType.equals("shovel") ||
                toolType.equals("hoe");
    }
}