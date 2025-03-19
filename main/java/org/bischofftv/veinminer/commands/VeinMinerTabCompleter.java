package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VeinMinerTabCompleter implements TabCompleter {

    private static final List<String> MAIN_COMMANDS = Arrays.asList("on", "off", "toggle", "tool", "level", "setlevel", "achievements");
    private static final List<String> ADMIN_COMMANDS = Arrays.asList("check", "mysql", "debug", "config", "sync", "reload", "help");
    private static final List<String> TOOL_TYPES = Arrays.asList("pickaxe", "axe", "shovel", "hoe");
    private static final List<String> MYSQL_SUBCOMMANDS = Collections.singletonList("reload");
    private static final List<String> DEBUG_OPTIONS = Arrays.asList("on", "off");

    private final Veinminer plugin;

    public VeinMinerTabCompleter(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - return all main commands
            String partialCommand = args[0].toLowerCase();
            List<String> availableCommands = new ArrayList<>(MAIN_COMMANDS);

            // Only show setlevel command if player has permission
            if (!sender.hasPermission("veinminer.admin.setlevel")) {
                availableCommands.remove("setlevel");
            }

            // Add admin commands if player has permission
            if (sender.hasPermission("veinminer.admin")) {
                availableCommands.addAll(ADMIN_COMMANDS);
            }

            StringUtil.copyPartialMatches(partialCommand, availableCommands, completions);
            Collections.sort(completions);
            return completions;
        } else if (args.length == 2) {
            String firstArg = args[0].toLowerCase();
            String partialSecondArg = args[1].toLowerCase();

            if (firstArg.equals("tool")) {
                // Second argument after "tool" - return all tool types
                StringUtil.copyPartialMatches(partialSecondArg, TOOL_TYPES, completions);
            } else if (firstArg.equals("setlevel") && sender.hasPermission("veinminer.admin.setlevel")) {
                // Second argument after "setlevel" - return online player names
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(partialSecondArg, playerNames, completions);
            } else if (firstArg.equals("mysql") && sender.hasPermission("veinminer.admin")) {
                // Second argument after "mysql" - return mysql subcommands
                StringUtil.copyPartialMatches(partialSecondArg, MYSQL_SUBCOMMANDS, completions);
            } else if (firstArg.equals("debug") && sender.hasPermission("veinminer.admin")) {
                // Second argument after "debug" - return debug options
                StringUtil.copyPartialMatches(partialSecondArg, DEBUG_OPTIONS, completions);
            } else if (firstArg.equals("config") && sender.hasPermission("veinminer.admin")) {
                // Second argument after "config" - return config paths
                List<String> configPaths = getConfigPaths("");
                StringUtil.copyPartialMatches(partialSecondArg, configPaths, completions);
            }
        } else if (args.length == 3) {
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1].toLowerCase();
            String partialThirdArg = args[2].toLowerCase();

            if (firstArg.equals("setlevel") && sender.hasPermission("veinminer.admin.setlevel")) {
                // Third argument after "setlevel <player>" - suggest levels 1-10
                List<String> levels = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
                StringUtil.copyPartialMatches(partialThirdArg, levels, completions);
            } else if (firstArg.equals("config") && sender.hasPermission("veinminer.admin")) {
                // Third argument after "config <path>" - suggest current value or type-specific values
                Object currentValue = plugin.getConfig().get(secondArg);
                if (currentValue instanceof Boolean) {
                    StringUtil.copyPartialMatches(partialThirdArg, Arrays.asList("true", "false"), completions);
                }
            }
        }

        return completions;
    }

    private List<String> getConfigPaths(String parent) {
        List<String> paths = new ArrayList<>();

        // Add top-level sections
        if (parent.isEmpty()) {
            paths.add("settings");
            paths.add("level-system");
            paths.add("achievement-system");
            paths.add("database");
            paths.add("permissions");
            paths.add("economy");
            paths.add("logging");
            paths.add("gui");

            // Add common direct settings
            paths.add("settings.debug");
            paths.add("settings.max-blocks");
            paths.add("settings.auto-save-interval");
            paths.add("level-system.enabled");
            paths.add("achievement-system.enabled");
            paths.add("database.use-mysql");
            paths.add("database.host");
            paths.add("database.port");
            paths.add("database.username");
            paths.add("database.database");
            paths.add("database.table-prefix");
        }

        return paths;
    }
}