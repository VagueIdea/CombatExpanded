package com.willy.combatexpanded.commands;

import com.willy.combatexpanded.CombatExpanded;
import com.willy.combatexpanded.permission.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record MainCommand(CombatExpanded plugin) implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")&& sender.hasPermission(Permissions.ARTIFICE)) {
            return new InfoCommand(plugin).onCommand(sender, command, label, args);
        }
        else if (args[0].equalsIgnoreCase("artifice") && sender.hasPermission(Permissions.ARTIFICE)) {
            return new ArtificeCommand(plugin).onCommand(sender, command, label, args);
        }
        else if (args[0].equalsIgnoreCase("a") && sender.hasPermission(Permissions.ARTIFICE)) {
            return new ArtificeCommand(plugin).onCommand(sender, command, label, args);
        }
        else if (args[0].equalsIgnoreCase("enable") && sender.hasPermission(Permissions.ADMIN)) {
            plugin.setPluginEnabled(true);
            sender.sendMessage("§aCombatExpanded enabled!");
            return true;
        }
        else if (args[0].equalsIgnoreCase("disable") && sender.hasPermission(Permissions.ADMIN)) {
            plugin.setPluginEnabled(false);
            sender.sendMessage("§cCombatExpanded disabled!");
            return true;
        }
        else if (args[0].equalsIgnoreCase("reload") && sender.hasPermission(Permissions.ADMIN)) {
            plugin.reloadPlugin(sender);
            return true;
        }
        sender.sendMessage("Command Not Found");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        List<String> completions = new ArrayList<>();

        if (sender.hasPermission(Permissions.ARTIFICE)) {
            options.add("info");
            options.add("artifice");
            options.add("a");
        }

        if (sender.hasPermission(Permissions.ADMIN)) {
            options.add("enable");
            options.add("disable");
            options.add("reload");
        }

        StringUtil.copyPartialMatches(args[0], options, completions);
        return completions;
    }
}

