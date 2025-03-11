package org.bischofftv.veinminer.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VeinMinerTabCompleter implements TabCompleter {

    private static final List<String> MAIN_COMMANDS = Arrays.asList("on", "off", "toggle", "tool", "level");
    private static final List<String> TOOL_TYPES = Arrays.asList("pickaxe", "axe", "shovel", "hoe");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - return all main commands
            String partialCommand = args[0].toLowerCase();
            completions.addAll(MAIN_COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(partialCommand))
                    .collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tool")) {
            // Second argument after "tool" - return all tool types
            String partialTool = args[1].toLowerCase();
            completions.addAll(TOOL_TYPES.stream()
                    .filter(tool -> tool.startsWith(partialTool))
                    .collect(Collectors.toList()));
        }

        return completions;
    }
}

